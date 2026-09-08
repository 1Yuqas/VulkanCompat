package one.yuqas.compat.render;

import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkAttachmentDescription;
import org.lwjgl.vulkan.VkAttachmentDescription2;
import org.lwjgl.vulkan.VkAttachmentReference2;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkRenderPassCreateInfo;
import org.lwjgl.vulkan.VkRenderPassCreateInfo2;
import org.lwjgl.vulkan.VkSubpassDependency2;
import org.lwjgl.vulkan.VkSubpassDescription2;

import java.nio.LongBuffer;
import java.util.Arrays;

import static one.yuqas.compat.render.VulkanConstants.*;

public final class RenderPassCache {
    private static final Object2LongOpenHashMap<RenderPassKey> CACHE = new Object2LongOpenHashMap<>(64);

    private RenderPassCache() {}

    public static long getOrCreate(VkDevice device, int[] colorFormats, int depthFormat,
                                   int[] colorLoadOps, int[] colorStoreOps,
                                   int depthLoadOp, int depthStoreOp) {
        RenderPassKey key = new RenderPassKey(device, colorFormats, depthFormat, colorLoadOps, colorStoreOps, depthLoadOp, depthStoreOp);
        if (CACHE.containsKey(key)) {
            return CACHE.getLong(key);
        }
        // Vulkan 1.2: try vkCreateRenderPass2 (Info2) first - more explicit, driver can optimize better
        long rp = tryCreateRenderPass2(device, colorFormats, depthFormat, colorLoadOps, colorStoreOps, depthLoadOp, depthStoreOp);
        if (rp != 0L) {
            CACHE.put(key, rp);
            return rp;
        }
        rp = createRenderPassLegacy(device, colorFormats, depthFormat, colorLoadOps, colorStoreOps, depthLoadOp, depthStoreOp);
        CACHE.put(key, rp);
        return rp;
    }

    private static long tryCreateRenderPass2(VkDevice device, int[] colorFormats, int depthFormat, int[] colorLoadOps, int[] colorStoreOps, int depthLoadOp, int depthStoreOp) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int colorCount = colorFormats.length;
            boolean hasDepth = depthFormat != VK_FORMAT_UNDEFINED;
            int attachmentCount = colorCount + (hasDepth ? 1 : 0);
            int depthIndex = colorCount;

