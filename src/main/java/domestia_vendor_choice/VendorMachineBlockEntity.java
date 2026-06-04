package domestia_vendor_choice;

import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.UUID;

public class VendorMachineBlockEntity extends BlockEntity implements Container {
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

	// Interaction constants.
	private static final double STILL_VALID_MAX_DISTANCE_SQUARED = 64.0;
	private static final Permission OPERATOR_PERMISSION = Permissions.COMMANDS_GAMEMASTER;

	private UUID ownerUuid;
	private String ownerName = DEFAULT_OWNER_NAME;
	private String displayName = DEFAULT_DISPLAY_NAME;

	private final NonNullList<ItemStack> items = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);

	public VendorMachineBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.VENDOR_MACHINE, pos, state);
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