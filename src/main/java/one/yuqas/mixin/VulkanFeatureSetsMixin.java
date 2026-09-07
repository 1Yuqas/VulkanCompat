package one.yuqas.mixin;

import com.mojang.renderpearl.backend.vulkan.VulkanFeatureSets;
import com.mojang.renderpearl.backend.vulkan.init.FeatureSet;
import one.yuqas.compat.FeatureCompat;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VulkanFeatureSets.class)
public abstract class VulkanFeatureSetsMixin {

    @Shadow @Final @Mutable
    public static FeatureSet REQUIRED_FEATURESET;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void qadish$stripDynamicRendering(CallbackInfo ci) {
        FeatureSet stripped = FeatureCompat.stripFeatureSet(REQUIRED_FEATURESET);
        if (stripped != REQUIRED_FEATURESET) {
            REQUIRED_FEATURESET = stripped;
        }
    }
}
