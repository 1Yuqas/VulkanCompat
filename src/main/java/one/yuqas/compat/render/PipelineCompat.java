package one.yuqas.compat.render;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkAllocationCallbacks;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkGraphicsPipelineCreateInfo;
import org.lwjgl.vulkan.VkPipelineRenderingCreateInfo;
import org.lwjgl.vulkan.VkPipelineRenderingCreateInfoKHR;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;

import static one.yuqas.compat.render.VulkanConstants.*;

public final class PipelineCompat {
    private PipelineCompat() {}

    public static int createGraphicsPipelines(VkDevice device, long pipelineCache, VkGraphicsPipelineCreateInfo.Buffer createInfos, VkAllocationCallbacks allocator, LongBuffer pPipelines) {
        long renderingInfoAddress = createInfos.pNext();
        if (renderingInfoAddress == 0L) {
            return VK12.vkCreateGraphicsPipelines(device, pipelineCache, createInfos, allocator, pPipelines);
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPipelineRenderingCreateInfo renderingInfo = null;
            try {
                renderingInfo = VkPipelineRenderingCreateInfo.create(renderingInfoAddress);
                if (renderingInfo.sType() != VK13.VK_STRUCTURE_TYPE_PIPELINE_RENDERING_CREATE_INFO) renderingInfo = null;
            } catch (Exception ignored) { renderingInfo = null; }
            VkPipelineRenderingCreateInfoKHR renderingInfoKHR = null;
            IntBuffer formatsBuffer;
            int declaredCount;
            if (renderingInfo != null) {
                formatsBuffer = renderingInfo.pColorAttachmentFormats();
                declaredCount = renderingInfo.colorAttachmentCount();
            } else {
                renderingInfoKHR = VkPipelineRenderingCreateInfoKHR.create(renderingInfoAddress);
                formatsBuffer = renderingInfoKHR.pColorAttachmentFormats();
                declaredCount = renderingInfoKHR.colorAttachmentCount();
            }
            int total = formatsBuffer != null && (declaredCount <= 0 || declaredCount > formatsBuffer.remaining()) ? formatsBuffer.remaining() : declaredCount;
            int colorCount = 0;
            if (formatsBuffer != null) for (int i = 0; i < total; i++) if (formatsBuffer.get(i) != VK_FORMAT_UNDEFINED) colorCount++;
            int[] colorFormats = new int[colorCount];
            int idx = 0;
            if (formatsBuffer != null) for (int i = 0; i < total; i++) { int f = formatsBuffer.get(i); if (f != VK_FORMAT_UNDEFINED) colorFormats[idx++] = f; }
            int[] loadOps = new int[colorCount];
            int[] storeOps = new int[colorCount];
            Arrays.fill(loadOps, VK_ATTACHMENT_LOAD_OP_LOAD);
            Arrays.fill(storeOps, VK_ATTACHMENT_STORE_OP_STORE);
            int depthFormat = renderingInfo != null ? renderingInfo.depthAttachmentFormat() : renderingInfoKHR.depthAttachmentFormat();
            long renderPass = RenderPassCache.getOrCreate(device, colorFormats, depthFormat, loadOps, storeOps, VK_ATTACHMENT_LOAD_OP_LOAD, VK_ATTACHMENT_STORE_OP_STORE);
            createInfos.renderPass(renderPass);
            createInfos.subpass(0);
            createInfos.pNext(0L);
            int result = VK12.vkCreateGraphicsPipelines(device, pipelineCache, createInfos, allocator, pPipelines);
            createInfos.pNext(renderingInfoAddress);
            return result;
        }
    }
}
