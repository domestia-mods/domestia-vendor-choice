package domestia_vendor_choice;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Function;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;

public class ModBlocks {
	public static final Block VENDOR_MACHINE = register(
			"vendor_machine",
			VendorMachineBlock::new,
			BlockBehaviour.Properties.of()
					.strength(5.0f, 6.0f)
					.sound(SoundType.METAL),
			true
	);

	public static final Block VENDOR_SAFE = register(
			"vendor_safe",
			VendorSafeBlock::new,
			BlockBehaviour.Properties.of()
					.strength(5.0f, 6.0f)
					.sound(SoundType.METAL),
			true
	);

	public static final Block VENDOR_HOPPER = register(
			"vendor_hopper",
			VendorHopperBlock::new,
			BlockBehaviour.Properties.of()
					.strength(3.0f, 4.8f)
					.sound(SoundType.METAL)
					.noOcclusion(),
			true
	);

	public static final Block VENDOR_HOLO_DISPLAY = register(
			"vendor_holo_display",
			VendorHoloDisplayBlock::new,
			BlockBehaviour.Properties.of()
					.strength(3.0f, 4.8f)
					.sound(SoundType.METAL)
					.lightLevel(state -> VendorHoloDisplayBlock.LIGHT_EMISSION)
					.noOcclusion(),
			new Item.Properties().stacksTo(1)
	);

	public static final Block VENDOR_NOTE = register(
			"vendor_note",
			VendorNoteBlock::new,
			BlockBehaviour.Properties.of()
					.strength(2.5f, 3.0f)
					.sound(SoundType.WOOD)
					.noOcclusion(),
			new Item.Properties()
					.stacksTo(1)
					.component(ModDataComponents.VENDOR_NOTE_DATA, VendorNoteData.EMPTY)
	);

	private static Block register(
			String name,
			Function<BlockBehaviour.Properties, Block> blockFactory,
			BlockBehaviour.Properties settings,
			boolean shouldRegisterItem
	) {
		return register(name, blockFactory, settings, shouldRegisterItem ? new Item.Properties() : null);
	}

	private static Block register(
			String name,
			Function<BlockBehaviour.Properties, Block> blockFactory,
			BlockBehaviour.Properties settings,
			Item.Properties itemProperties
	) {
		ResourceKey<Block> blockKey = ResourceKey.create(
				Registries.BLOCK,
				Identifier.fromNamespaceAndPath(DomestiaVendorChoice.MOD_ID, name)
		);

		Block block = blockFactory.apply(settings.setId(blockKey));

		if (itemProperties != null) {
			registerBlockItem(name, block, itemProperties);
		}

		return Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
	}

	private static void registerBlockItem(String name, Block block, Item.Properties properties) {
		ResourceKey<Item> itemKey = ResourceKey.create(
				Registries.ITEM,
				Identifier.fromNamespaceAndPath(DomestiaVendorChoice.MOD_ID, name)
		);

		BlockItem blockItem = new BlockItem(
				block,
				properties
						.setId(itemKey)
						.useBlockDescriptionPrefix()
		);

		Registry.register(BuiltInRegistries.ITEM, itemKey, blockItem);
	}

	public static void initialize() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> {
			output.accept(VENDOR_MACHINE.asItem());
			output.accept(VENDOR_SAFE.asItem());
			output.accept(VENDOR_HOPPER.asItem());
			output.accept(VENDOR_HOLO_DISPLAY.asItem());
			output.accept(VENDOR_NOTE.asItem());
		});

		DomestiaVendorChoice.LOGGER.info("Registering Domestia Vendor Choice blocks.");
	}
}