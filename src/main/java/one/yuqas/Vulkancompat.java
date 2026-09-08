package one.yuqas;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Vulkancompat implements ModInitializer {
    public static final String MOD_ID = "vulkancompat";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("VulkanCompat loaded. Legacy render pass fallback is enabled.");
    }
}
