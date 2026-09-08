package one.yuqas.mixin;

import  com.mojang.blaze3d.vulkan.VulkanBackend;
import com.mojang.blaze3d.vulkan.init.VulkanFeature;
import one.yuqas.compat.FeatureCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Set;

@Mixin(VulkanBackend.class)
public abstract class VulkanBackendMixin {

    @Shadow
    private static Set<String> REQUIRED_DEVICE_EXTENSIONS;

    @Shadow
    private static Set<VulkanFeature> REQUIRED_DEVICE_FEATURES;

    @Redirect(
            method = "createDevice(JLcom/mojang/blaze3d/shaders/ShaderSource;Lcom/mojang/blaze3d/shaders/GpuDebugOptions;Ljava/lang/Runnable;)Lcom/mojang/blaze3d/systems/GpuDevice;",
            at = @At(value = "FIELD", target = "Lcom/mojang/blaze3d/vulkan/VulkanBackend;REQUIRED_DEVICE_EXTENSIONS:Ljava/util/Set;")
    )
    private static Set<String> qadish$createDeviceExtensions() {
        return FeatureCompat.stripRequiredExtensions(REQUIRED_DEVICE_EXTENSIONS);
    }

    @Redirect(
            method = "createDevice(JLcom/mojang/blaze3d/shaders/ShaderSource;Lcom/mojang/blaze3d/shaders/GpuDebugOptions;Ljava/lang/Runnable;)Lcom/mojang/blaze3d/systems/GpuDevice;",
            at = @At(value = "FIELD", target = "Lcom/mojang/blaze3d/vulkan/VulkanBackend;REQUIRED_DEVICE_FEATURES:Ljava/util/Set;")
    )
    private static Set<VulkanFeature> qadish$createDeviceFeatures() {
        return FeatureCompat.stripRequiredFeatures(REQUIRED_DEVICE_FEATURES);
    }

    @Redirect(
            method = "isDeviceSuitable(Lorg/lwjgl/vulkan/VkPhysicalDevice;)Z",
            at = @At(value = "FIELD", target = "Lcom/mojang/blaze3d/vulkan/VulkanBackend;REQUIRED_DEVICE_EXTENSIONS:Ljava/util/Set;")
    )
    private static Set<String> qadish$isDeviceSuitableExtensions() {
        return FeatureCompat.stripRequiredExtensions(REQUIRED_DEVICE_EXTENSIONS);
    }

    @Redirect(
            method = "isDeviceSuitable(Lorg/lwjgl/vulkan/VkPhysicalDevice;)Z",
            at = @At(value = "FIELD", target = "Lcom/mojang/blaze3d/vulkan/VulkanBackend;REQUIRED_DEVICE_FEATURES:Ljava/util/Set;")
    )
    private static Set<VulkanFeature> qadish$isDeviceSuitableFeatures() {
        return FeatureCompat.stripRequiredFeatures(REQUIRED_DEVICE_FEATURES);
    }

    @Redirect(
            method = "throwForMissingRequrements(Lorg/lwjgl/vulkan/VkPhysicalDevice;)V",
            at = @At(value = "FIELD", target = "Lcom/mojang/blaze3d/vulkan/VulkanBackend;REQUIRED_DEVICE_EXTENSIONS:Ljava/util/Set;")
    )
    private static Set<String> qadish$throwForMissingExtensions() {
        return FeatureCompat.stripRequiredExtensions(REQUIRED_DEVICE_EXTENSIONS);
    }

    @Redirect(
            method = "throwForMissingRequrements(Lorg/lwjgl/vulkan/VkPhysicalDevice;)V",
            at = @At(value = "FIELD", target = "Lcom/mojang/blaze3d/vulkan/VulkanBackend;REQUIRED_DEVICE_FEATURES:Ljava/util/Set;")
    )
    private static Set<VulkanFeature> qadish$throwForMissingFeatures() {
        return FeatureCompat.stripRequiredFeatures(REQUIRED_DEVICE_FEATURES);
    }
}
