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

public class VendorMachineBlockEntity extends BlockEntity implements Container, WorldlyContainer, VendorPublicDepositTarget {
	// Persistent NBT keys.
	private static final String KEY_OWNER_UUID = "OwnerUuid";
	private static final String KEY_OWNER_NAME = "OwnerName";
	private static final String KEY_DISPLAY_NAME = "DisplayName";

	// Translation key for the default visible label. Blank stored values use this client-side label.
	public static final String DEFAULT_DISPLAY_NAME_KEY = "display.domestia_vendor_choice.vendor_machine.default_label";

	// Translation key for unknown legacy owner names. Blank stored values use this client-side label.
	public static final String DEFAULT_OWNER_NAME_KEY = "display.domestia_vendor_choice.owner.fallback";

	// Empty stored defaults keep user-facing fallback text in lang files instead of NBT/code.
	public static final String DEFAULT_DISPLAY_NAME = "";
	public static final String DEFAULT_OWNER_NAME = "";

	// Vendor inventory layout.
	public static final int STOCK_SLOT_START = 0;
	public static final int STOCK_SLOT_COUNT = 5;

	public static final int PRICE_SLOT_START = STOCK_SLOT_START + STOCK_SLOT_COUNT;
	public static final int PRICE_SLOT_COUNT = 5;

	public static final int VAULT_SLOT_START = PRICE_SLOT_START + PRICE_SLOT_COUNT;
	public static final int VAULT_SLOT_COUNT = 15;

	public static final int INVENTORY_SIZE = STOCK_SLOT_COUNT + PRICE_SLOT_COUNT + VAULT_SLOT_COUNT;

	// Vanilla hopper exposure.
	// Security rule: vanilla hoppers must never access private Vendor Machine storage.
	private static final int[] VANILLA_HOPPER_NO_SLOTS = new int[0];

	// Interaction constants.
	private static final double STILL_VALID_MAX_DISTANCE_SQUARED = 64.0;

	// Transaction pulse constants.
	private static final int TICKS_TRANSACTION_PULSE_DURATION = 4;
	private static final long TIME_NO_TRANSACTION_PULSE = 0L;

	// Secure logistics constants.
	private static final int TICKS_SECURE_TRANSFER_INTERVAL = 8;
	private static final int COUNT_SECURE_TRANSFER_ITEMS_PER_CYCLE = 1;

	private UUID ownerUuid;
	private String ownerName = DEFAULT_OWNER_NAME;
	private String displayName = DEFAULT_DISPLAY_NAME;

	private boolean transactionPowered = false;
	private long transactionPulseEndGameTime = TIME_NO_TRANSACTION_PULSE;

	private int secureTransferCooldownTicks = 0;

	// Runtime-only sales session state.
	// This is not saved to NBT. It exists only while at least one Sales GUI is open.
	private int openSalesMenuCount = 0;
	private final NonNullList<ItemStack> activeStockTemplates = NonNullList.withSize(STOCK_SLOT_COUNT, ItemStack.EMPTY);

