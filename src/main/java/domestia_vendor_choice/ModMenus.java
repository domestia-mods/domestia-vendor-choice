package domestia_vendor_choice;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

public class ModMenus {
	public static final MenuType<VendorMachineControlMenu> VENDOR_MACHINE_CONTROL = Registry.register(
			BuiltInRegistries.MENU,
			Identifier.fromNamespaceAndPath(DomestiaVendorChoice.MOD_ID, "vendor_machine_control"),
			new MenuType<>(VendorMachineControlMenu::new, FeatureFlags.DEFAULT_FLAGS)
	);

	public static final MenuType<VendorMachineSalesMenu> VENDOR_MACHINE_SALES = Registry.register(
			BuiltInRegistries.MENU,
			Identifier.fromNamespaceAndPath(DomestiaVendorChoice.MOD_ID, "vendor_machine_sales"),
			new MenuType<>(VendorMachineSalesMenu::new, FeatureFlags.DEFAULT_FLAGS)
	);

	public static final MenuType<VendorSafeMenu> VENDOR_SAFE = Registry.register(
			BuiltInRegistries.MENU,
			Identifier.fromNamespaceAndPath(DomestiaVendorChoice.MOD_ID, "vendor_safe"),
			new MenuType<>(VendorSafeMenu::new, FeatureFlags.DEFAULT_FLAGS)
	);

	public static final MenuType<VendorHoloDisplayMenu> VENDOR_HOLO_DISPLAY_CONTROL = Registry.register(
			BuiltInRegistries.MENU,
			Identifier.fromNamespaceAndPath(DomestiaVendorChoice.MOD_ID, "vendor_holo_display_control"),
			new MenuType<>(VendorHoloDisplayMenu::new, FeatureFlags.DEFAULT_FLAGS)
	);

	public static final MenuType<VendorHopperMenu> VENDOR_HOPPER = Registry.register(
			BuiltInRegistries.MENU,
			Identifier.fromNamespaceAndPath(DomestiaVendorChoice.MOD_ID, "vendor_hopper"),
			new MenuType<>(VendorHopperMenu::new, FeatureFlags.DEFAULT_FLAGS)
	);

	public static void initialize() {
		DomestiaVendorChoice.LOGGER.info("Registering Domestia Vendor Choice menus.");
	}
}