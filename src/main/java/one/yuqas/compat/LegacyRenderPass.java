package one.yuqas.compat;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkAllocationCallbacks;
import org.lwjgl.vulkan.VkAttachmentDescription;
import org.lwjgl.vulkan.VkAttachmentReference;
import org.lwjgl.vulkan.VkClearValue;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkFramebufferCreateInfo;
import org.lwjgl.vulkan.VkGraphicsPipelineCreateInfo;
import org.lwjgl.vulkan.VkPipelineRenderingCreateInfo;
import org.lwjgl.vulkan.VkPipelineRenderingCreateInfoKHR;
import org.lwjgl.vulkan.VkRenderPassBeginInfo;
import org.lwjgl.vulkan.VkRenderPassCreateInfo;
import org.lwjgl.vulkan.VkRenderingAttachmentInfo;
import org.lwjgl.vulkan.VkRenderingInfo;
import org.lwjgl.vulkan.VkSubpassDependency;
import org.lwjgl.vulkan.VkSubpassDescription;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import java.util.Arrays;



public final class LegacyRenderPass {
    private static final int VK_FORMAT_UNDEFINED = 0;
    private static final int VK_IMAGE_LAYOUT_GENERAL = 1;
    private static final int VK_ATTACHMENT_LOAD_OP_LOAD = 0;
    private static final int VK_ATTACHMENT_LOAD_OP_CLEAR = 1;
    private static final int VK_ATTACHMENT_LOAD_OP_DONT_CARE = 2;
    private static final int VK_ATTACHMENT_STORE_OP_STORE = 0;
    private static final int VK_ATTACHMENT_STORE_OP_DONT_CARE = 1;
    private static final int VK_PIPELINE_BIND_POINT_GRAPHICS = 0;
    private static final int VK_SUBPASS_EXTERNAL = 0xFFFFFFFF;
    private static final int VK_SUBPASS_CONTENTS_INLINE = 0;
    private static final int VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT = 0x20;
    private static final int VK_PIPELINE_STAGE_LATE_FRAGMENT_TESTS_BIT = 0x40;
    private static final int VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT = 0x100;
    private static final int VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT = 0x40;
    private static final int VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT = 0x10;
    private static final Object2LongOpenHashMap<RenderPassKey> RENDER_PASSES = new Object2LongOpenHashMap<>(64);
    private static final Object2LongOpenHashMap<FramebufferKey> FRAMEBUFFERS = new Object2LongOpenHashMap<>(64);

    private LegacyRenderPass() {}

