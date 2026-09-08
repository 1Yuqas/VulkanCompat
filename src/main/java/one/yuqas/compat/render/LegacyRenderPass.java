package one.yuqas.compat.render;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkAllocationCallbacks;
import org.lwjgl.vulkan.VkClearValue;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkGraphicsPipelineCreateInfo;
import org.lwjgl.vulkan.VkRenderPassBeginInfo;
import org.lwjgl.vulkan.VkRenderingAttachmentInfo;
import org.lwjgl.vulkan.VkRenderingInfo;

import java.nio.LongBuffer;
import one.yuqas.compat.view.ViewRegistry;

import static one.yuqas.compat.render.VulkanConstants.*;

public final class LegacyRenderPass {
    private LegacyRenderPass() {}

    public static void invalidateFramebuffers(long imageView) {
        FramebufferCache.invalidate(imageView);
    }

    public static void begin(VkCommandBuffer commandBuffer, VkRenderingInfo renderingInfo) {
        VkDevice device = commandBuffer.getDevice();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkRenderingAttachmentInfo.Buffer colorAttachmentInfos = renderingInfo.pColorAttachments();
            int declaredColorCount = renderingInfo.colorAttachmentCount();
            int totalColors = colorAttachmentInfos != null && (declaredColorCount <= 0 || declaredColorCount > colorAttachmentInfos.remaining()) ? colorAttachmentInfos.remaining() : declaredColorCount;
            int colorCount = 0;
            if (colorAttachmentInfos != null) {
                long base = colorAttachmentInfos.address();
                for (int i = 0; i < totalColors; i++) {
                    long addr = base + (long) i * VkRenderingAttachmentInfo.SIZEOF;
                    if (VkRenderingAttachmentInfo.nimageView(addr) != 0L) colorCount++;
                }
            }
            VkRenderingAttachmentInfo depthAttachmentInfo = renderingInfo.pDepthAttachment();
            boolean hasDepth = depthAttachmentInfo != null && depthAttachmentInfo.imageView() != 0L;
            int attachmentCount = colorCount + (hasDepth ? 1 : 0);
            int depthIndex = colorCount;
            int[] colorFormats = new int[colorCount];
            int[] colorLoadOps = new int[colorCount];
            int[] colorStoreOps = new int[colorCount];
            long[] colorViews = new long[colorCount];
            VkClearValue.Buffer clearValues = VkClearValue.calloc(Math.max(1, attachmentCount), stack);
            int framebufferWidth = 0;
            int framebufferHeight = 0;
            int index = 0;
            if (colorAttachmentInfos != null) {
                long base2 = colorAttachmentInfos.address();
                for (int i = 0; i < totalColors; i++) {
                    long addr = base2 + (long) i * VkRenderingAttachmentInfo.SIZEOF;
                    long view = VkRenderingAttachmentInfo.nimageView(addr);
                    if (view == 0L) continue;
                    int loadOp = VkRenderingAttachmentInfo.nloadOp(addr);
                    int storeOp = VkRenderingAttachmentInfo.nstoreOp(addr);
                    long packed = ViewRegistry.getPacked(view);
                    if (packed == Long.MIN_VALUE) throw new IllegalStateException("VulkanCompat: unknown color image view " + view);
                    colorFormats[index] = ViewRegistry.getFormat(packed);
                    colorLoadOps[index] = loadOp;
                    colorStoreOps[index] = storeOp;
                    colorViews[index] = view;
                    if (framebufferWidth == 0) {
                        framebufferWidth = ViewRegistry.getWidth(packed);
                        framebufferHeight = ViewRegistry.getHeight(packed);
                    }
                    if (loadOp == VK_ATTACHMENT_LOAD_OP_CLEAR) {
                        VkClearValue src = VkRenderingAttachmentInfo.nclearValue(addr);
                        clearValues.get(index).set(src);
                    }
                    index++;
                }
            }
            int depthFormat = VK_FORMAT_UNDEFINED;
            int depthLoadOp = VK_ATTACHMENT_LOAD_OP_DONT_CARE;
            int depthStoreOp = VK_ATTACHMENT_STORE_OP_DONT_CARE;
            if (hasDepth) {
                long depthView = depthAttachmentInfo.imageView();
                long packed = ViewRegistry.getPacked(depthView);
                if (packed == Long.MIN_VALUE) throw new IllegalStateException("VulkanCompat: unknown depth image view " + depthView);
                depthFormat = ViewRegistry.getFormat(packed);
                depthLoadOp = depthAttachmentInfo.loadOp();
                depthStoreOp = depthAttachmentInfo.storeOp();
                if (framebufferWidth == 0) {
                    framebufferWidth = ViewRegistry.getWidth(packed);
                    framebufferHeight = ViewRegistry.getHeight(packed);
                }
                if (depthLoadOp == VK_ATTACHMENT_LOAD_OP_CLEAR) clearValues.get(depthIndex).set(depthAttachmentInfo.clearValue());
            }
            long renderPass = RenderPassCache.getOrCreate(device, colorFormats, depthFormat, colorLoadOps, colorStoreOps, depthLoadOp, depthStoreOp);
            long framebuffer = 0L;
            if (attachmentCount > 0) {
                long[] views = new long[attachmentCount];
                System.arraycopy(colorViews, 0, views, 0, colorCount);
                if (hasDepth) views[colorCount] = depthAttachmentInfo.imageView();
                framebuffer = FramebufferCache.getOrCreate(device, renderPass, views, framebufferWidth, framebufferHeight);
            }
            VkRenderPassBeginInfo beginInfo = VkRenderPassBeginInfo.calloc(stack).sType$Default();
            beginInfo.renderPass(renderPass);
            beginInfo.framebuffer(framebuffer);
            beginInfo.renderArea(renderingInfo.renderArea());
            beginInfo.clearValueCount(attachmentCount);
            beginInfo.pClearValues(clearValues);
            // Vulkan 1.2: direct n-call (VK12 core) avoids wrapper + null allocator, uses MemoryUtil.NULL for speed
            VK12.nvkCmdBeginRenderPass(commandBuffer, beginInfo.address(), VK_SUBPASS_CONTENTS_INLINE);
        }
    }

    public static int createGraphicsPipelines(VkDevice device, long pipelineCache, VkGraphicsPipelineCreateInfo.Buffer createInfos, VkAllocationCallbacks allocator, LongBuffer pPipelines) {
        return PipelineCompat.createGraphicsPipelines(device, pipelineCache, createInfos, allocator, pPipelines);
    }
}
