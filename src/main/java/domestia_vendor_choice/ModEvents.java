package domestia_vendor_choice;

import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public class ModEvents {
	private static final float VENDOR_SCRAP_DROP_CHANCE = 0.05f;

	// Translation keys.
	private static final String ID_MESSAGE_MACHINE_BREAK_DENIED = "message.domestia_vendor_choice.vendor_machine.break_denied";
	private static final String ID_MESSAGE_SAFE_BREAK_DENIED = "message.domestia_vendor_choice.vendor_safe.break_denied";
	private static final String ID_MESSAGE_HOPPER_BREAK_DENIED = "message.domestia_vendor_choice.vendor_hopper.break_denied";
	private static final String ID_MESSAGE_NOTE_BREAK_DENIED = "message.domestia_vendor_choice.vendor_note.break_denied";
	private static final String ID_MESSAGE_HOLO_DISPLAY_BREAK_DENIED = "message.domestia_vendor_choice.vendor_holo_display.break_denied";
	private static final String ID_MESSAGE_RECEIVER_BREAK_DENIED = "message.domestia_vendor_choice.vendor_receiver.break_denied";

	// Feedback messages.
	private static final Component MESSAGE_MACHINE_BREAK_DENIED = Component.translatable(ID_MESSAGE_MACHINE_BREAK_DENIED);
	private static final Component MESSAGE_SAFE_BREAK_DENIED = Component.translatable(ID_MESSAGE_SAFE_BREAK_DENIED);
	private static final Component MESSAGE_HOPPER_BREAK_DENIED = Component.translatable(ID_MESSAGE_HOPPER_BREAK_DENIED);
	private static final Component MESSAGE_NOTE_BREAK_DENIED = Component.translatable(ID_MESSAGE_NOTE_BREAK_DENIED);
	private static final Component MESSAGE_HOLO_DISPLAY_BREAK_DENIED = Component.translatable(ID_MESSAGE_HOLO_DISPLAY_BREAK_DENIED);
	private static final Component MESSAGE_RECEIVER_BREAK_DENIED = Component.translatable(ID_MESSAGE_RECEIVER_BREAK_DENIED);

	public static void initialize() {
		AttackBlockCallback.EVENT.register(ModEvents::handleProtectedVendorBlockAttack);

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

			if (state.is(ModBlocks.VENDOR_HOLO_DISPLAY)) {
				return handleVendorHoloDisplayBreak(level, player, pos, blockEntity);
			}

			if (state.is(ModBlocks.VENDOR_RECEIVER)) {
				return handleVendorReceiverBreak(level, player, pos, blockEntity);
			}

			if (state.is(ModBlocks.VENDOR_NOTE)) {
				return handleVendorNoteBreak(level, player, pos, blockEntity);
			}

			return true;
		});

		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
			tryDropVendorScrap(level, player, pos, state);
		});

		DomestiaVendorChoice.LOGGER.info("Registering Domestia Vendor Choice events.");
	}

	private static InteractionResult handleProtectedVendorBlockAttack(
			Player player,
			Level level,
			InteractionHand hand,
			BlockPos pos,
			Direction direction
	) {
		BlockEntity blockEntity = level.getBlockEntity(pos);
		Component deniedMessage = getProtectedVendorBreakDeniedMessage(player, blockEntity);

		if (deniedMessage == null) {
			return InteractionResult.PASS;
		}

		denyProtectedVendorBreak(level, player, pos, deniedMessage);
		return InteractionResult.FAIL;
	}

	private static Component getProtectedVendorBreakDeniedMessage(Player player, BlockEntity blockEntity) {
		if (blockEntity instanceof VendorMachineBlockEntity vendorMachineBlockEntity) {
			return vendorMachineBlockEntity.canBreak(player) ? null : MESSAGE_MACHINE_BREAK_DENIED;
		}

		if (blockEntity instanceof VendorSafeBlockEntity vendorSafeBlockEntity) {
			return vendorSafeBlockEntity.canBreak(player) ? null : MESSAGE_SAFE_BREAK_DENIED;
		}

		if (blockEntity instanceof VendorHopperBlockEntity vendorHopperBlockEntity) {
			return vendorHopperBlockEntity.canBreak(player) ? null : MESSAGE_HOPPER_BREAK_DENIED;
		}

		if (blockEntity instanceof VendorHoloDisplayBlockEntity vendorHoloDisplayBlockEntity) {
			return vendorHoloDisplayBlockEntity.canBreak(player) ? null : MESSAGE_HOLO_DISPLAY_BREAK_DENIED;
		}

		if (blockEntity instanceof VendorReceiverBlockEntity vendorReceiverBlockEntity) {
			return vendorReceiverBlockEntity.canBreak(player) ? null : MESSAGE_RECEIVER_BREAK_DENIED;
		}

		if (blockEntity instanceof VendorNoteBlockEntity vendorNoteBlockEntity) {
			return vendorNoteBlockEntity.canBreak(player) ? null : MESSAGE_NOTE_BREAK_DENIED;
		}

		return null;
	}

	private static void denyProtectedVendorBreak(Level level, Player player, BlockPos pos, Component message) {
		if (level.isClientSide()) {
			return;
		}

		player.sendSystemMessage(message);

		BlockState state = level.getBlockState(pos);
		level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
	}

	private static void tryDropVendorScrap(Level level, Player player, BlockPos pos, BlockState state) {
		if (level.isClientSide() || player.isCreative()) {
			return;
		}

		if (!isVendorScrapOre(state)) {
			return;
		}

		ItemStack toolStack = player.getMainHandItem();

		if (!player.hasCorrectToolForDrops(state) || hasSilkTouch(level, toolStack)) {
			return;
		}

		if (level.getRandom().nextFloat() >= VENDOR_SCRAP_DROP_CHANCE) {
			return;
		}

		Block.popResource(level, pos, new ItemStack(ModItems.VENDOR_SCRAP));
	}

	private static boolean isVendorScrapOre(BlockState state) {
		return state.is(Blocks.IRON_ORE)
				|| state.is(Blocks.DEEPSLATE_IRON_ORE)
				|| state.is(Blocks.COPPER_ORE)
				|| state.is(Blocks.DEEPSLATE_COPPER_ORE)
				|| state.is(Blocks.GOLD_ORE)
				|| state.is(Blocks.DEEPSLATE_GOLD_ORE);
	}

	private static boolean hasSilkTouch(Level level, ItemStack stack) {
		return EnchantmentHelper.getItemEnchantmentLevel(
				level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH),
				stack
		) > 0;
	}

	private static boolean handleVendorMachineBreak(Level level, Player player, BlockPos pos, BlockEntity blockEntity) {
		if (!(blockEntity instanceof VendorMachineBlockEntity vendorMachineBlockEntity)) {
			return true;
		}

		if (!vendorMachineBlockEntity.canBreak(player)) {
			denyProtectedVendorBreak(level, player, pos, MESSAGE_MACHINE_BREAK_DENIED);
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
			denyProtectedVendorBreak(level, player, pos, MESSAGE_SAFE_BREAK_DENIED);
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
			denyProtectedVendorBreak(level, player, pos, MESSAGE_HOPPER_BREAK_DENIED);
			return false;
		}

		handlePrivateContainerBreak(level, player, pos, vendorHopperBlockEntity);

		return true;
	}

	private static boolean handleVendorHoloDisplayBreak(Level level, Player player, BlockPos pos, BlockEntity blockEntity) {
		if (!(blockEntity instanceof VendorHoloDisplayBlockEntity vendorHoloDisplayBlockEntity)) {
			return true;
		}

		if (!vendorHoloDisplayBlockEntity.canBreak(player)) {
			denyProtectedVendorBreak(level, player, pos, MESSAGE_HOLO_DISPLAY_BREAK_DENIED);
			return false;
		}

		handlePrivateContainerBreak(level, player, pos, vendorHoloDisplayBlockEntity);

		return true;
	}

	private static boolean handleVendorReceiverBreak(Level level, Player player, BlockPos pos, BlockEntity blockEntity) {
		if (!(blockEntity instanceof VendorReceiverBlockEntity vendorReceiverBlockEntity)) {
			return true;
		}

		if (!vendorReceiverBlockEntity.canBreak(player)) {
			denyProtectedVendorBreak(level, player, pos, MESSAGE_RECEIVER_BREAK_DENIED);
			return false;
		}

		return true;
	}

	private static boolean handleVendorNoteBreak(Level level, Player player, BlockPos pos, BlockEntity blockEntity) {
		if (!(blockEntity instanceof VendorNoteBlockEntity vendorNoteBlockEntity)) {
			return true;
		}

		if (!vendorNoteBlockEntity.canBreak(player)) {
			denyProtectedVendorBreak(level, player, pos, MESSAGE_NOTE_BREAK_DENIED);
			return false;
		}

		if (!level.isClientSide()) {
			Block.popResource(level, pos, vendorNoteBlockEntity.createPreservedItemStack());
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
			return;
		}

		if (container instanceof VendorHoloDisplayBlockEntity vendorHoloDisplayBlockEntity) {
			handleVendorHoloDisplayContentsBreak(level, player, pos, vendorHoloDisplayBlockEntity);
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

	private static void handleVendorHoloDisplayContentsBreak(Level level, Player player, BlockPos pos, VendorHoloDisplayBlockEntity vendorHoloDisplayBlockEntity) {
		if (vendorHoloDisplayBlockEntity.isOwner(player.getUUID())) {
			dropContainerContents(level, pos, vendorHoloDisplayBlockEntity);
		}

		vendorHoloDisplayBlockEntity.clearContent();
	}

	private static void dropContainerContents(Level level, BlockPos pos, Container container) {
		Containers.dropContents(level, pos, container);
	}
}