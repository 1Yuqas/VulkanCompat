package one.yuqas.compat.view;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import one.yuqas.compat.render.LegacyRenderPass;

public final class ViewRegistry {
    private static final Long2LongOpenHashMap VIEWS = new Long2LongOpenHashMap(128);
    static {
        VIEWS.defaultReturnValue(Long.MIN_VALUE);
    }

    private ViewRegistry() {}

    // pack: high 32 = vkFormat, mid 16 = width, low 16 = height (16384 max, fits 16 bits)
    private static long pack(int vkFormat, int width, int height) {
        return ((long) vkFormat << 32) | ((long) (width & 0xFFFF) << 16) | (height & 0xFFFF);
    }

    public static void register(long imageView, int vkFormat, int width, int height) {
        VIEWS.put(imageView, pack(vkFormat, width, height));
    }

    public static void unregister(long imageView) {
        VIEWS.remove(imageView);
        LegacyRenderPass.invalidateFramebuffers(imageView);
    }

    public static long getPacked(long imageView) {
        return VIEWS.get(imageView);
    }

    public static boolean contains(long imageView) {
        return VIEWS.containsKey(imageView);
    }

    // legacy object view for compatibility, zero-alloc unpack
    public static ViewInfo get(long imageView) {
        long packed = VIEWS.get(imageView);
        if (packed == Long.MIN_VALUE) return null;
        return new ViewInfo((int) (packed >>> 32), (int) ((packed >>> 16) & 0xFFFF), (int) (packed & 0xFFFF));
    }

    public static int getFormat(long packed) {
        return (int) (packed >>> 32);
    }

    public static int getWidth(long packed) {
        return (int) ((packed >>> 16) & 0xFFFF);
    }

    public static int getHeight(long packed) {
        return (int) (packed & 0xFFFF);
    }

    public static final class ViewInfo {
        public final int format;
        public final int width;
        public final int height;

        ViewInfo(int format, int width, int height) {
            this.format = format;
            this.width = width;
            this.height = height;
        }
    }
}
