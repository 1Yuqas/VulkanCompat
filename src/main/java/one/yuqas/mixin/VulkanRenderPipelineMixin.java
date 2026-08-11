package one.yuqas.mixin;

import com.mojang.blaze3d.vulkan.VulkanRenderPipeline;
import one.yuqas.Vulkancompat;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkAllocationCallbacks;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkGraphicsPipelineCreateInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.LongBuffer;

@Mixin(VulkanRenderPipeline.class)
public abstract class VulkanRenderPipelineMixin {

    @Redirect(
            method = "compile",
            at = @At(value = "INVOKE",
                    target = "Lorg/lwjgl/vulkan/VK12;vkCreateGraphicsPipelines(Lorg/lwjgl/vulkan/VkDevice;JLorg/lwjgl/vulkan/VkGraphicsPipelineCreateInfo$Buffer;Lorg/lwjgl/vulkan/VkAllocationCallbacks;Ljava/nio/LongBuffer;)I")
    )
    private static int qadish$createGraphicsPipelines(VkDevice device, long pipelineCache,
                                                      VkGraphicsPipelineCreateInfo.Buffer createInfos,
                                                      VkAllocationCallbacks allocator, LongBuffer pPipelines) {
        if (!Vulkancompat.supportsDynamicRendering(device)) {
            return Vulkancompat.createGraphicsPipelines(device, pipelineCache, createInfos, allocator, pPipelines);
        }
        return VK12.vkCreateGraphicsPipelines(device, pipelineCache, createInfos, allocator, pPipelines);
    }
}
