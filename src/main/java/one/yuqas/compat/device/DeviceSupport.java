package one.yuqas.compat.device;

import com.mojang.blaze3d.vulkan.VulkanPhysicalDevice;
import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import org.lwjgl.vulkan.VkDevice;

public final class DeviceSupport {
    private static final String DYNAMIC_RENDERING_EXTENSION = "VK_KHR_dynamic_rendering";
    private static final Long2BooleanOpenHashMap CACHE = new Long2BooleanOpenHashMap(16);

    private DeviceSupport() {}

    public static boolean supportsDynamicRendering(VkDevice device) {
        long key = device.getPhysicalDevice().address();
        if (CACHE.containsKey(key)) {
            return CACHE.get(key);
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

    public static String extensionName() {
        return DYNAMIC_RENDERING_EXTENSION;
    }

    public static String featureName() {
        return "dynamicRendering";
    }
}