	private final NonNullList<ItemStack> items = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);

	public VendorMachineBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.VENDOR_MACHINE, pos, state);
	}

	public void serverTick() {
		if (this.level == null || this.level.isClientSide()) {
			return;
		}

		if (!this.hasOwner()) {
			return;
		}

		if (this.secureTransferCooldownTicks > 0) {
			this.secureTransferCooldownTicks--;
			return;
		}

		this.secureTransferCooldownTicks = TICKS_SECURE_TRANSFER_INTERVAL;

		if (this.transferOneSecureLogisticsCycle()) {
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

		this.clearTransactionPulseRuntimeState();
		this.clearSalesSessionRuntimeState();
		this.secureTransferCooldownTicks = 0;
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
		this.clearContent();
	}

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

		this.level.updateNeighborsAt(this.worldPosition, vendorMachineBlock);

		for (Direction direction : Direction.values()) {
			if (!VendorMachineBlock.isTransactionOutputFace(state, direction)) {
				continue;
			}

			this.level.updateNeighborsAt(this.worldPosition.relative(direction), vendorMachineBlock);
		}
	}

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

	private boolean transferOneSecureLogisticsCycle() {
		boolean importedStock = this.importOneStockItemFromOwnerSafeAbove();
		boolean exportedVault = this.exportOneVaultItemToOwnerTargetBelow();

		return importedStock || exportedVault;
	}

	private boolean importOneStockItemFromOwnerSafeAbove() {
		VendorSafeBlockEntity sourceSafe = this.getOwnerSafeAt(this.worldPosition.above());

		if (sourceSafe == null) {
			return false;
		}

		for (int stockIndex = 0; stockIndex < STOCK_SLOT_COUNT; stockIndex++) {
			int stockSlot = STOCK_SLOT_START + stockIndex;
			ItemStack stockStack = this.items.get(stockSlot);
			ItemStack templateStack = this.getStockInputTemplate(stockIndex, stockStack);

			if (templateStack.isEmpty()) {
				continue;
			}

			if (this.getStockMissingCount(stockStack, templateStack) <= 0) {
				continue;
			}

			ItemStack extractedStack = sourceSafe.extractMatchingForSecureTransfer(
					templateStack,
					COUNT_SECURE_TRANSFER_ITEMS_PER_CYCLE
			);

			if (extractedStack.isEmpty()) {
				continue;
			}

			this.insertIntoStockSlot(stockSlot, stockIndex, extractedStack);
			return true;
		}

		return false;
	}

	private ItemStack getStockInputTemplate(int stockIndex, ItemStack stockStack) {
		if (!stockStack.isEmpty()) {
			return createStockTemplate(stockStack);
		}

		if (stockIndex < 0 || stockIndex >= this.activeStockTemplates.size()) {
			return ItemStack.EMPTY;
		}

		return this.activeStockTemplates.get(stockIndex);
	}

	private int getStockMissingCount(ItemStack stockStack, ItemStack templateStack) {
		int maxStackSize = Math.min(templateStack.getMaxStackSize(), this.getMaxStackSize());

		if (stockStack.isEmpty()) {
			return maxStackSize;
		}

		if (!ItemStack.isSameItemSameComponents(stockStack, templateStack)) {
			return 0;
		}

		return Math.max(0, maxStackSize - stockStack.getCount());
	}

	private void insertIntoStockSlot(int stockSlot, int stockIndex, ItemStack insertedStack) {
		if (insertedStack.isEmpty()) {
			return;
		}

		ItemStack stockStack = this.items.get(stockSlot);

		if (stockStack.isEmpty()) {
			ItemStack newStockStack = insertedStack.copy();
			this.items.set(stockSlot, newStockStack);

			if (this.openSalesMenuCount > 0) {
				this.activeStockTemplates.set(stockIndex, createStockTemplate(newStockStack));
			}

			return;
		}

		stockStack.grow(insertedStack.getCount());
	}

	private boolean exportOneVaultItemToOwnerTargetBelow() {
		BlockPos targetPos = this.worldPosition.below();
		VendorSafeBlockEntity targetSafe = this.getOwnerSafeAt(targetPos);

		if (targetSafe != null) {
			return this.exportOneVaultItemToOwnerSafe(targetSafe);
		}

		VendorMachineBlockEntity targetMachine = this.getOwnerMachineAt(targetPos);

		if (targetMachine != null) {
			return this.exportOneVaultItemToOwnerMachineStock(targetMachine);
		}

		return false;
	}

	private boolean exportOneVaultItemToOwnerSafe(VendorSafeBlockEntity targetSafe) {
		for (int index = 0; index < VAULT_SLOT_COUNT; index++) {
			int vaultSlot = VAULT_SLOT_START + index;
			ItemStack vaultStack = this.items.get(vaultSlot);

			if (vaultStack.isEmpty()) {
				continue;
			}

			ItemStack transferStack = vaultStack.copy();
			transferStack.setCount(COUNT_SECURE_TRANSFER_ITEMS_PER_CYCLE);

			ItemStack remainingStack = targetSafe.insertForSecureTransfer(transferStack);

			if (!remainingStack.isEmpty()) {
				continue;
			}

			this.shrinkVaultSlot(vaultSlot, vaultStack, COUNT_SECURE_TRANSFER_ITEMS_PER_CYCLE);
			return true;
		}

		return false;
	}

	private boolean exportOneVaultItemToOwnerMachineStock(VendorMachineBlockEntity targetMachine) {
		for (int index = 0; index < VAULT_SLOT_COUNT; index++) {
			int vaultSlot = VAULT_SLOT_START + index;
			ItemStack vaultStack = this.items.get(vaultSlot);

			if (vaultStack.isEmpty()) {
				continue;
			}

			ItemStack transferStack = vaultStack.copy();
			transferStack.setCount(COUNT_SECURE_TRANSFER_ITEMS_PER_CYCLE);

			ItemStack remainingStack = targetMachine.insertStockForSecureTransfer(transferStack);

			if (!remainingStack.isEmpty()) {
				continue;
			}

			this.shrinkVaultSlot(vaultSlot, vaultStack, COUNT_SECURE_TRANSFER_ITEMS_PER_CYCLE);
			return true;
		}

		return false;
	}

	private void shrinkVaultSlot(int vaultSlot, ItemStack vaultStack, int count) {
		vaultStack.shrink(count);

		if (vaultStack.isEmpty()) {
			this.items.set(vaultSlot, ItemStack.EMPTY);
		}
	}

	private VendorSafeBlockEntity getOwnerSafeAt(BlockPos pos) {
		if (this.level == null || this.ownerUuid == null) {
			return null;
		}

		BlockEntity blockEntity = this.level.getBlockEntity(pos);

		if (!(blockEntity instanceof VendorSafeBlockEntity vendorSafeBlockEntity)) {
			return null;
		}

		if (!vendorSafeBlockEntity.isOwner(this.ownerUuid)) {
			return null;
		}

		return vendorSafeBlockEntity;
	}

	private VendorMachineBlockEntity getOwnerMachineAt(BlockPos pos) {
		if (this.level == null || this.ownerUuid == null) {
			return null;
		}

		BlockEntity blockEntity = this.level.getBlockEntity(pos);

		if (!(blockEntity instanceof VendorMachineBlockEntity vendorMachineBlockEntity)) {
			return null;
		}

		if (!vendorMachineBlockEntity.isOwner(this.ownerUuid)) {
			return null;
		}

		return vendorMachineBlockEntity;
	}


	// Secure input for owner-matched Vendor Hopper.
	// Vendor Hopper may restock only Stock slots. Price and Vault are never written by hopper logistics.
	public ItemStack insertStockForSecureTransfer(ItemStack sourceStack) {
		if (sourceStack.isEmpty()) {
			return ItemStack.EMPTY;
		}

		ItemStack remainingStack = sourceStack.copy();

		this.mergeSecureTransferIntoExistingStock(remainingStack);
		this.insertSecureTransferIntoActiveSoldOutStock(remainingStack);

		if (remainingStack.getCount() != sourceStack.getCount()) {
			this.setChanged();
		}

		return remainingStack;
	}

	private void mergeSecureTransferIntoExistingStock(ItemStack remainingStack) {
		for (int stockIndex = 0; stockIndex < STOCK_SLOT_COUNT; stockIndex++) {
			if (remainingStack.isEmpty()) {
				return;
			}

			int stockSlot = STOCK_SLOT_START + stockIndex;
			ItemStack stockStack = this.items.get(stockSlot);

			if (stockStack.isEmpty()) {
				continue;
			}

			if (!ItemStack.isSameItemSameComponents(stockStack, remainingStack)) {
				continue;
			}

			int freeSpace = Math.min(stockStack.getMaxStackSize(), this.getMaxStackSize()) - stockStack.getCount();

			if (freeSpace <= 0) {
				continue;
			}

			int movedCount = Math.min(freeSpace, remainingStack.getCount());

			stockStack.grow(movedCount);
			remainingStack.shrink(movedCount);
		}
	}

	private void insertSecureTransferIntoActiveSoldOutStock(ItemStack remainingStack) {
		if (this.openSalesMenuCount <= 0) {
			return;
		}

		for (int stockIndex = 0; stockIndex < STOCK_SLOT_COUNT; stockIndex++) {
			if (remainingStack.isEmpty()) {
				return;
			}

			int stockSlot = STOCK_SLOT_START + stockIndex;
			ItemStack stockStack = this.items.get(stockSlot);

			if (!stockStack.isEmpty()) {
				continue;
			}

			ItemStack templateStack = this.activeStockTemplates.get(stockIndex);

			if (templateStack.isEmpty()) {
				continue;
			}

			if (!ItemStack.isSameItemSameComponents(templateStack, remainingStack)) {
				continue;
			}

			ItemStack insertedStack = remainingStack.copy();
			insertedStack.setCount(Math.min(remainingStack.getMaxStackSize(), remainingStack.getCount()));

			this.items.set(stockSlot, insertedStack);
			remainingStack.shrink(insertedStack.getCount());
		}
	}

	@Override
	public ItemStack insertForPublicDeposit(ItemStack sourceStack, Player sender) {
		return this.insertStockForSecureTransfer(sourceStack);
	}

	// Secure output for owner-matched Vendor Hopper.
	// Vendor Hopper may extract only received payments from Vault. Stock and Price are never drained by hopper logistics.
	public ItemStack extractVaultForSecureTransfer(int maxCount) {
		if (maxCount <= 0) {
			return ItemStack.EMPTY;
		}

		for (int index = 0; index < VAULT_SLOT_COUNT; index++) {
			int vaultSlot = VAULT_SLOT_START + index;
			ItemStack vaultStack = this.items.get(vaultSlot);

			if (vaultStack.isEmpty()) {
				continue;
			}

			ItemStack extractedStack = vaultStack.copy();
			extractedStack.setCount(Math.min(maxCount, vaultStack.getCount()));

			vaultStack.shrink(extractedStack.getCount());

			if (vaultStack.isEmpty()) {
				this.items.set(vaultSlot, ItemStack.EMPTY);
			}

			this.setChanged();
			return extractedStack;
		}

		return ItemStack.EMPTY;
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

	private boolean isStockSlot(int slot) {
		return slot >= STOCK_SLOT_START && slot < STOCK_SLOT_START + STOCK_SLOT_COUNT;
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

		if (this.ownerName != null && !this.ownerName.isBlank()) {
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
		return VendorAccess.hasOwner(this.ownerUuid);
	}

	public boolean isOwner(UUID playerUuid) {
		return VendorAccess.isOwner(this.ownerUuid, playerUuid);
	}

	public boolean canManage(Player player) {
		if (!VendorAccess.canManage(this.ownerUuid, player)) {
			return false;
		}

		this.updateOwnerNameFromPlayerIfNeeded(player);
		return true;
	}

	public boolean canBreak(Player player) {
		if (VendorAccess.canManage(this.ownerUuid, player)) {
			this.updateOwnerNameFromPlayerIfNeeded(player);
			return true;
		}

		return VendorAccess.isAdministrator(player);
	}
}