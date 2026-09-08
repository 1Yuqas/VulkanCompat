package one.yuqas.compat;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

public final class ViewRegistry {
    private static final Long2ObjectOpenHashMap<ViewInfo> VIEWS = new Long2ObjectOpenHashMap<>(64);

    private ViewRegistry() {}

    public static void register(long imageView, int vkFormat, int width, int height) {
        VIEWS.put(imageView, new ViewInfo(vkFormat, width, height));
    }

    public static void unregister(long imageView) {
        VIEWS.remove(imageView);
        LegacyRenderPass.invalidateFramebuffers(imageView);
    }

    static ViewInfo get(long imageView) {
        return VIEWS.get(imageView);
    }

    static final class ViewInfo {
        final int format;
        final int width;
        final int height;

        ViewInfo(int format, int width, int height) {
            this.format = format;
            this.width = width;
            this.height = height;
        }
    }
}
