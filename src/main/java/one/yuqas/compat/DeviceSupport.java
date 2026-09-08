package one.yuqas.compat;

import com.mojang.blaze3d.vulkan.VulkanPhysicalDevice;
import org.lwjgl.vulkan.VkDevice;

import java.util.HashMap;
import java.util.Map;

public final class DeviceSupport {
    private static final String DYNAMIC_RENDERING_EXTENSION = "VK_KHR_dynamic_rendering";
    private static final Map<Long, Boolean> CACHE = new HashMap<>();

    private DeviceSupport() {}

    public static boolean supportsDynamicRendering(VkDevice device) {
        long key = device.getPhysicalDevice().address();
        Boolean cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        boolean supported = true;
        try (VulkanPhysicalDevice physicalDevice = new VulkanPhysicalDevice(device.getPhysicalDevice())) {
            supported = physicalDevice.hasDeviceExtension(DYNAMIC_RENDERING_EXTENSION);
        } catch (Exception ignored) {
        }
        CACHE.put(key, supported);
        return supported;
    }

    public static boolean deviceSupportsDynamicRendering(org.lwjgl.vulkan.VkPhysicalDevice vkPhysicalDevice) {
        try (VulkanPhysicalDevice physicalDevice = new VulkanPhysicalDevice(vkPhysicalDevice)) {
            return physicalDevice.hasDeviceExtension(DYNAMIC_RENDERING_EXTENSION);
        } catch (Exception e) {
            return false;
        }
    }

    static String extensionName() {
        return DYNAMIC_RENDERING_EXTENSION;
    }

    static String featureName() {
        return "dynamicRendering";
    }
}
