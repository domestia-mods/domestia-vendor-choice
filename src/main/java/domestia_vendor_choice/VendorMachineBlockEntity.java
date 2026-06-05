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
import net.minecraft.server.level.ServerLevel;
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

public class VendorMachineBlockEntity extends BlockEntity implements Container, WorldlyContainer {
	// Persistent NBT keys.
	private static final String KEY_OWNER_UUID = "OwnerUuid";
	private static final String KEY_OWNER_NAME = "OwnerName";
	private static final String KEY_DISPLAY_NAME = "DisplayName";

	// Default back label. Used when the block item was not renamed on an anvil.
	public static final String DEFAULT_DISPLAY_NAME = "Sales";

	// Fallback text for legacy blocks that have OwnerUuid but no stored owner name.
	public static final String DEFAULT_OWNER_NAME = "Owner";

	// Vendor inventory layout.
	public static final int STOCK_SLOT_START = 0;
	public static final int STOCK_SLOT_COUNT = 5;

	public static final int PRICE_SLOT_START = STOCK_SLOT_START + STOCK_SLOT_COUNT;
	public static final int PRICE_SLOT_COUNT = 5;

	public static final int VAULT_SLOT_START = PRICE_SLOT_START + PRICE_SLOT_COUNT;
	public static final int VAULT_SLOT_COUNT = 15;

	public static final int INVENTORY_SIZE = STOCK_SLOT_COUNT + PRICE_SLOT_COUNT + VAULT_SLOT_COUNT;

	// Hopper IO slot exposure.
	private static final int[] HOPPER_STOCK_SLOTS = createSlotRange(STOCK_SLOT_START, STOCK_SLOT_COUNT);
	private static final int[] HOPPER_VAULT_SLOTS = createSlotRange(VAULT_SLOT_START, VAULT_SLOT_COUNT);
	private static final int[] HOPPER_NO_SLOTS = new int[0];

	// Interaction constants.
	private static final double STILL_VALID_MAX_DISTANCE_SQUARED = 64.0;
	private static final Permission OPERATOR_PERMISSION = Permissions.COMMANDS_GAMEMASTER;

	// Transaction pulse constants.
	private static final int TICKS_TRANSACTION_PULSE_DURATION = 4;
	private static final long TIME_NO_TRANSACTION_PULSE = 0L;

	private UUID ownerUuid;
	private String ownerName = DEFAULT_OWNER_NAME;
	private String displayName = DEFAULT_DISPLAY_NAME;

	private boolean transactionPowered = false;
	private long transactionPulseEndGameTime = TIME_NO_TRANSACTION_PULSE;

	// Runtime-only sales session state.
	// This is not saved to NBT. It exists only while at least one Sales GUI is open.
	private int openSalesMenuCount = 0;
	private final NonNullList<ItemStack> activeStockTemplates = NonNullList.withSize(STOCK_SLOT_COUNT, ItemStack.EMPTY);

