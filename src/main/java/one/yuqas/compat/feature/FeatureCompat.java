package one.yuqas.compat.feature;

import com.mojang.blaze3d.vulkan.init.VulkanFeature;
import one.yuqas.compat.device.DeviceSupport;

import java.util.HashSet;
import java.util.Set;

public final class FeatureCompat {
    private FeatureCompat() {}

    public static Set<String> stripRequiredExtensions(Set<String> extensions) {
        if (extensions == null || extensions.isEmpty()) {
            return extensions;
        }
        if (!extensions.contains(DeviceSupport.extensionName())) {
            return extensions;
        }
        Set<String> result = new HashSet<>(extensions);
        result.remove(DeviceSupport.extensionName());
        return result;
    }

    public static Set<VulkanFeature> stripRequiredFeatures(Set<VulkanFeature> features) {
        if (features == null || features.isEmpty()) {
            return features;
        }
        boolean needsStrip = false;
        for (VulkanFeature f : features) {
            if (DeviceSupport.featureName().equals(f.name())) {
                needsStrip = true;
                break;
            }
        }
        if (!needsStrip) return features;
        Set<VulkanFeature> result = new HashSet<>(features);
        result.removeIf(feature -> DeviceSupport.featureName().equals(feature.name()));
        return result;
    }

    public static Set<String> filterExtensionsForDevice(Set<String> extensions, org.lwjgl.vulkan.VkPhysicalDevice vkPhysicalDevice) {
        if (DeviceSupport.deviceSupportsDynamicRendering(vkPhysicalDevice)) {
            return extensions;
        }
        return stripRequiredExtensions(extensions);
    }

    public static Set<VulkanFeature> filterFeaturesForDevice(Set<VulkanFeature> features, org.lwjgl.vulkan.VkPhysicalDevice vkPhysicalDevice) {
        if (DeviceSupport.deviceSupportsDynamicRendering(vkPhysicalDevice)) {
            return features;
        }
        return stripRequiredFeatures(features);
    }
}
