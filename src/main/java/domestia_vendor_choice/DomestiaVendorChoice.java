package domestia_vendor_choice;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DomestiaVendorChoice implements ModInitializer {
	public static final String MOD_ID = "domestia_vendor_choice";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModBlocks.initialize();
		ModBlockEntities.initialize();
		ModMenus.initialize();
		ModEvents.initialize();

		LOGGER.info("Domestia Vendor Choice initialized.");
	}
}