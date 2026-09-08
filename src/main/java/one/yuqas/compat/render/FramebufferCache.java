package one.yuqas.compat.render;

import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkFramebufferCreateInfo;

import java.nio.LongBuffer;
import java.util.Arrays;

public final class FramebufferCache {
    private static final Object2LongOpenHashMap<FramebufferKey> CACHE = new Object2LongOpenHashMap<>(64);

    private FramebufferCache() {}

    public static long getOrCreate(VkDevice device, long renderPass, long[] imageViews, int width, int height) {
        FramebufferKey key = new FramebufferKey(device, renderPass, imageViews, width, height);
        if (CACHE.containsKey(key)) {
            return CACHE.getLong(key);
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkFramebufferCreateInfo ci = VkFramebufferCreateInfo.calloc(stack).sType$Default();
            ci.renderPass(renderPass);
            ci.pAttachments(stack.longs(imageViews));
            ci.width(width);
            ci.height(height);
            ci.layers(1);
            LongBuffer h = stack.callocLong(1);
            // Vulkan 1.2: direct n-call + stack.longs already uses MemoryUtil, zero-copy
            int result = VK12.nvkCreateFramebuffer(device, ci.address(), MemoryUtil.NULL, MemoryUtil.memAddress(h));
            if (result != 0) throw new IllegalStateException("VulkanCompat: failed to create framebuffer, result=" + result);
            long fb = h.get(0);
            CACHE.put(key, fb);
            return fb;
        }
    }

    public static void invalidate(long imageView) {
        var it = CACHE.object2LongEntrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            FramebufferKey k = e.getKey();
            if (k.references(imageView)) {
                // Vulkan 1.2: direct destroy via n-method would be VK12.nvkDestroyFramebuffer but wrapper is fine for rare path
                VK12.vkDestroyFramebuffer(k.device, e.getLongValue(), null);
                it.remove();
            }
        }
    }

    private static final class FramebufferKey {
        final VkDevice device;
        final long renderPass;
        final long[] imageViews;
        final int width;
        final int height;

        FramebufferKey(VkDevice device, long renderPass, long[] imageViews, int width, int height) {
            this.device = device;
            this.renderPass = renderPass;
            this.imageViews = imageViews;
            this.width = width;
            this.height = height;
        }

        boolean references(long imageView) {
            for (long v : imageViews) if (v == imageView) return true;
            return false;
        }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof FramebufferKey other)) return false;
            return device.equals(other.device) && renderPass == other.renderPass && width == other.width && height == other.height && Arrays.equals(imageViews, other.imageViews);
        }
        @Override public int hashCode() {
            int r = device.hashCode();
            r = 31 * r + (int) (renderPass ^ (renderPass >>> 32));
            r = 31 * r + Arrays.hashCode(imageViews);
            r = 31 * r + width;
            r = 31 * r + height;
            return r;
        }
    }
}
