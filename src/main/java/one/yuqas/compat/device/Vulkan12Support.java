package one.yuqas.compat.device;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures2;
import org.lwjgl.vulkan.VkPhysicalDeviceVulkan12Features;

public final class Vulkan12Support {
    private Vulkan12Support() {}

    public static boolean supportsTimelineSemaphore(VkPhysicalDevice vkPhysicalDevice) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPhysicalDeviceVulkan12Features f12 = VkPhysicalDeviceVulkan12Features.calloc(stack).sType$Default();
            VkPhysicalDeviceFeatures2 f2 = VkPhysicalDeviceFeatures2.calloc(stack).sType$Default();
            f2.pNext(f12.address());
            org.lwjgl.vulkan.VK11.vkGetPhysicalDeviceFeatures2(vkPhysicalDevice, f2);
            return f12.timelineSemaphore();
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean supportsHostQueryReset(VkPhysicalDevice vkPhysicalDevice) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPhysicalDeviceVulkan12Features f12 = VkPhysicalDeviceVulkan12Features.calloc(stack).sType$Default();
            VkPhysicalDeviceFeatures2 f2 = VkPhysicalDeviceFeatures2.calloc(stack).sType$Default();
            f2.pNext(f12.address());
            org.lwjgl.vulkan.VK11.vkGetPhysicalDeviceFeatures2(vkPhysicalDevice, f2);
            return f12.hostQueryReset();
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean supportsBufferDeviceAddress(VkPhysicalDevice vkPhysicalDevice) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPhysicalDeviceVulkan12Features f12 = VkPhysicalDeviceVulkan12Features.calloc(stack).sType$Default();
            VkPhysicalDeviceFeatures2 f2 = VkPhysicalDeviceFeatures2.calloc(stack).sType$Default();
            f2.pNext(f12.address());
            org.lwjgl.vulkan.VK11.vkGetPhysicalDeviceFeatures2(vkPhysicalDevice, f2);
            return f12.bufferDeviceAddress();
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean supportsDescriptorIndexing(VkPhysicalDevice vkPhysicalDevice) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPhysicalDeviceVulkan12Features f12 = VkPhysicalDeviceVulkan12Features.calloc(stack).sType$Default();
            VkPhysicalDeviceFeatures2 f2 = VkPhysicalDeviceFeatures2.calloc(stack).sType$Default();
            f2.pNext(f12.address());
            org.lwjgl.vulkan.VK11.vkGetPhysicalDeviceFeatures2(vkPhysicalDevice, f2);
            return f12.descriptorIndexing();
        } catch (Exception e) {
            return false;
        }
    }
}
