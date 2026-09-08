package one.yuqas.mixin;

import com.mojang.renderpearl.backend.vulkan.VulkanConst;
import com.mojang.renderpearl.backend.vulkan.VulkanGpuTexture;
import com.mojang.renderpearl.backend.vulkan.VulkanGpuTextureView;
import one.yuqas.compat.view.ViewRegistry;
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

    @Inject(method = "<init>(Lcom/mojang/renderpearl/backend/vulkan/VulkanDevice;Lcom/mojang/renderpearl/backend/vulkan/VulkanGpuTexture;II)V", at = @At("TAIL"))
    private void qadish$registerView(CallbackInfo ci) {
        ViewRegistry.register(this.vkImageView,
                VulkanConst.toVk(this.texture().getFormat()),
                this.texture().getWidth(0),
                this.texture().getHeight(0));
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void qadish$unregisterView(CallbackInfo ci) {
        ViewRegistry.unregister(this.vkImageView);
    }
}