    static void invalidateFramebuffers(long imageView) {
        var it = FRAMEBUFFERS.object2LongEntrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            FramebufferKey key = entry.getKey();
            if (key.references(imageView)) {
                VK12.vkDestroyFramebuffer(key.device, entry.getLongValue(), null);
                it.remove();
            }
        }
    }

    public static void begin(VkCommandBuffer commandBuffer, VkRenderingInfo renderingInfo) {
        VkDevice device = commandBuffer.getDevice();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkRenderingAttachmentInfo.Buffer colorAttachmentInfos = renderingInfo.pColorAttachments();
            int declaredColorCount = renderingInfo.colorAttachmentCount();
            int totalColors = colorAttachmentInfos != null
                    && (declaredColorCount <= 0 || declaredColorCount > colorAttachmentInfos.remaining())
                    ? colorAttachmentInfos.remaining() : declaredColorCount;
            int colorCount = 0;
            if (colorAttachmentInfos != null) {
                for (int i = 0; i < totalColors; i++) {
                    if (colorAttachmentInfos.get(i).imageView() != 0L) {
                        colorCount++;
                    }
                }
            }
            VkRenderingAttachmentInfo depthAttachmentInfo = renderingInfo.pDepthAttachment();
            boolean hasDepth = depthAttachmentInfo != null && depthAttachmentInfo.imageView() != 0L;
            int attachmentCount = colorCount + (hasDepth ? 1 : 0);
            int depthIndex = colorCount;
            int[] colorFormats = new int[colorCount];
            int[] colorLoadOps = new int[colorCount];
            int[] colorStoreOps = new int[colorCount];
            long[] colorViews = new long[colorCount];
            VkClearValue.Buffer clearValues = VkClearValue.calloc(Math.max(1, attachmentCount), stack);
            int framebufferWidth = 0;
            int framebufferHeight = 0;
            int index = 0;
            if (colorAttachmentInfos != null) {
                for (int i = 0; i < totalColors; i++) {
                    VkRenderingAttachmentInfo attachment = colorAttachmentInfos.get(i);
                    long view = attachment.imageView();
                    if (view == 0L) {
                        continue;
                    }
                    ViewRegistry.ViewInfo info = ViewRegistry.get(view);
                    if (info == null) {
                        throw new IllegalStateException("VulkanCompat: unknown color image view " + view);
                    }
                    colorFormats[index] = info.format;
                    colorLoadOps[index] = attachment.loadOp();
                    colorStoreOps[index] = attachment.storeOp();
                    colorViews[index] = view;
                    if (framebufferWidth == 0) {
                        framebufferWidth = info.width;
                        framebufferHeight = info.height;
                    }
                    if (attachment.loadOp() == VK_ATTACHMENT_LOAD_OP_CLEAR) {
                        clearValues.get(index).set(attachment.clearValue());
                    }
                    index++;
                }
            }
            int depthFormat = VK_FORMAT_UNDEFINED;
            int depthLoadOp = VK_ATTACHMENT_LOAD_OP_DONT_CARE;
            int depthStoreOp = VK_ATTACHMENT_STORE_OP_DONT_CARE;
            if (hasDepth) {
                long depthView = depthAttachmentInfo.imageView();
                ViewRegistry.ViewInfo info = ViewRegistry.get(depthView);
                if (info == null) {
                    throw new IllegalStateException("VulkanCompat: unknown depth image view " + depthView);
                }
                depthFormat = info.format;
                depthLoadOp = depthAttachmentInfo.loadOp();
                depthStoreOp = depthAttachmentInfo.storeOp();
                if (framebufferWidth == 0) {
                    framebufferWidth = info.width;
                    framebufferHeight = info.height;
                }
                if (depthLoadOp == VK_ATTACHMENT_LOAD_OP_CLEAR) {
                    clearValues.get(depthIndex).set(depthAttachmentInfo.clearValue());
                }
            }
            long renderPass = getOrCreateRenderPass(device, colorFormats, depthFormat, colorLoadOps, colorStoreOps,
                    depthLoadOp, depthStoreOp);
            long framebuffer = 0L;
            if (attachmentCount > 0) {
                long[] views = new long[attachmentCount];
                System.arraycopy(colorViews, 0, views, 0, colorCount);
                if (hasDepth) {
                    views[colorCount] = depthAttachmentInfo.imageView();
                }
                framebuffer = getOrCreateFramebuffer(device, renderPass, views, framebufferWidth, framebufferHeight);
            }
            VkRenderPassBeginInfo beginInfo = VkRenderPassBeginInfo.calloc(stack).sType$Default();
            beginInfo.renderPass(renderPass);
            beginInfo.framebuffer(framebuffer);
            beginInfo.renderArea(renderingInfo.renderArea());
            beginInfo.clearValueCount(attachmentCount);
            beginInfo.pClearValues(clearValues);
            VK12.vkCmdBeginRenderPass(commandBuffer, beginInfo, VK_SUBPASS_CONTENTS_INLINE);
        }
    }

    public static int createGraphicsPipelines(VkDevice device, long pipelineCache,
                                              VkGraphicsPipelineCreateInfo.Buffer createInfos,
                                              VkAllocationCallbacks allocator, LongBuffer pPipelines) {
        long renderingInfoAddress = createInfos.pNext();
        if (renderingInfoAddress == 0L) {
            return VK12.vkCreateGraphicsPipelines(device, pipelineCache, createInfos, allocator, pPipelines);
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // New LWJGL technique: try core VkPipelineRenderingCreateInfo (VK13) first, fallback to KHR
            // Both share same layout (sType 1000044000), so either works - prefer core for 1.3+
            VkPipelineRenderingCreateInfo renderingInfo;
            try {
                renderingInfo = VkPipelineRenderingCreateInfo.create(renderingInfoAddress);
                // Validate sType is rendering create info (core or KHR share value)
                if (renderingInfo.sType() != VK13.VK_STRUCTURE_TYPE_PIPELINE_RENDERING_CREATE_INFO) {
                    renderingInfo = null;
                }
            } catch (Exception e) {
                renderingInfo = null;
            }
            VkPipelineRenderingCreateInfoKHR renderingInfoKHR = null;
            IntBuffer formatsBuffer;
            int declaredCount;
            if (renderingInfo != null) {
                formatsBuffer = renderingInfo.pColorAttachmentFormats();
                declaredCount = renderingInfo.colorAttachmentCount();
                // keep depth format from core struct
                // will be read below after setup
            } else {
                renderingInfoKHR = VkPipelineRenderingCreateInfoKHR.create(renderingInfoAddress);
                formatsBuffer = renderingInfoKHR.pColorAttachmentFormats();
                declaredCount = renderingInfoKHR.colorAttachmentCount();
            }
            int total = formatsBuffer != null
                    && (declaredCount <= 0 || declaredCount > formatsBuffer.remaining())
                    ? formatsBuffer.remaining() : declaredCount;
            int colorCount = 0;
            if (formatsBuffer != null) {
                for (int i = 0; i < total; i++) {
                    if (formatsBuffer.get(i) != VK_FORMAT_UNDEFINED) {
                        colorCount++;
                    }
                }
            }
            int[] colorFormats = new int[colorCount];
            int index = 0;
            if (formatsBuffer != null) {
                for (int i = 0; i < total; i++) {
                    int format = formatsBuffer.get(i);
                    if (format != VK_FORMAT_UNDEFINED) {
                        colorFormats[index++] = format;
                    }
                }
            }
            int[] loadOps = new int[colorCount];
            int[] storeOps = new int[colorCount];
            Arrays.fill(loadOps, VK_ATTACHMENT_LOAD_OP_LOAD);
            Arrays.fill(storeOps, VK_ATTACHMENT_STORE_OP_STORE);
            int depthFormat = renderingInfo != null ? renderingInfo.depthAttachmentFormat() : renderingInfoKHR.depthAttachmentFormat();
            long renderPass = getOrCreateRenderPass(device, colorFormats, depthFormat,
                    loadOps, storeOps, VK_ATTACHMENT_LOAD_OP_LOAD, VK_ATTACHMENT_STORE_OP_STORE);
            createInfos.renderPass(renderPass);
            createInfos.subpass(0);
            createInfos.pNext(0L);
            int result = VK12.vkCreateGraphicsPipelines(device, pipelineCache, createInfos, allocator, pPipelines);
            createInfos.pNext(renderingInfoAddress);
            return result;
        }
    }

    private static long getOrCreateRenderPass(VkDevice device, int[] colorFormats, int depthFormat,
                                              int[] colorLoadOps, int[] colorStoreOps,
                                              int depthLoadOp, int depthStoreOp) {
        RenderPassKey key = new RenderPassKey(device, colorFormats, depthFormat, colorLoadOps, colorStoreOps,
                depthLoadOp, depthStoreOp);
        Long existing = RENDER_PASSES.get(key);
        if (existing != null) {
            return existing;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int colorCount = colorFormats.length;
            boolean hasDepth = depthFormat != VK_FORMAT_UNDEFINED;
            int attachmentCount = colorCount + (hasDepth ? 1 : 0);
            int depthIndex = colorCount;
            VkAttachmentDescription.Buffer attachments = attachmentCount > 0
                    ? VkAttachmentDescription.calloc(attachmentCount, stack) : null;
            for (int i = 0; i < colorCount; i++) {
                VkAttachmentDescription description = attachments.get(i);
                description.format(colorFormats[i]);
                description.samples(1);
                description.loadOp(colorLoadOps[i]);
                description.storeOp(colorStoreOps[i]);
                description.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE);
                description.stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE);
                description.initialLayout(VK_IMAGE_LAYOUT_GENERAL);
                description.finalLayout(VK_IMAGE_LAYOUT_GENERAL);
            }
            if (hasDepth) {
                VkAttachmentDescription description = attachments.get(depthIndex);
                description.format(depthFormat);
                description.samples(1);
                description.loadOp(depthLoadOp);
                description.storeOp(depthStoreOp);
                description.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_LOAD);
                description.stencilStoreOp(VK_ATTACHMENT_STORE_OP_STORE);
                description.initialLayout(VK_IMAGE_LAYOUT_GENERAL);
                description.finalLayout(VK_IMAGE_LAYOUT_GENERAL);
            }
            VkAttachmentReference.Buffer colorReferences = colorCount > 0
                    ? VkAttachmentReference.calloc(colorCount, stack) : null;
            for (int i = 0; i < colorCount; i++) {
                VkAttachmentReference reference = colorReferences.get(i);
                reference.attachment(i);
                reference.layout(VK_IMAGE_LAYOUT_GENERAL);
            }
            VkSubpassDescription.Buffer subpasses = VkSubpassDescription.calloc(1, stack);
            VkSubpassDescription subpass = subpasses.get(0);
            subpass.pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS);
            subpass.colorAttachmentCount(colorCount);
            subpass.pColorAttachments(colorReferences);
            if (hasDepth) {
                VkAttachmentReference depthReference = VkAttachmentReference.calloc(stack);
                depthReference.attachment(depthIndex);
                depthReference.layout(VK_IMAGE_LAYOUT_GENERAL);
                subpass.pDepthStencilAttachment(depthReference);
            }
            VkSubpassDependency.Buffer dependencies = VkSubpassDependency.calloc(1, stack);
            VkSubpassDependency dependency = dependencies.get(0);
            dependency.srcSubpass(VK_SUBPASS_EXTERNAL);
            dependency.dstSubpass(0);
            int stages = VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT | VK_PIPELINE_STAGE_LATE_FRAGMENT_TESTS_BIT
                    | VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
            int access = VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT | VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT;
            dependency.srcStageMask(stages);
            dependency.dstStageMask(stages);
            dependency.srcAccessMask(0);
            dependency.dstAccessMask(access);
            VkRenderPassCreateInfo createInfo = VkRenderPassCreateInfo.calloc(stack).sType$Default();
            createInfo.pAttachments(attachments);
            createInfo.pSubpasses(subpasses);
            createInfo.pDependencies(dependencies);
            LongBuffer handle = stack.callocLong(1);
            int result = VK12.vkCreateRenderPass(device, createInfo, null, handle);
            if (result != 0) {
                throw new IllegalStateException("VulkanCompat: failed to create render pass, result=" + result);
            }
            long renderPass = handle.get(0);
            RENDER_PASSES.put(key, renderPass);
            return renderPass;
        }
    }

    private static long getOrCreateFramebuffer(VkDevice device, long renderPass, long[] imageViews,
                                               int width, int height) {
        FramebufferKey key = new FramebufferKey(device, renderPass, imageViews, width, height);
        Long existing = FRAMEBUFFERS.get(key);
        if (existing != null) {
            return existing;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkFramebufferCreateInfo createInfo = VkFramebufferCreateInfo.calloc(stack).sType$Default();
            createInfo.renderPass(renderPass);
            createInfo.pAttachments(stack.longs(imageViews));
            createInfo.width(width);
            createInfo.height(height);
            createInfo.layers(1);
            LongBuffer handle = stack.callocLong(1);
            int result = VK12.vkCreateFramebuffer(device, createInfo, null, handle);
            if (result != 0) {
                throw new IllegalStateException("VulkanCompat: failed to create framebuffer, result=" + result);
            }
            long framebuffer = handle.get(0);
            FRAMEBUFFERS.put(key, framebuffer);
            return framebuffer;
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

        RenderPassKey(VkDevice device, int[] colorFormats, int depthFormat, int[] colorLoadOps, int[] colorStoreOps,
                      int depthLoadOp, int depthStoreOp) {
            this.device = device;
            this.colorFormats = colorFormats;
            this.depthFormat = depthFormat;
            this.colorLoadOps = colorLoadOps;
            this.colorStoreOps = colorStoreOps;
            this.depthLoadOp = depthLoadOp;
            this.depthStoreOp = depthStoreOp;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof RenderPassKey other)) {
                return false;
            }
            return this.device.equals(other.device)
                    && this.depthFormat == other.depthFormat
                    && this.depthLoadOp == other.depthLoadOp
                    && this.depthStoreOp == other.depthStoreOp
                    && Arrays.equals(this.colorFormats, other.colorFormats)
                    && Arrays.equals(this.colorLoadOps, other.colorLoadOps)
                    && Arrays.equals(this.colorStoreOps, other.colorStoreOps);
        }

        @Override
        public int hashCode() {
            int result = this.device.hashCode();
            result = 31 * result + Arrays.hashCode(this.colorFormats);
            result = 31 * result + this.depthFormat;
            result = 31 * result + Arrays.hashCode(this.colorLoadOps);
            result = 31 * result + Arrays.hashCode(this.colorStoreOps);
            result = 31 * result + this.depthLoadOp;
            result = 31 * result + this.depthStoreOp;
            return result;
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
            for (long view : this.imageViews) {
                if (view == imageView) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof FramebufferKey other)) {
                return false;
            }
            return this.device.equals(other.device)
                    && this.renderPass == other.renderPass
                    && this.width == other.width
                    && this.height == other.height
                    && Arrays.equals(this.imageViews, other.imageViews);
        }

        @Override
        public int hashCode() {
            int result = this.device.hashCode();
            result = 31 * result + (int) (this.renderPass ^ (this.renderPass >>> 32));
            result = 31 * result + Arrays.hashCode(this.imageViews);
            result = 31 * result + this.width;
            result = 31 * result + this.height;
            return result;
        }
    }
}
