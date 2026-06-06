package domestia_vendor_choice;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class VendorSafeMenu extends AbstractContainerMenu {
	// Base slot geometry.
	private static final int SIZE_SLOT = 18;

	// Safe inventory layout. Double-chest style.
	private static final int POS_SAFE_INVENTORY_X = 8;
	private static final int POS_SAFE_INVENTORY_Y = 18;
	private static final int LAYOUT_SAFE_COLUMNS = 9;
	private static final int LAYOUT_SAFE_ROWS = 6;

	// Player inventory layout.
	private static final int POS_PLAYER_INVENTORY_X = 8;
	private static final int POS_PLAYER_INVENTORY_Y = 139;
	private static final int POS_PLAYER_HOTBAR_Y = 197;

	// Vanilla player inventory dimensions.
	private static final int LAYOUT_PLAYER_INVENTORY_COLUMNS = 9;
	private static final int LAYOUT_PLAYER_INVENTORY_ROWS = 3;
	private static final int INDEX_PLAYER_HOTBAR_START = 0;
	private static final int INDEX_PLAYER_MAIN_INVENTORY_START = 9;

	// Menu slot index ranges.
	// Registration order is: Safe inventory -> Player main inventory -> Player hotbar.
	private static final int MENU_SAFE_START = 0;
	private static final int MENU_SAFE_END = MENU_SAFE_START + VendorSafeBlockEntity.SAFE_SLOT_COUNT;

	private static final int MENU_PLAYER_MAIN_START = MENU_SAFE_END;
	private static final int MENU_PLAYER_MAIN_END = MENU_PLAYER_MAIN_START + LAYOUT_PLAYER_INVENTORY_COLUMNS * LAYOUT_PLAYER_INVENTORY_ROWS;

	private static final int MENU_PLAYER_HOTBAR_START = MENU_PLAYER_MAIN_END;
	private static final int MENU_PLAYER_HOTBAR_END = MENU_PLAYER_HOTBAR_START + LAYOUT_PLAYER_INVENTORY_COLUMNS;

	private static final int MENU_PLAYER_INVENTORY_START = MENU_PLAYER_MAIN_START;
	private static final int MENU_PLAYER_INVENTORY_END = MENU_PLAYER_HOTBAR_END;

	private final @Nullable VendorSafeBlockEntity vendorSafeBlockEntity;

	public VendorSafeMenu(final int containerId, final Inventory playerInventory) {
		this(containerId, playerInventory, null);
	}

	public VendorSafeMenu(final int containerId, final Inventory playerInventory, final @Nullable VendorSafeBlockEntity vendorSafeBlockEntity) {
		super(ModMenus.VENDOR_SAFE, containerId);

		this.vendorSafeBlockEntity = vendorSafeBlockEntity;

		final @NotNull Container safeInventory;
		if (vendorSafeBlockEntity != null) {
			safeInventory = vendorSafeBlockEntity;
		} else {
			safeInventory = new SimpleContainer(VendorSafeBlockEntity.SAFE_SLOT_COUNT);
		}

		addSafeInventorySlots(safeInventory);
		addPlayerInventorySlots(playerInventory);
	}

	private void addSafeInventorySlots(final @NotNull Container safeInventory) {
		for (int row = 0; row < LAYOUT_SAFE_ROWS; row++) {
			for (int column = 0; column < LAYOUT_SAFE_COLUMNS; column++) {
				int slotIndex = column + row * LAYOUT_SAFE_COLUMNS;

				this.addSlot(new Slot(
						safeInventory,
						slotIndex,
						POS_SAFE_INVENTORY_X + column * SIZE_SLOT,
						POS_SAFE_INVENTORY_Y + row * SIZE_SLOT
				));
			}
		}
	}

	private void addPlayerInventorySlots(final Inventory playerInventory) {
		this.addPlayerMainInventorySlots(playerInventory);
		this.addPlayerHotbarSlots(playerInventory);
	}

	private void addPlayerMainInventorySlots(final Inventory playerInventory) {
		for (int row = 0; row < LAYOUT_PLAYER_INVENTORY_ROWS; row++) {
			for (int column = 0; column < LAYOUT_PLAYER_INVENTORY_COLUMNS; column++) {
				this.addSlot(new Slot(
						playerInventory,
						INDEX_PLAYER_MAIN_INVENTORY_START + column + row * LAYOUT_PLAYER_INVENTORY_COLUMNS,
						POS_PLAYER_INVENTORY_X + column * SIZE_SLOT,
						POS_PLAYER_INVENTORY_Y + row * SIZE_SLOT
				));
			}
		}
	}

	private void addPlayerHotbarSlots(final Inventory playerInventory) {
		for (int column = 0; column < LAYOUT_PLAYER_INVENTORY_COLUMNS; column++) {
			this.addSlot(new Slot(
					playerInventory,
					INDEX_PLAYER_HOTBAR_START + column,
					POS_PLAYER_INVENTORY_X + column * SIZE_SLOT,
					POS_PLAYER_HOTBAR_Y
			));
		}
	}

	@Override
	public void removed(final Player player) {
		super.removed(player);

		ModSounds.playSafeClose(player);
	}

	@Override
	public ItemStack quickMoveStack(final Player player, final int index) {
		final Slot sourceSlot = slots.get(index);

		if (!sourceSlot.hasItem()) {
			return ItemStack.EMPTY;
		}

		final ItemStack sourceStack = sourceSlot.getItem();
		final ItemStack originalStack = sourceStack.copy();

		if (index >= MENU_SAFE_START && index < MENU_SAFE_END) {
			if (!moveItemStackTo(sourceStack, MENU_PLAYER_INVENTORY_START, MENU_PLAYER_INVENTORY_END, true)) {
				return ItemStack.EMPTY;
			}
		} else if (index >= MENU_PLAYER_INVENTORY_START && index < MENU_PLAYER_INVENTORY_END) {
			if (!moveItemStackTo(sourceStack, MENU_SAFE_START, MENU_SAFE_END, false)) {
				return ItemStack.EMPTY;
			}
		} else {
			return ItemStack.EMPTY;
		}

		if (sourceStack.isEmpty()) {
			sourceSlot.set(ItemStack.EMPTY);
		} else {
			sourceSlot.setChanged();
		}

		if (sourceStack.getCount() == originalStack.getCount()) {
			return ItemStack.EMPTY;
		}

		sourceSlot.onTake(player, sourceStack);

		return originalStack;
	}

	@Override
	public boolean stillValid(final Player player) {
		return vendorSafeBlockEntity == null || vendorSafeBlockEntity.stillValid(player);
	}
}