package domestia_vendor_choice;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

import java.util.function.Function;

public final class ModItems {
	public static final Item VENDOR_SCRAP = register(
			"vendor_scrap",
			VendorScrapItem::new,
			new Item.Properties().stacksTo(64)
	);

	private ModItems() {
	}

	private static Item register(String name, Function<Item.Properties, Item> itemFactory, Item.Properties properties) {
		ResourceKey<Item> itemKey = ResourceKey.create(
				Registries.ITEM,
				Identifier.fromNamespaceAndPath(DomestiaVendorChoice.MOD_ID, name)
		);

		Item item = itemFactory.apply(properties.setId(itemKey));

		return Registry.register(BuiltInRegistries.ITEM, itemKey, item);
	}

	public static void initialize() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> {
			output.accept(VENDOR_SCRAP);
		});

		DomestiaVendorChoice.LOGGER.info("Registering Domestia Vendor Choice items.");
	}
}
