package domestia_vendor_choice;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class VendorReceiverBlock extends Block implements EntityBlock {
	public static final MapCodec<VendorReceiverBlock> CODEC = simpleCodec(VendorReceiverBlock::new);

	private static final String ID_MESSAGE_DEPOSIT_ACCEPTED = "message.domestia_vendor_choice.vendor_receiver.deposit_accepted";
	private static final String ID_MESSAGE_DEPOSIT_REJECTED = "message.domestia_vendor_choice.vendor_receiver.deposit_rejected";
	private static final String ID_MESSAGE_DEPOSIT_COOLDOWN = "message.domestia_vendor_choice.vendor_receiver.deposit_cooldown";

	private static final Component MESSAGE_DEPOSIT_ACCEPTED = Component.translatable(ID_MESSAGE_DEPOSIT_ACCEPTED);
	private static final Component MESSAGE_DEPOSIT_REJECTED = Component.translatable(ID_MESSAGE_DEPOSIT_REJECTED);
	private static final Component MESSAGE_DEPOSIT_COOLDOWN = Component.translatable(ID_MESSAGE_DEPOSIT_COOLDOWN);
	private static final int COUNT_DEPOSIT_ITEMS_PER_CLICK = 1;

	public VendorReceiverBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<? extends Block> codec() {
		return CODEC;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new VendorReceiverBlockEntity(pos, state);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);

		if (level.isClientSide()) {
			return;
		}

		if (!(placer instanceof Player player)) {
			return;
		}

		BlockEntity blockEntity = level.getBlockEntity(pos);

		if (blockEntity instanceof VendorReceiverBlockEntity vendorReceiverBlockEntity) {
			vendorReceiverBlockEntity.setOwnerAndDisplayName(player, stack);
		}
	}

	@Override
	protected InteractionResult useItemOn(
			ItemStack stack,
			BlockState state,
			Level level,
			BlockPos pos,
			Player player,
			InteractionHand hand,
			BlockHitResult hitResult
	) {
		return this.handleVendorReceiverInteraction(level, pos, player, hand);
	}

	@Override
	protected InteractionResult useWithoutItem(
			BlockState state,
			Level level,
			BlockPos pos,
			Player player,
			BlockHitResult hitResult
	) {
		return InteractionResult.SUCCESS;
	}

	private InteractionResult handleVendorReceiverInteraction(Level level, BlockPos pos, Player player, InteractionHand hand) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		BlockEntity blockEntity = level.getBlockEntity(pos);

		if (!(blockEntity instanceof VendorReceiverBlockEntity vendorReceiverBlockEntity)) {
			return InteractionResult.SUCCESS;
		}

		ItemStack handStack = player.getItemInHand(hand);

		if (handStack.isEmpty()) {
			return InteractionResult.SUCCESS;
		}

		long gameTime = level.getGameTime();

		if (!vendorReceiverBlockEntity.canUseNow(player, gameTime)) {
			player.sendSystemMessage(MESSAGE_DEPOSIT_COOLDOWN);
			return InteractionResult.SUCCESS;
		}

		vendorReceiverBlockEntity.markUsed(player, gameTime);

		if (!vendorReceiverBlockEntity.hasOwner()) {
			this.rejectDeposit(player);
			return InteractionResult.SUCCESS;
		}

		ItemStack transferStack = handStack.copy();
		transferStack.setCount(COUNT_DEPOSIT_ITEMS_PER_CLICK);

		if (!this.tryDepositIntoBlockBelow(level, pos, player, transferStack)) {
			this.rejectDeposit(player);
			return InteractionResult.SUCCESS;
		}

		handStack.shrink(COUNT_DEPOSIT_ITEMS_PER_CLICK);
		player.sendSystemMessage(MESSAGE_DEPOSIT_ACCEPTED);
		ModSounds.playMachineCheckoutSuccess(player);

		return InteractionResult.SUCCESS;
	}

	private boolean tryDepositIntoBlockBelow(Level level, BlockPos receiverPos, Player player, ItemStack transferStack) {
		BlockEntity targetBlockEntity = level.getBlockEntity(receiverPos.below());

		if (targetBlockEntity instanceof VendorPublicDepositTarget publicDepositTarget) {
			ItemStack remainingStack = publicDepositTarget.insertForPublicDeposit(transferStack, player);
			return remainingStack.isEmpty();
		}

		if (this.isProtectedVendorTarget(targetBlockEntity)) {
			return false;
		}

		if (!(targetBlockEntity instanceof Container targetContainer)) {
			return false;
		}

		ItemStack remainingStack = this.insertIntoVanillaTarget(targetContainer, transferStack, Direction.UP);
		return remainingStack.isEmpty();
	}

	private boolean isProtectedVendorTarget(BlockEntity targetBlockEntity) {
		return targetBlockEntity instanceof VendorMachineBlockEntity
				|| targetBlockEntity instanceof VendorSafeBlockEntity
				|| targetBlockEntity instanceof VendorHopperBlockEntity
				|| targetBlockEntity instanceof VendorHoloDisplayBlockEntity
				|| targetBlockEntity instanceof VendorNoteBlockEntity
				|| targetBlockEntity instanceof VendorReceiverBlockEntity;
	}

	private ItemStack insertIntoVanillaTarget(Container targetContainer, ItemStack sourceStack, Direction targetSide) {
		ItemStack remainingStack = sourceStack.copy();

		if (targetContainer instanceof WorldlyContainer worldlyContainer) {
			int[] slots = worldlyContainer.getSlotsForFace(targetSide);

			for (int slot : slots) {
				if (remainingStack.isEmpty()) {
					break;
				}

				if (!worldlyContainer.canPlaceItemThroughFace(slot, remainingStack, targetSide)) {
					continue;
				}

				remainingStack = this.insertIntoContainerSlot(targetContainer, slot, remainingStack);
			}
		} else {
			for (int slot = 0; slot < targetContainer.getContainerSize(); slot++) {
				if (remainingStack.isEmpty()) {
					break;
				}

				if (!targetContainer.canPlaceItem(slot, remainingStack)) {
					continue;
				}

				remainingStack = this.insertIntoContainerSlot(targetContainer, slot, remainingStack);
			}
		}

		if (remainingStack.getCount() != sourceStack.getCount()) {
			targetContainer.setChanged();
		}

		return remainingStack;
	}

	private ItemStack insertIntoContainerSlot(Container container, int slot, ItemStack remainingStack) {
		ItemStack targetStack = container.getItem(slot);

		if (targetStack.isEmpty()) {
			ItemStack insertedStack = remainingStack.copy();
			insertedStack.setCount(Math.min(remainingStack.getMaxStackSize(), remainingStack.getCount()));

			container.setItem(slot, insertedStack);
			remainingStack.shrink(insertedStack.getCount());

			return remainingStack;
		}

		if (!ItemStack.isSameItemSameComponents(targetStack, remainingStack)) {
			return remainingStack;
		}

		int freeSpace = Math.min(targetStack.getMaxStackSize(), container.getMaxStackSize()) - targetStack.getCount();

		if (freeSpace <= 0) {
			return remainingStack;
		}

		int insertedCount = Math.min(freeSpace, remainingStack.getCount());

		targetStack.grow(insertedCount);
		remainingStack.shrink(insertedCount);

		return remainingStack;
	}

	private void rejectDeposit(Player player) {
		player.sendSystemMessage(MESSAGE_DEPOSIT_REJECTED);
		ModSounds.playMachineError(player);
	}
}
