package one.yuqas.compat;

public final class VulkanConstants {
    private VulkanConstants() {}

    public static final int VK_FORMAT_UNDEFINED = 0;
    public static final int VK_IMAGE_LAYOUT_GENERAL = 1;
    public static final int VK_ATTACHMENT_LOAD_OP_LOAD = 0;
    public static final int VK_ATTACHMENT_LOAD_OP_CLEAR = 1;
    public static final int VK_ATTACHMENT_LOAD_OP_DONT_CARE = 2;
    public static final int VK_ATTACHMENT_STORE_OP_STORE = 0;
    public static final int VK_ATTACHMENT_STORE_OP_DONT_CARE = 1;
    public static final int VK_PIPELINE_BIND_POINT_GRAPHICS = 0;
    public static final int VK_SUBPASS_EXTERNAL = 0xFFFFFFFF;
    public static final int VK_SUBPASS_CONTENTS_INLINE = 0;
    public static final int VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT = 0x20;
    public static final int VK_PIPELINE_STAGE_LATE_FRAGMENT_TESTS_BIT = 0x40;
    public static final int VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT = 0x100;
    public static final int VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT = 0x40;
    public static final int VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT = 0x10;
}
