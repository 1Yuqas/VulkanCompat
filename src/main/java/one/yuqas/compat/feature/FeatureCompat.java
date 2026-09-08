package one.yuqas.compat.feature;

import com.mojang.renderpearl.backend.vulkan.init.FeatureSet;
import com.mojang.renderpearl.backend.vulkan.init.VulkanFeature;
import one.yuqas.compat.device.DeviceSupport;

import java.util.HashSet;
import java.util.Set;

public final class FeatureCompat {
    private FeatureCompat() {}

    public static FeatureSet stripFeatureSet(FeatureSet original) {
        if (original == null) return null;
        boolean hasDynExt = original.extensions().contains(DeviceSupport.extensionName());
        boolean hasDynFeat = false;
        for (VulkanFeature f : original.features()) {
            if (DeviceSupport.featureName().equals(f.name())) {
                hasDynFeat = true;
                break;
            }
        }
        if (!hasDynExt && !hasDynFeat) return original;
        Set<String> newExts = new HashSet<>(original.extensions());
        newExts.remove(DeviceSupport.extensionName());
        Set<VulkanFeature> newFeatures = new HashSet<>(original.features());
        newFeatures.removeIf(feature -> DeviceSupport.featureName().equals(feature.name()));
        return new FeatureSet(original.name(), Set.copyOf(newExts), Set.copyOf(newFeatures), original.condition());
    }

    public static Set<FeatureSet> stripFeatureSets(Set<FeatureSet> original) {
        if (original == null || original.isEmpty()) return original;
        boolean needsStrip = false;
        for (FeatureSet fs : original) {
            if (fs.extensions().contains(DeviceSupport.extensionName())) { needsStrip = true; break; }
            for (VulkanFeature f : fs.features()) {
                if (DeviceSupport.featureName().equals(f.name())) { needsStrip = true; break; }
            }
            if (needsStrip) break;
        }
        if (!needsStrip) return original;
        Set<FeatureSet> result = new HashSet<>();
        for (FeatureSet fs : original) {
            result.add(stripFeatureSet(fs));
        }
        return result;
    }

    public static Set<FeatureSet> filterFeatureSetsForDevice(Set<FeatureSet> featureSets, org.lwjgl.vulkan.VkPhysicalDevice vkPhysicalDevice) {
        if (DeviceSupport.deviceSupportsDynamicRendering(vkPhysicalDevice)) {
            return featureSets;
        }
        return stripFeatureSets(featureSets);
    }
}