            VkAttachmentDescription2.Buffer attachments = attachmentCount > 0 ? VkAttachmentDescription2.calloc(attachmentCount, stack) : null;
            if (attachments != null) {
                for (int i = 0; i < colorCount; i++) {
                    VkAttachmentDescription2 d = attachments.get(i);
                    d.sType$Default();
                    d.format(colorFormats[i]);
                    d.samples(1);
                    d.loadOp(colorLoadOps[i]);
                    d.storeOp(colorStoreOps[i]);
                    d.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE);
                    d.stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE);
                    d.initialLayout(VK_IMAGE_LAYOUT_GENERAL);
                    d.finalLayout(VK_IMAGE_LAYOUT_GENERAL);
                }
                if (hasDepth) {
                    VkAttachmentDescription2 d = attachments.get(depthIndex);
                    d.sType$Default();
                    d.format(depthFormat);
                    d.samples(1);
                    d.loadOp(depthLoadOp);
                    d.storeOp(depthStoreOp);
                    d.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_LOAD);
                    d.stencilStoreOp(VK_ATTACHMENT_STORE_OP_STORE);
                    d.initialLayout(VK_IMAGE_LAYOUT_GENERAL);
                    d.finalLayout(VK_IMAGE_LAYOUT_GENERAL);
                }
            }

            VkAttachmentReference2.Buffer colorRefs = colorCount > 0 ? VkAttachmentReference2.calloc(colorCount, stack) : null;
            if (colorRefs != null) {
                for (int i = 0; i < colorCount; i++) {
                    VkAttachmentReference2 r = colorRefs.get(i);
                    r.sType$Default();
                    r.attachment(i);
                    r.layout(VK_IMAGE_LAYOUT_GENERAL);
                    r.aspectMask(0);
                }
            }

            VkSubpassDescription2.Buffer subpasses = VkSubpassDescription2.calloc(1, stack);
            VkSubpassDescription2 subpass = subpasses.get(0);
            subpass.sType$Default();
            subpass.pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS);
            subpass.viewMask(0);
            subpass.colorAttachmentCount(colorCount);
            subpass.pColorAttachments(colorRefs);
            if (hasDepth) {
                VkAttachmentReference2 depthRef = VkAttachmentReference2.calloc(stack).sType$Default();
                depthRef.attachment(depthIndex);
                depthRef.layout(VK_IMAGE_LAYOUT_GENERAL);
                depthRef.aspectMask(0);
                subpass.pDepthStencilAttachment(depthRef);
            }

            VkSubpassDependency2.Buffer deps = VkSubpassDependency2.calloc(1, stack);
            VkSubpassDependency2 dep = deps.get(0);
            dep.sType$Default();
            dep.srcSubpass(VK_SUBPASS_EXTERNAL);
            dep.dstSubpass(0);
            int stages = VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT | VK_PIPELINE_STAGE_LATE_FRAGMENT_TESTS_BIT | VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
            int access = VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT | VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT;
            dep.srcStageMask(stages);
            dep.dstStageMask(stages);
            dep.srcAccessMask(0);
            dep.dstAccessMask(access);
            dep.dependencyFlags(0);
            dep.viewOffset(0);

            VkRenderPassCreateInfo2 ci = VkRenderPassCreateInfo2.calloc(stack).sType$Default();
            ci.pAttachments(attachments);
            ci.pSubpasses(subpasses);
            ci.pDependencies(deps);

            LongBuffer h = stack.callocLong(1);
            int result = VK12.nvkCreateRenderPass2(device, ci.address(), MemoryUtil.NULL, MemoryUtil.memAddress(h));
            if (result == 0) return h.get(0);
            return 0L;
        } catch (Throwable t) {
            return 0L;
        }
    }

    private static long createRenderPassLegacy(VkDevice device, int[] colorFormats, int depthFormat, int[] colorLoadOps, int[] colorStoreOps, int depthLoadOp, int depthStoreOp) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int colorCount = colorFormats.length;
            boolean hasDepth = depthFormat != VK_FORMAT_UNDEFINED;
            int attachmentCount = colorCount + (hasDepth ? 1 : 0);
            int depthIndex = colorCount;

            VkAttachmentDescription.Buffer attachments = attachmentCount > 0 ? VkAttachmentDescription.calloc(attachmentCount, stack) : null;
            for (int i = 0; i < colorCount; i++) {
                VkAttachmentDescription d = attachments.get(i);
                d.format(colorFormats[i]);
                d.samples(1);
                d.loadOp(colorLoadOps[i]);
                d.storeOp(colorStoreOps[i]);
                d.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE);
                d.stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE);
                d.initialLayout(VK_IMAGE_LAYOUT_GENERAL);
                d.finalLayout(VK_IMAGE_LAYOUT_GENERAL);
            }
            if (hasDepth) {
                VkAttachmentDescription d = attachments.get(depthIndex);
                d.format(depthFormat);
                d.samples(1);
                d.loadOp(depthLoadOp);
                d.storeOp(depthStoreOp);
                d.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_LOAD);
                d.stencilStoreOp(VK_ATTACHMENT_STORE_OP_STORE);
                d.initialLayout(VK_IMAGE_LAYOUT_GENERAL);
                d.finalLayout(VK_IMAGE_LAYOUT_GENERAL);
            }

            org.lwjgl.vulkan.VkAttachmentReference.Buffer colorRefs = colorCount > 0 ? org.lwjgl.vulkan.VkAttachmentReference.calloc(colorCount, stack) : null;
            for (int i = 0; i < colorCount; i++) {
                org.lwjgl.vulkan.VkAttachmentReference r = colorRefs.get(i);
                r.attachment(i);
                r.layout(VK_IMAGE_LAYOUT_GENERAL);
            }

            org.lwjgl.vulkan.VkSubpassDescription.Buffer subpasses = org.lwjgl.vulkan.VkSubpassDescription.calloc(1, stack);
            org.lwjgl.vulkan.VkSubpassDescription subpass = subpasses.get(0);
            subpass.pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS);
            subpass.colorAttachmentCount(colorCount);
            subpass.pColorAttachments(colorRefs);
            if (hasDepth) {
                org.lwjgl.vulkan.VkAttachmentReference depthRef = org.lwjgl.vulkan.VkAttachmentReference.calloc(stack);
                depthRef.attachment(depthIndex);
                depthRef.layout(VK_IMAGE_LAYOUT_GENERAL);
                subpass.pDepthStencilAttachment(depthRef);
            }

            org.lwjgl.vulkan.VkSubpassDependency.Buffer deps = org.lwjgl.vulkan.VkSubpassDependency.calloc(1, stack);
            org.lwjgl.vulkan.VkSubpassDependency dep = deps.get(0);
            dep.srcSubpass(VK_SUBPASS_EXTERNAL);
            dep.dstSubpass(0);
            int stages = VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT | VK_PIPELINE_STAGE_LATE_FRAGMENT_TESTS_BIT | VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
            int access = VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT | VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT;
            dep.srcStageMask(stages);
            dep.dstStageMask(stages);
            dep.srcAccessMask(0);
            dep.dstAccessMask(access);

            VkRenderPassCreateInfo ci = VkRenderPassCreateInfo.calloc(stack).sType$Default();
            ci.pAttachments(attachments);
            ci.pSubpasses(subpasses);
            ci.pDependencies(deps);

            LongBuffer h = stack.callocLong(1);
            int result = VK12.nvkCreateRenderPass(device, ci.address(), MemoryUtil.NULL, MemoryUtil.memAddress(h));
            if (result != 0) throw new IllegalStateException("VulkanCompat: failed to create render pass, result=" + result);
            return h.get(0);
        }
    }

    private static final class RenderPassKey {
        final VkDevice device;
        final int[] colorFormats;
        final int depthFormat;
        final int[] colorLoadOps;
        final int[] colorStoreOps;
        final int depthLoadOp;
        final int depthStoreOp;

        RenderPassKey(VkDevice device, int[] colorFormats, int depthFormat, int[] colorLoadOps, int[] colorStoreOps, int depthLoadOp, int depthStoreOp) {
            this.device = device;
            this.colorFormats = colorFormats;
            this.depthFormat = depthFormat;
            this.colorLoadOps = colorLoadOps;
            this.colorStoreOps = colorStoreOps;
            this.depthLoadOp = depthLoadOp;
            this.depthStoreOp = depthStoreOp;
        }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof RenderPassKey other)) return false;
            return device.equals(other.device) && depthFormat == other.depthFormat && depthLoadOp == other.depthLoadOp && depthStoreOp == other.depthStoreOp
                    && Arrays.equals(colorFormats, other.colorFormats) && Arrays.equals(colorLoadOps, other.colorLoadOps) && Arrays.equals(colorStoreOps, other.colorStoreOps);
        }
        @Override public int hashCode() {
            int r = device.hashCode();
            r = 31 * r + Arrays.hashCode(colorFormats);
            r = 31 * r + depthFormat;
            r = 31 * r + Arrays.hashCode(colorLoadOps);
            r = 31 * r + Arrays.hashCode(colorStoreOps);
            r = 31 * r + depthLoadOp;
            r = 31 * r + depthStoreOp;
            return r;
        }
    }
}
