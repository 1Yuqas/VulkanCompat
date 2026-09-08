package one.yuqas.mixin;

import com.mojang.blaze3d.vulkan.VulkanCommandEncoder;
import one.yuqas.compat.DeviceSupport;
import one.yuqas.compat.LegacyRenderPass;
import org.lwjgl.vulkan.KHRDynamicRendering;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkRenderingInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(VulkanCommandEncoder.class)
public abstract class VulkanCommandEncoderMixin {

    @Redirect(
            method = "createRenderPass",
            at = @At(value = "INVOKE",
                    target = "Lorg/lwjgl/vulkan/KHRDynamicRendering;vkCmdBeginRenderingKHR(Lorg/lwjgl/vulkan/VkCommandBuffer;Lorg/lwjgl/vulkan/VkRenderingInfo;)V")
    )
    private static void qadish$beginRenderPass(VkCommandBuffer commandBuffer, VkRenderingInfo renderingInfo) {
        if (!DeviceSupport.supportsDynamicRendering(commandBuffer.getDevice())) {
            LegacyRenderPass.begin(commandBuffer, renderingInfo);
        } else {
            KHRDynamicRendering.vkCmdBeginRenderingKHR(commandBuffer, renderingInfo);
        }
    }

    @Redirect(
            method = "submitRenderPass",
            at = @At(value = "INVOKE",
                    target = "Lorg/lwjgl/vulkan/KHRDynamicRendering;vkCmdEndRenderingKHR(Lorg/lwjgl/vulkan/VkCommandBuffer;)V")
    )
    private static void qadish$endRenderPass(VkCommandBuffer commandBuffer) {
        if (!DeviceSupport.supportsDynamicRendering(commandBuffer.getDevice())) {
            VK12.vkCmdEndRenderPass(commandBuffer);
        } else {
            KHRDynamicRendering.vkCmdEndRenderingKHR(commandBuffer);
        }
    }
}
