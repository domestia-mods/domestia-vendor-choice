package domestia_vendor_choice;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;

public class ModEvents {
	// Translation keys.
	private static final String ID_MESSAGE_MACHINE_BREAK_DENIED = "message.domestia_vendor_choice.vendor_machine_break_denied";
	private static final String ID_MESSAGE_SAFE_BREAK_DENIED = "message.domestia_vendor_choice.vendor_safe_break_denied";
	private static final String ID_MESSAGE_HOPPER_BREAK_DENIED = "message.domestia_vendor_choice.vendor_hopper_break_denied";
	private static final String ID_MESSAGE_STAND_BREAK_DENIED = "message.domestia_vendor_choice.vendor_stand_break_denied";

	// Feedback messages.
	private static final Component MESSAGE_MACHINE_BREAK_DENIED = Component.translatable(ID_MESSAGE_MACHINE_BREAK_DENIED);
	private static final Component MESSAGE_SAFE_BREAK_DENIED = Component.translatable(ID_MESSAGE_SAFE_BREAK_DENIED);
	private static final Component MESSAGE_HOPPER_BREAK_DENIED = Component.translatable(ID_MESSAGE_HOPPER_BREAK_DENIED);
	private static final Component MESSAGE_STAND_BREAK_DENIED = Component.translatable(ID_MESSAGE_STAND_BREAK_DENIED);

	public static void initialize() {
		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
			if (state.is(ModBlocks.VENDOR_MACHINE)) {
				return handleVendorMachineBreak(level, player, pos, blockEntity);
			}

			if (state.is(ModBlocks.VENDOR_SAFE)) {
				return handleVendorSafeBreak(level, player, pos, blockEntity);
			}

			if (state.is(ModBlocks.VENDOR_HOPPER)) {
				return handleVendorHopperBreak(level, player, pos, blockEntity);
			}

			if (state.is(ModBlocks.VENDOR_STAND)) {
				return handleVendorStandBreak(level, player, pos, blockEntity);
			}

			return true;
		});

		DomestiaVendorChoice.LOGGER.info("Registering Domestia Vendor Choice events.");
	}

	private static boolean handleVendorMachineBreak(Level level, Player player, BlockPos pos, BlockEntity blockEntity) {
		if (!(blockEntity instanceof VendorMachineBlockEntity vendorMachineBlockEntity)) {
			return true;
		}

		if (!vendorMachineBlockEntity.canBreak(player)) {
			player.sendSystemMessage(MESSAGE_MACHINE_BREAK_DENIED);
			return false;
		}

		handlePrivateContainerBreak(level, player, pos, vendorMachineBlockEntity);

		return true;
	}

	private static boolean handleVendorSafeBreak(Level level, Player player, BlockPos pos, BlockEntity blockEntity) {
		if (!(blockEntity instanceof VendorSafeBlockEntity vendorSafeBlockEntity)) {
			return true;
		}

		if (!vendorSafeBlockEntity.canBreak(player)) {
			player.sendSystemMessage(MESSAGE_SAFE_BREAK_DENIED);
			return false;
		}

		handlePrivateContainerBreak(level, player, pos, vendorSafeBlockEntity);

		return true;
	}


	private static boolean handleVendorHopperBreak(Level level, Player player, BlockPos pos, BlockEntity blockEntity) {
		if (!(blockEntity instanceof VendorHopperBlockEntity vendorHopperBlockEntity)) {
			return true;
		}

		if (!vendorHopperBlockEntity.canBreak(player)) {
			player.sendSystemMessage(MESSAGE_HOPPER_BREAK_DENIED);
			return false;
		}

		handlePrivateContainerBreak(level, player, pos, vendorHopperBlockEntity);

		return true;
	}

	private static boolean handleVendorStandBreak(Level level, Player player, BlockPos pos, BlockEntity blockEntity) {
		if (!(blockEntity instanceof VendorStandBlockEntity vendorStandBlockEntity)) {
			return true;
		}

		if (!vendorStandBlockEntity.canBreak(player)) {
			player.sendSystemMessage(MESSAGE_STAND_BREAK_DENIED);
			return false;
		}

		if (!level.isClientSide()) {
			Block.popResource(level, pos, vendorStandBlockEntity.createPreservedItemStack());
		}

		return true;
	}

	private static void handlePrivateContainerBreak(Level level, Player player, BlockPos pos, Container container) {
		if (level.isClientSide()) {
			return;
		}

		if (container instanceof VendorMachineBlockEntity vendorMachineBlockEntity) {
			handleVendorMachineContentsBreak(level, player, pos, vendorMachineBlockEntity);
			return;
		}

		if (container instanceof VendorSafeBlockEntity vendorSafeBlockEntity) {
			handleVendorSafeContentsBreak(level, player, pos, vendorSafeBlockEntity);
			return;
		}

		if (container instanceof VendorHopperBlockEntity vendorHopperBlockEntity) {
			handleVendorHopperContentsBreak(level, player, pos, vendorHopperBlockEntity);
		}
	}

	private static void handleVendorMachineContentsBreak(Level level, Player player, BlockPos pos, VendorMachineBlockEntity vendorMachineBlockEntity) {
		if (vendorMachineBlockEntity.isOwner(player.getUUID())) {
			dropContainerContents(level, pos, vendorMachineBlockEntity);
		}

		vendorMachineBlockEntity.clearContent();
	}

	private static void handleVendorSafeContentsBreak(Level level, Player player, BlockPos pos, VendorSafeBlockEntity vendorSafeBlockEntity) {
		if (vendorSafeBlockEntity.isOwner(player.getUUID())) {
			dropContainerContents(level, pos, vendorSafeBlockEntity);
		}

		vendorSafeBlockEntity.clearContent();
	}

	private static void handleVendorHopperContentsBreak(Level level, Player player, BlockPos pos, VendorHopperBlockEntity vendorHopperBlockEntity) {
		if (vendorHopperBlockEntity.isOwner(player.getUUID())) {
			dropContainerContents(level, pos, vendorHopperBlockEntity);
		}

		vendorHopperBlockEntity.clearContent();
	}

	private static void dropContainerContents(Level level, BlockPos pos, Container container) {
		Containers.dropContents(level, pos, container);
	}
}