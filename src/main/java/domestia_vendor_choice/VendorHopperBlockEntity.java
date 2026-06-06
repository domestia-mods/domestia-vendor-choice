package domestia_vendor_choice;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.UUID;

public class VendorHopperBlockEntity extends BlockEntity implements Container, WorldlyContainer {
	// Persistent NBT keys.
	private static final String KEY_OWNER_UUID = "OwnerUuid";
	private static final String KEY_OWNER_NAME = "OwnerName";
	private static final String KEY_DISPLAY_NAME = "DisplayName";

	// Default label. Used when the block item was not renamed on an anvil.
	public static final String DEFAULT_DISPLAY_NAME = "Hopper";

	// Fallback text for legacy blocks that have OwnerUuid but no stored owner name.
	public static final String DEFAULT_OWNER_NAME = "Owner";

	// Hopper inventory layout. Vanilla hopper capacity.
	public static final int HOPPER_SLOT_COUNT = 5;

	// Vanilla hopper exposure.
	// Security rule: vanilla hoppers must never siphon private Vendor Hopper storage.
	private static final int[] VANILLA_HOPPER_NO_SLOTS = new int[0];

	// Interaction constants.
	private static final double STILL_VALID_MAX_DISTANCE_SQUARED = 64.0;
	private static final Permission OPERATOR_PERMISSION = Permissions.COMMANDS_GAMEMASTER;

	// Transfer constants. Vanilla hopper-like cadence: one item per cycle.
	private static final int TICKS_TRANSFER_INTERVAL = 8;
	private static final int COUNT_TRANSFER_ITEMS_PER_CYCLE = 1;

	private UUID ownerUuid;
	private String ownerName = DEFAULT_OWNER_NAME;
	private String displayName = DEFAULT_DISPLAY_NAME;

	private int transferCooldownTicks = 0;

	private final NonNullList<ItemStack> items = NonNullList.withSize(HOPPER_SLOT_COUNT, ItemStack.EMPTY);

