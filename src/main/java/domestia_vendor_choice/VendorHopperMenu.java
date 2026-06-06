package domestia_vendor_choice;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VendorHopperMenu extends AbstractContainerMenu {
	// Base slot geometry.
	private static final int SIZE_SLOT = 18;

	// Vendor Hopper inventory layout.
	private static final int POS_HOPPER_INVENTORY_X = 44;
	private static final int POS_HOPPER_INVENTORY_Y = 20;

	// Player inventory layout.
	private static final int POS_PLAYER_INVENTORY_X = 8;
	private static final int POS_PLAYER_INVENTORY_Y = 51;
	private static final int POS_PLAYER_HOTBAR_Y = 109;

	// Vanilla player inventory dimensions.
	private static final int LAYOUT_PLAYER_INVENTORY_COLUMNS = 9;
	private static final int LAYOUT_PLAYER_INVENTORY_ROWS = 3;
	private static final int INDEX_PLAYER_HOTBAR_START = 0;
	private static final int INDEX_PLAYER_MAIN_INVENTORY_START = 9;

	// Menu slot index ranges.
	// Registration order is: Vendor Hopper inventory -> Player main inventory -> Player hotbar.
	private static final int MENU_HOPPER_START = 0;
	private static final int MENU_HOPPER_END = MENU_HOPPER_START + VendorHopperBlockEntity.HOPPER_SLOT_COUNT;

	private static final int MENU_PLAYER_MAIN_START = MENU_HOPPER_END;
	private static final int MENU_PLAYER_MAIN_END = MENU_PLAYER_MAIN_START + LAYOUT_PLAYER_INVENTORY_COLUMNS * LAYOUT_PLAYER_INVENTORY_ROWS;

	private static final int MENU_PLAYER_HOTBAR_START = MENU_PLAYER_MAIN_END;
	private static final int MENU_PLAYER_HOTBAR_END = MENU_PLAYER_HOTBAR_START + LAYOUT_PLAYER_INVENTORY_COLUMNS;

	private static final int MENU_PLAYER_INVENTORY_START = MENU_PLAYER_MAIN_START;
	private static final int MENU_PLAYER_INVENTORY_END = MENU_PLAYER_HOTBAR_END;

	private final @Nullable VendorHopperBlockEntity vendorHopperBlockEntity;

	public VendorHopperMenu(final int containerId, final Inventory playerInventory) {
		this(containerId, playerInventory, null);
	}

	public VendorHopperMenu(final int containerId, final Inventory playerInventory, final @Nullable VendorHopperBlockEntity vendorHopperBlockEntity) {
		super(ModMenus.VENDOR_HOPPER, containerId);

		this.vendorHopperBlockEntity = vendorHopperBlockEntity;

		final @NotNull Container hopperInventory;
		if (vendorHopperBlockEntity != null) {
			hopperInventory = vendorHopperBlockEntity;
		} else {
			hopperInventory = new SimpleContainer(VendorHopperBlockEntity.HOPPER_SLOT_COUNT);
		}

		this.addVendorHopperSlots(hopperInventory);
		this.addPlayerInventorySlots(playerInventory);
	}

	private void addVendorHopperSlots(final @NotNull Container hopperInventory) {
		for (int slot = 0; slot < VendorHopperBlockEntity.HOPPER_SLOT_COUNT; slot++) {
			this.addSlot(new Slot(
					hopperInventory,
					slot,
					POS_HOPPER_INVENTORY_X + slot * SIZE_SLOT,
					POS_HOPPER_INVENTORY_Y
			));
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
		final Slot sourceSlot = this.slots.get(index);

		if (!sourceSlot.hasItem()) {
			return ItemStack.EMPTY;
		}

		final ItemStack sourceStack = sourceSlot.getItem();
		final ItemStack originalStack = sourceStack.copy();

		if (index >= MENU_HOPPER_START && index < MENU_HOPPER_END) {
			if (!this.moveItemStackTo(sourceStack, MENU_PLAYER_INVENTORY_START, MENU_PLAYER_INVENTORY_END, true)) {
				return ItemStack.EMPTY;
			}
		} else if (index >= MENU_PLAYER_INVENTORY_START && index < MENU_PLAYER_INVENTORY_END) {
			if (!this.moveItemStackTo(sourceStack, MENU_HOPPER_START, MENU_HOPPER_END, false)) {
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
		return this.vendorHopperBlockEntity == null || this.vendorHopperBlockEntity.stillValid(player);
	}
}
