package domestia_vendor_choice;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ModBlockEntities {
	public static final BlockEntityType<VendorMachineBlockEntity> VENDOR_MACHINE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			Identifier.fromNamespaceAndPath(DomestiaVendorChoice.MOD_ID, "vendor_machine"),
			FabricBlockEntityTypeBuilder.create(VendorMachineBlockEntity::new, ModBlocks.VENDOR_MACHINE).build()
	);

	public static final BlockEntityType<VendorSafeBlockEntity> VENDOR_SAFE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			Identifier.fromNamespaceAndPath(DomestiaVendorChoice.MOD_ID, "vendor_safe"),
			FabricBlockEntityTypeBuilder.create(VendorSafeBlockEntity::new, ModBlocks.VENDOR_SAFE).build()
	);

	public static final BlockEntityType<VendorHopperBlockEntity> VENDOR_HOPPER = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			Identifier.fromNamespaceAndPath(DomestiaVendorChoice.MOD_ID, "vendor_hopper"),
			FabricBlockEntityTypeBuilder.create(VendorHopperBlockEntity::new, ModBlocks.VENDOR_HOPPER).build()
	);

	public static void initialize() {
		DomestiaVendorChoice.LOGGER.info("Registering Domestia Vendor Choice block entities.");
	}
}