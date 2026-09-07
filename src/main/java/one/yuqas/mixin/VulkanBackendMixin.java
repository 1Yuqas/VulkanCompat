package one.yuqas.mixin;

import com.mojang.renderpearl.backend.vulkan.VulkanBackend;
import com.mojang.renderpearl.backend.vulkan.init.FeatureSet;
import one.yuqas.compat.FeatureCompat;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Set;

@Mixin(VulkanBackend.class)
public abstract class VulkanBackendMixin {

    @ModifyVariable(
            method = "checkDeviceSuitability(Lorg/lwjgl/vulkan/VkPhysicalDevice;Ljava/util/Set;Ljava/util/Set;)Lcom/mojang/renderpearl/api/device/BackendCreationException;",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private static Set<FeatureSet> qadish$filterRequiredFeatureSets(Set<FeatureSet> requiredFeatureSets, VkPhysicalDevice vkPhysicalDevice) {
        return FeatureCompat.filterFeatureSetsForDevice(requiredFeatureSets, vkPhysicalDevice);
    }

    @ModifyVariable(
            method = "checkDeviceSuitability(Lorg/lwjgl/vulkan/VkPhysicalDevice;Ljava/util/Set;Ljava/util/Set;)Lcom/mojang/renderpearl/api/device/BackendCreationException;",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 1
    )
    private static Set<FeatureSet> qadish$filterRequiredIfExtensionsAvailable(Set<FeatureSet> requiredIfExtensionsAvailable, VkPhysicalDevice vkPhysicalDevice) {
        return FeatureCompat.filterFeatureSetsForDevice(requiredIfExtensionsAvailable, vkPhysicalDevice);
    }

    @ModifyVariable(
            method = "findPhysicalDevice(Lcom/mojang/renderpearl/backend/vulkan/VulkanInstance;Ljava/util/Set;Ljava/util/Set;)Lcom/mojang/renderpearl/backend/vulkan/VulkanPhysicalDevice;",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private static Set<FeatureSet> qadish$filterFindRequired(Set<FeatureSet> requiredFeatureSets) {
        return requiredFeatureSets;
    }
}