	private final NonNullList<ItemStack> items = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);

	public VendorMachineBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.VENDOR_MACHINE, pos, state);
	}

	private static int[] createSlotRange(int firstSlot, int slotCount) {
		int[] slots = new int[slotCount];

		for (int index = 0; index < slotCount; index++) {
			slots[index] = firstSlot + index;
		}

		return slots;
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

		// Important for client-side renderer synchronization:
		// saved item lists usually omit empty slots. If we load over an old client-side
		// inventory without clearing it first, removed stock items remain visible
		// on the front panel.
		this.clearItemsWithoutUpdate();

		ContainerHelper.loadAllItems(input, this.items);

		// Runtime-only states must never survive chunk reloads.
		this.clearTransactionPulseRuntimeState();
		this.clearSalesSessionRuntimeState();
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
		// Vendor Machine must drop only the block item unless ModEvents explicitly drops contents for the owner.
		this.clearContent();
	}

	// Starts a short redstone transaction pulse after a successful checkout.
	// The signal is emitted from all utility faces: up, down, left, and right relative to block facing.
	public void startTransactionPulse() {
		if (!(this.level instanceof ServerLevel serverLevel)) {
			return;
		}

		long currentGameTime = serverLevel.getGameTime();

		this.transactionPulseEndGameTime = currentGameTime + TICKS_TRANSACTION_PULSE_DURATION;

		if (!this.transactionPowered) {
			this.transactionPowered = true;
			this.notifyTransactionOutputNeighbors();
		}

		this.scheduleTransactionPulseTick(serverLevel, TICKS_TRANSACTION_PULSE_DURATION);
	}

	public void handleTransactionPulseScheduledTick(ServerLevel serverLevel) {
		if (!this.transactionPowered) {
			return;
		}

		long remainingTicks = this.transactionPulseEndGameTime - serverLevel.getGameTime();

		if (remainingTicks > 0L) {
			this.scheduleTransactionPulseTick(serverLevel, getSafeTickDelay(remainingTicks));
			return;
		}

		this.clearTransactionPulseRuntimeState();
		this.notifyTransactionOutputNeighbors();
	}

	public boolean isTransactionPowered() {
		return this.transactionPowered;
	}

	private void clearTransactionPulseRuntimeState() {
		this.transactionPowered = false;
		this.transactionPulseEndGameTime = TIME_NO_TRANSACTION_PULSE;
	}

	private void scheduleTransactionPulseTick(ServerLevel serverLevel, int delayTicks) {
		serverLevel.scheduleTick(
				this.worldPosition,
				this.getBlockState().getBlock(),
				Math.max(1, delayTicks)
		);
	}

	private static int getSafeTickDelay(long remainingTicks) {
		if (remainingTicks > Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		}

		return Math.max(1, (int) remainingTicks);
	}

	private void notifyTransactionOutputNeighbors() {
		if (this.level == null) {
			return;
		}

		BlockState state = this.getBlockState();

		if (!(state.getBlock() instanceof VendorMachineBlock vendorMachineBlock)) {
			return;
		}

		// Notify the machine position and every output-side neighbor.
		// Front and back are deliberately excluded: they are interactive UI faces.
		this.level.updateNeighborsAt(this.worldPosition, vendorMachineBlock);

		for (Direction direction : Direction.values()) {
			if (!VendorMachineBlock.isTransactionOutputFace(state, direction)) {
				continue;
			}

			this.level.updateNeighborsAt(this.worldPosition.relative(direction), vendorMachineBlock);
		}
	}

	// Sales session memory:
	// while at least one Sales GUI is open, recently sold-out stock slots remember
	// their product and can be refilled by hoppers. When the last Sales GUI closes,
	// empty sold-out positions are forgotten.
	public void registerSalesMenuOpened() {
		if (this.openSalesMenuCount <= 0) {
			this.openSalesMenuCount = 0;
			this.clearActiveStockTemplates();
			this.captureActiveStockTemplatesFromCurrentStock();
		} else {
			this.captureActiveStockTemplatesFromCurrentStock();
		}

		this.openSalesMenuCount++;
	}

	public void registerSalesMenuClosed() {
		if (this.openSalesMenuCount <= 0) {
			this.openSalesMenuCount = 0;
			this.clearActiveStockTemplates();
			return;
		}

		this.openSalesMenuCount--;

		if (this.openSalesMenuCount <= 0) {
			this.openSalesMenuCount = 0;
			this.forgetActiveTemplatesForEmptyStockSlots();
		}
	}

	public void clearStockAfterSale(int productIndex) {
		if (productIndex < 0 || productIndex >= STOCK_SLOT_COUNT) {
			return;
		}

		int stockSlot = STOCK_SLOT_START + productIndex;

		this.items.set(stockSlot, ItemStack.EMPTY);
		this.setChanged();
	}

	private void clearSalesSessionRuntimeState() {
		this.openSalesMenuCount = 0;
		this.clearActiveStockTemplates();
	}

	private void captureActiveStockTemplatesFromCurrentStock() {
		for (int index = 0; index < STOCK_SLOT_COUNT; index++) {
			ItemStack stockStack = this.items.get(STOCK_SLOT_START + index);

			if (stockStack.isEmpty()) {
				continue;
			}

			this.activeStockTemplates.set(index, createStockTemplate(stockStack));
		}
	}

	private void forgetActiveTemplatesForEmptyStockSlots() {
		for (int index = 0; index < STOCK_SLOT_COUNT; index++) {
			ItemStack stockStack = this.items.get(STOCK_SLOT_START + index);

			if (stockStack.isEmpty()) {
				this.activeStockTemplates.set(index, ItemStack.EMPTY);
			}
		}
	}

	private void clearActiveStockTemplates() {
		for (int index = 0; index < this.activeStockTemplates.size(); index++) {
			this.activeStockTemplates.set(index, ItemStack.EMPTY);
		}
	}

	private void handleStockSlotDirectSet(int slot, ItemStack stack) {
		int stockIndex = slot - STOCK_SLOT_START;

		if (stockIndex < 0 || stockIndex >= STOCK_SLOT_COUNT) {
			return;
		}

		if (stack.isEmpty()) {
			// Manual/administrative clearing is a real product-position clear.
			// Checkout sold-out clearing must use clearStockAfterSale() to preserve session memory.
			this.activeStockTemplates.set(stockIndex, ItemStack.EMPTY);
			return;
		}

		if (this.openSalesMenuCount > 0) {
			this.activeStockTemplates.set(stockIndex, createStockTemplate(stack));
		}
	}

	private static ItemStack createStockTemplate(ItemStack sourceStack) {
		if (sourceStack.isEmpty()) {
			return ItemStack.EMPTY;
		}

		ItemStack templateStack = sourceStack.copy();
		templateStack.setCount(1);

		return templateStack;
	}

	@Override
	public int getContainerSize() {
		return INVENTORY_SIZE;
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

	public ItemStack getFrontDisplayStack(int displayIndex) {
		if (displayIndex < 0 || displayIndex >= STOCK_SLOT_COUNT) {
			return ItemStack.EMPTY;
		}

		return this.getItem(STOCK_SLOT_START + displayIndex);
	}

	@Override
	public ItemStack removeItem(int slot, int amount) {
		ItemStack removedStack = ContainerHelper.removeItem(this.items, slot, amount);

		if (!removedStack.isEmpty()) {
			if (this.isStockSlot(slot) && this.items.get(slot).isEmpty()) {
				this.handleStockSlotDirectSet(slot, ItemStack.EMPTY);
			}

			this.setChanged();
		}

		return removedStack;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		ItemStack removedStack = ContainerHelper.takeItem(this.items, slot);

		// Important:
		// despite the vanilla method name, this container must notify clients when a stack is removed.
		// Otherwise the front renderer keeps drawing the old stock item.
		if (!removedStack.isEmpty()) {
			if (this.isStockSlot(slot)) {
				this.handleStockSlotDirectSet(slot, ItemStack.EMPTY);
			}

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

		if (this.isStockSlot(slot)) {
			this.handleStockSlotDirectSet(slot, stack);
		}

		this.setChanged();
	}

	// Hopper IO:
	// - Top and viewer-right sides can refill existing Stock positions only.
	// - Bottom side can drain Vault only.
	// - Price slots are never exposed.
	// - Empty Stock slots reject hopper insertion unless the current Sales session remembers this product.
	@Override
	public int[] getSlotsForFace(Direction side) {
		if (this.isStockHopperInputSide(side)) {
			return HOPPER_STOCK_SLOTS;
		}

		if (this.isVaultHopperOutputSide(side)) {
			return HOPPER_VAULT_SLOTS;
		}

		return HOPPER_NO_SLOTS;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
		if (!this.isStockHopperInputSide(side)) {
			return false;
		}

		if (!this.isStockSlot(slot)) {
			return false;
		}

		return this.canHopperRefillStockSlot(slot, stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return this.isVaultHopperOutputSide(side) && this.isVaultSlot(slot);
	}

	private boolean canHopperRefillStockSlot(int slot, ItemStack insertedStack) {
		if (insertedStack.isEmpty()) {
			return false;
		}

		ItemStack stockStack = this.items.get(slot);

		if (!stockStack.isEmpty()) {
			return this.canHopperMergeIntoExistingStock(stockStack, insertedStack);
		}

		return this.canHopperRestoreSoldOutStock(slot, insertedStack);
	}

	private boolean canHopperMergeIntoExistingStock(ItemStack stockStack, ItemStack insertedStack) {
		if (!ItemStack.isSameItemSameComponents(stockStack, insertedStack)) {
			return false;
		}

		return stockStack.getCount() < Math.min(stockStack.getMaxStackSize(), this.getMaxStackSize());
	}

	private boolean canHopperRestoreSoldOutStock(int slot, ItemStack insertedStack) {
		int stockIndex = slot - STOCK_SLOT_START;

		if (stockIndex < 0 || stockIndex >= STOCK_SLOT_COUNT) {
			return false;
		}

		ItemStack templateStack = this.activeStockTemplates.get(stockIndex);

		if (templateStack.isEmpty()) {
			return false;
		}

		return ItemStack.isSameItemSameComponents(templateStack, insertedStack);
	}

	private boolean isStockHopperInputSide(Direction side) {
		if (side == Direction.UP) {
			return true;
		}

		return side == this.getViewerRightSide();
	}

	private boolean isVaultHopperOutputSide(Direction side) {
		return side == Direction.DOWN;
	}

	private Direction getViewerRightSide() {
		return this.getFrontSide().getCounterClockWise();
	}

	private Direction getFrontSide() {
		BlockState state = this.getBlockState();

		if (state.hasProperty(VendorMachineBlock.FACING)) {
			return state.getValue(VendorMachineBlock.FACING);
		}

		return Direction.NORTH;
	}

	private boolean isStockSlot(int slot) {
		return slot >= STOCK_SLOT_START && slot < STOCK_SLOT_START + STOCK_SLOT_COUNT;
	}

	private boolean isVaultSlot(int slot) {
		return slot >= VAULT_SLOT_START && slot < VAULT_SLOT_START + VAULT_SLOT_COUNT;
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
		this.clearSalesSessionRuntimeState();
		this.setChanged();
	}

	private void clearItemsWithoutUpdate() {
		for (int slot = 0; slot < this.items.size(); slot++) {
			this.items.set(slot, ItemStack.EMPTY);
		}
	}

	public Container getInventory() {
		return this;
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

	// Management means opening the rear Control interface.
	// Only the owner can configure the machine.
	public boolean canManage(Player player) {
		if (!this.isOwner(player.getUUID())) {
			return false;
		}

		this.updateOwnerNameFromPlayerIfNeeded(player);
		return true;
	}

	// Breaking is administrative recovery.
	// The owner can break the machine, and operators can destroy it if needed.
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