	public VendorHopperBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.VENDOR_HOPPER, pos, state);
	}

	public void serverTick() {
		if (this.level == null || this.level.isClientSide()) {
			return;
		}

		if (!this.hasOwner()) {
			return;
		}

		if (this.transferCooldownTicks > 0) {
			this.transferCooldownTicks--;
			return;
		}

		this.transferCooldownTicks = TICKS_TRANSFER_INTERVAL;

		if (this.transferOneCycle()) {
			this.setChanged();
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);

		this.saveOwner(output);
		this.saveDisplayName(output);
		ContainerHelper.saveAllItems(output, this.items);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);

		this.loadOwner(input);
		this.loadDisplayName(input);

		this.clearItemsWithoutUpdate();
		ContainerHelper.loadAllItems(input, this.items);

		this.transferCooldownTicks = 0;
	}

	private void saveOwner(ValueOutput output) {
		if (this.ownerUuid != null) {
			output.putString(KEY_OWNER_UUID, this.ownerUuid.toString());
		}

		output.putString(KEY_OWNER_NAME, this.ownerName);
	}

	private void loadOwner(ValueInput input) {
		String ownerUuidString = input.getStringOr(KEY_OWNER_UUID, "");

		if (ownerUuidString.isEmpty()) {
			this.ownerUuid = null;
		} else {
			try {
				this.ownerUuid = UUID.fromString(ownerUuidString);
			} catch (IllegalArgumentException exception) {
				this.ownerUuid = null;
			}
		}

		this.ownerName = normalizeStoredText(
				input.getStringOr(KEY_OWNER_NAME, DEFAULT_OWNER_NAME),
				DEFAULT_OWNER_NAME
		);
	}

	private void saveDisplayName(ValueOutput output) {
		output.putString(KEY_DISPLAY_NAME, this.displayName);
	}

	private void loadDisplayName(ValueInput input) {
		this.displayName = normalizeStoredText(
				input.getStringOr(KEY_DISPLAY_NAME, DEFAULT_DISPLAY_NAME),
				DEFAULT_DISPLAY_NAME
		);
	}

	private static String normalizeStoredText(String value, String fallback) {
		if (value == null || value.isBlank()) {
			return fallback;
		}

		return value;
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registryLookup) {
		return this.saveWithoutMetadata(registryLookup);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public void setChanged() {
		super.setChanged();

		if (this.level == null) {
			return;
		}

		BlockState state = this.getBlockState();

		this.level.sendBlockUpdated(
				this.worldPosition,
				state,
				state,
				Block.UPDATE_ALL
		);
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		// Privacy rule:
		// Do not call super here. The default BlockEntity behavior drops container contents.
		// Vendor Hopper must drop contents only through ModEvents owner-break logic.
		this.clearContent();
	}

	private boolean transferOneCycle() {
		// Push first to free internal space, then pull if nothing was pushed.
		// This mirrors the practical behavior players expect from a hopper chain.
		if (this.pushOneItemToOutput()) {
			return true;
		}

		return this.pullOneItemFromAbove();
	}

	private boolean pushOneItemToOutput() {
		Direction outputDirection = this.getOutputDirection();

		if (outputDirection == Direction.UP) {
			return false;
		}

		BlockPos targetPos = this.worldPosition.relative(outputDirection);
		BlockEntity targetBlockEntity = this.level.getBlockEntity(targetPos);

		if (!(targetBlockEntity instanceof Container targetContainer)) {
			return false;
		}

		Direction targetSide = outputDirection.getOpposite();

		for (int sourceSlot = 0; sourceSlot < HOPPER_SLOT_COUNT; sourceSlot++) {
			ItemStack sourceStack = this.items.get(sourceSlot);

			if (sourceStack.isEmpty()) {
				continue;
			}

			ItemStack transferStack = sourceStack.copy();
			transferStack.setCount(COUNT_TRANSFER_ITEMS_PER_CYCLE);

			ItemStack remainingStack = this.insertIntoTargetContainer(targetBlockEntity, targetContainer, transferStack, targetSide);

			if (!remainingStack.isEmpty()) {
				continue;
			}

			sourceStack.shrink(COUNT_TRANSFER_ITEMS_PER_CYCLE);

			if (sourceStack.isEmpty()) {
				this.items.set(sourceSlot, ItemStack.EMPTY);
			}

			return true;
		}

		return false;
	}

	private boolean pullOneItemFromAbove() {
		if (!this.hasAnyFreeInternalSpace()) {
			return false;
		}

		BlockPos sourcePos = this.worldPosition.above();
		BlockEntity sourceBlockEntity = this.level.getBlockEntity(sourcePos);

		if (!(sourceBlockEntity instanceof Container sourceContainer)) {
			return false;
		}

		ItemStack extractedStack = this.extractFromSourceContainer(
				sourceBlockEntity,
				sourceContainer,
				Direction.DOWN,
				COUNT_TRANSFER_ITEMS_PER_CYCLE
		);

		if (extractedStack.isEmpty()) {
			return false;
		}

		ItemStack remainingStack = this.insertIntoInternalInventory(extractedStack);

		if (!remainingStack.isEmpty()) {
			// This should not normally happen because insertion is pre-checked, but keep the transfer lossless.
			this.insertIntoTargetContainer(sourceBlockEntity, sourceContainer, remainingStack, Direction.DOWN);
			return false;
		}

		return true;
	}

	private ItemStack insertIntoTargetContainer(
			BlockEntity targetBlockEntity,
			Container targetContainer,
			ItemStack sourceStack,
			Direction targetSide
	) {
		if (sourceStack.isEmpty()) {
			return ItemStack.EMPTY;
		}

		if (!this.canAccessTargetContainer(targetBlockEntity)) {
			return sourceStack;
		}

		if (targetBlockEntity instanceof VendorMachineBlockEntity vendorMachineBlockEntity) {
			return vendorMachineBlockEntity.insertStockForSecureTransfer(sourceStack);
		}

		ItemStack remainingStack = sourceStack.copy();

		if (targetContainer instanceof WorldlyContainer worldlyContainer && !this.isVendorContainer(targetBlockEntity)) {
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

	private ItemStack extractFromSourceContainer(
			BlockEntity sourceBlockEntity,
			Container sourceContainer,
			Direction sourceSide,
			int maxCount
	) {
		if (!this.canAccessSourceContainer(sourceBlockEntity)) {
			return ItemStack.EMPTY;
		}

		if (sourceBlockEntity instanceof VendorMachineBlockEntity vendorMachineBlockEntity) {
			return vendorMachineBlockEntity.extractVaultForSecureTransfer(maxCount);
		}

		if (sourceContainer instanceof WorldlyContainer worldlyContainer && !this.isVendorContainer(sourceBlockEntity)) {
			int[] slots = worldlyContainer.getSlotsForFace(sourceSide);

			for (int slot : slots) {
				ItemStack extractedStack = this.extractFromContainerSlot(sourceContainer, worldlyContainer, slot, sourceSide, maxCount);

				if (!extractedStack.isEmpty()) {
					return extractedStack;
				}
			}
		} else {
			for (int slot = 0; slot < sourceContainer.getContainerSize(); slot++) {
				ItemStack extractedStack = this.extractFromContainerSlot(sourceContainer, null, slot, sourceSide, maxCount);

				if (!extractedStack.isEmpty()) {
					return extractedStack;
				}
			}
		}

		return ItemStack.EMPTY;
	}

	private ItemStack extractFromContainerSlot(
			Container container,
			WorldlyContainer worldlyContainer,
			int slot,
			Direction sourceSide,
			int maxCount
	) {
		ItemStack sourceStack = container.getItem(slot);

		if (sourceStack.isEmpty()) {
			return ItemStack.EMPTY;
		}

		if (worldlyContainer != null && !worldlyContainer.canTakeItemThroughFace(slot, sourceStack, sourceSide)) {
			return ItemStack.EMPTY;
		}

		ItemStack extractedStack = sourceStack.copy();
		extractedStack.setCount(Math.min(maxCount, sourceStack.getCount()));

		sourceStack.shrink(extractedStack.getCount());

		if (sourceStack.isEmpty()) {
			container.setItem(slot, ItemStack.EMPTY);
		}

		container.setChanged();

		return extractedStack;
	}

	private ItemStack insertIntoInternalInventory(ItemStack sourceStack) {
		ItemStack remainingStack = sourceStack.copy();

		for (int slot = 0; slot < HOPPER_SLOT_COUNT; slot++) {
			if (remainingStack.isEmpty()) {
				break;
			}

			remainingStack = this.insertIntoContainerSlot(this, slot, remainingStack);
		}

		if (remainingStack.getCount() != sourceStack.getCount()) {
			this.setChanged();
		}

		return remainingStack;
	}

	private boolean hasAnyFreeInternalSpace() {
		for (int slot = 0; slot < HOPPER_SLOT_COUNT; slot++) {
			ItemStack stack = this.items.get(slot);

			if (stack.isEmpty()) {
				return true;
			}

			if (stack.getCount() < Math.min(stack.getMaxStackSize(), this.getMaxStackSize())) {
				return true;
			}
		}

		return false;
	}

	private boolean canAccessSourceContainer(BlockEntity sourceBlockEntity) {
		return this.canAccessNeighborContainer(sourceBlockEntity);
	}

	private boolean canAccessTargetContainer(BlockEntity targetBlockEntity) {
		return this.canAccessNeighborContainer(targetBlockEntity);
	}

	private boolean canAccessNeighborContainer(BlockEntity blockEntity) {
		if (this.isVendorContainer(blockEntity)) {
			return this.isSameOwnerVendorContainer(blockEntity);
		}

		return true;
	}

	private boolean isVendorContainer(BlockEntity blockEntity) {
		return blockEntity instanceof VendorMachineBlockEntity
				|| blockEntity instanceof VendorSafeBlockEntity
				|| blockEntity instanceof VendorHopperBlockEntity;
	}

	private boolean isSameOwnerVendorContainer(BlockEntity blockEntity) {
		if (this.ownerUuid == null) {
			return false;
		}

		if (blockEntity instanceof VendorMachineBlockEntity vendorMachineBlockEntity) {
			return vendorMachineBlockEntity.isOwner(this.ownerUuid);
		}

		if (blockEntity instanceof VendorSafeBlockEntity vendorSafeBlockEntity) {
			return vendorSafeBlockEntity.isOwner(this.ownerUuid);
		}

		if (blockEntity instanceof VendorHopperBlockEntity vendorHopperBlockEntity) {
			return vendorHopperBlockEntity.isOwner(this.ownerUuid);
		}

		return false;
	}

	private Direction getOutputDirection() {
		BlockState state = this.getBlockState();

		if (state.hasProperty(VendorHopperBlock.FACING)) {
			return state.getValue(VendorHopperBlock.FACING);
		}

		return Direction.DOWN;
	}

	@Override
	public int getContainerSize() {
		return HOPPER_SLOT_COUNT;
	}

	@Override
	public boolean isEmpty() {
		for (ItemStack itemStack : this.items) {
			if (!itemStack.isEmpty()) {
				return false;
			}
		}

		return true;
	}

	@Override
	public ItemStack getItem(int slot) {
		return this.items.get(slot);
	}

	@Override
	public ItemStack removeItem(int slot, int amount) {
		ItemStack removedStack = ContainerHelper.removeItem(this.items, slot, amount);

		if (!removedStack.isEmpty()) {
			this.setChanged();
		}

		return removedStack;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		ItemStack removedStack = ContainerHelper.takeItem(this.items, slot);

		if (!removedStack.isEmpty()) {
			this.setChanged();
		}

		return removedStack;
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		this.items.set(slot, stack);

		if (stack.getCount() > this.getMaxStackSize()) {
			stack.setCount(this.getMaxStackSize());
		}

		this.setChanged();
	}

	// Vanilla hopper access is intentionally disabled.
	// Vendor Hopper performs its own owner-aware pull/push logic instead.
	@Override
	public int[] getSlotsForFace(Direction side) {
		return VANILLA_HOPPER_NO_SLOTS;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
		return false;
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return false;
	}

	@Override
	public boolean stillValid(Player player) {
		if (this.level == null) {
			return false;
		}

		if (this.level.getBlockEntity(this.worldPosition) != this) {
			return false;
		}

		return player.distanceToSqr(
				this.worldPosition.getX() + 0.5,
				this.worldPosition.getY() + 0.5,
				this.worldPosition.getZ() + 0.5
		) <= STILL_VALID_MAX_DISTANCE_SQUARED;
	}

	@Override
	public void clearContent() {
		this.clearItemsWithoutUpdate();
		this.setChanged();
	}

	private void clearItemsWithoutUpdate() {
		for (int slot = 0; slot < this.items.size(); slot++) {
			this.items.set(slot, ItemStack.EMPTY);
		}
	}

	public UUID getOwnerUuid() {
		return this.ownerUuid;
	}

	public String getOwnerName() {
		return this.ownerName;
	}

	public String getDisplayName() {
		return this.displayName;
	}

	public void setOwnerUuid(UUID ownerUuid) {
		this.ownerUuid = ownerUuid;
		this.setChanged();
	}

	public void setOwner(Player player) {
		this.ownerUuid = player.getUUID();
		this.ownerName = getPlayerName(player);
		this.setChanged();
	}

	public void setOwnerAndDisplayName(Player player, ItemStack placedStack) {
		this.ownerUuid = player.getUUID();
		this.ownerName = getPlayerName(player);
		this.displayName = extractCustomDisplayName(placedStack);
		this.setChanged();
	}

	private void updateOwnerNameFromPlayerIfNeeded(Player player) {
		if (!this.isOwner(player.getUUID())) {
			return;
		}

		if (!DEFAULT_OWNER_NAME.equals(this.ownerName)) {
			return;
		}

		this.ownerName = getPlayerName(player);
		this.setChanged();
	}

	private static String getPlayerName(Player player) {
		return normalizeStoredText(player.getName().getString(), DEFAULT_OWNER_NAME);
	}

	private static String extractCustomDisplayName(ItemStack stack) {
		Component customName = stack.get(DataComponents.CUSTOM_NAME);

		if (customName == null) {
			return DEFAULT_DISPLAY_NAME;
		}

		return normalizeStoredText(customName.getString(), DEFAULT_DISPLAY_NAME);
	}

	public boolean hasOwner() {
		return this.ownerUuid != null;
	}

	public boolean isOwner(UUID playerUuid) {
		return this.ownerUuid != null && this.ownerUuid.equals(playerUuid);
	}

	// Management means opening the private hopper container.
	// Only the owner can access the contents.
	public boolean canManage(Player player) {
		if (!this.isOwner(player.getUUID())) {
			return false;
		}

		this.updateOwnerNameFromPlayerIfNeeded(player);
		return true;
	}

	// Breaking is administrative recovery.
	// The owner can break the hopper, and operators can destroy it if needed.
	public boolean canBreak(Player player) {
		if (this.isOwner(player.getUUID())) {
			this.updateOwnerNameFromPlayerIfNeeded(player);
			return true;
		}

		return this.isOperator(player);
	}

	private boolean isOperator(Player player) {
		return player.permissions().hasPermission(OPERATOR_PERMISSION);
	}
}
