package domestia_vendor_choice;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public final class ModDataComponents {
	public static final DataComponentType<VendorStandData> VENDOR_STAND_DATA = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE,
			Identifier.fromNamespaceAndPath(DomestiaVendorChoice.MOD_ID, "vendor_stand_data"),
			DataComponentType.<VendorStandData>builder()
					.persistent(VendorStandData.CODEC)
					.build()
	);

	private ModDataComponents() {
	}

	public static void initialize() {
		DomestiaVendorChoice.LOGGER.info("Registering Domestia Vendor Choice data components.");
	}
}
