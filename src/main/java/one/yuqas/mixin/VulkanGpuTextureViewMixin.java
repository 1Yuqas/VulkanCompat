package one.yuqas.mixin;

import com.mojang.blaze3d.vulkan.VulkanConst;
import com.mojang.blaze3d.vulkan.VulkanGpuTexture;
import com.mojang.blaze3d.vulkan.VulkanGpuTextureView;
import one.yuqas.Vulkancompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VulkanGpuTextureView.class)
public abstract class VulkanGpuTextureViewMixin {

    @Shadow
    private long vkImageView;

    @Shadow
    public abstract VulkanGpuTexture texture();

    @Inject(method = "<init>(Lcom/mojang/blaze3d/vulkan/VulkanDevice;Lcom/mojang/blaze3d/vulkan/VulkanGpuTexture;II)V", at = @At("TAIL"))
    private void qadish$registerView(CallbackInfo ci) {
        Vulkancompat.registerImageView(this.vkImageView,
                VulkanConst.toVk(this.texture().getFormat()),
                this.texture().getWidth(0),
                this.texture().getHeight(0));
    }

    @Inject(method = "destroy", at = @At("HEAD"))
    private void qadish$unregisterView(CallbackInfo ci) {
        Vulkancompat.unregisterImageView(this.vkImageView);
    }
}
