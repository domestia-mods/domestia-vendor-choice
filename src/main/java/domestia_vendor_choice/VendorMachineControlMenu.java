package domestia_vendor_choice;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class VendorMachineControlMenu extends AbstractContainerMenu {
	// Base slot geometry.
	private static final int SIZE_SLOT = 18;

	// Vendor machine stock column.
	private static final int POS_STOCK_SLOT_X = 8;
	private static final int POS_STOCK_PRICE_SLOT_Y = 36;

	// Vendor machine price column.
	private static final int POS_PRICE_SLOT_X = 44;

	// Vendor machine vault grid.
	private static final int POS_VAULT_SLOT_X = 116;
	private static final int POS_VAULT_SLOT_Y = 36;
	private static final int LAYOUT_VAULT_COLUMNS = 3;

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
	// Registration order is: Vault -> Stock -> Price -> Player main inventory -> Player hotbar.
	private static final int MENU_VAULT_START = 0;
	private static final int MENU_VAULT_END = MENU_VAULT_START + VendorMachineBlockEntity.VAULT_SLOT_COUNT;

	private static final int MENU_STOCK_START = MENU_VAULT_END;
	private static final int MENU_STOCK_END = MENU_STOCK_START + VendorMachineBlockEntity.STOCK_SLOT_COUNT;

	private static final int MENU_PRICE_START = MENU_STOCK_END;
	private static final int MENU_PRICE_END = MENU_PRICE_START + VendorMachineBlockEntity.PRICE_SLOT_COUNT;

	private static final int MENU_MACHINE_START = MENU_VAULT_START;
	private static final int MENU_MACHINE_END = MENU_PRICE_END;

	private static final int MENU_PLAYER_MAIN_START = MENU_MACHINE_END;
	private static final int MENU_PLAYER_MAIN_END = MENU_PLAYER_MAIN_START + LAYOUT_PLAYER_INVENTORY_COLUMNS * LAYOUT_PLAYER_INVENTORY_ROWS;

	private static final int MENU_PLAYER_HOTBAR_START = MENU_PLAYER_MAIN_END;
	private static final int MENU_PLAYER_HOTBAR_END = MENU_PLAYER_HOTBAR_START + LAYOUT_PLAYER_INVENTORY_COLUMNS;

	private static final int MENU_PLAYER_INVENTORY_START = MENU_PLAYER_MAIN_START;
	private static final int MENU_PLAYER_INVENTORY_END = MENU_PLAYER_HOTBAR_END;

	private final VendorMachineBlockEntity vendorMachineBlockEntity;

	public VendorMachineControlMenu(int containerId, Inventory playerInventory) {
		this(containerId, playerInventory, null);
	}

	public VendorMachineControlMenu(int containerId, Inventory playerInventory, VendorMachineBlockEntity vendorMachineBlockEntity) {
		super(ModMenus.VENDOR_MACHINE_CONTROL, containerId);

		this.vendorMachineBlockEntity = vendorMachineBlockEntity;

		Container vendorInventory = vendorMachineBlockEntity != null
				? vendorMachineBlockEntity.getInventory()
				: new SimpleContainer(VendorMachineBlockEntity.INVENTORY_SIZE);

		this.addVendorMachineSlots(vendorInventory);
		this.addPlayerInventorySlots(playerInventory);
	}

	private void addVendorMachineSlots(Container vendorInventory) {
		this.addVaultSlots(vendorInventory);
		this.addStockSlots(vendorInventory);
		this.addPriceSlots(vendorInventory);
	}

	private void addStockSlots(Container vendorInventory) {
		for (int row = 0; row < VendorMachineBlockEntity.STOCK_SLOT_COUNT; row++) {
			this.addSlot(new Slot(
					vendorInventory,
					VendorMachineBlockEntity.STOCK_SLOT_START + row,
					POS_STOCK_SLOT_X,
					POS_STOCK_PRICE_SLOT_Y + row * SIZE_SLOT
			));
		}
	}

	private void addPriceSlots(Container vendorInventory) {
		for (int row = 0; row < VendorMachineBlockEntity.PRICE_SLOT_COUNT; row++) {
			this.addSlot(new Slot(
					vendorInventory,
					VendorMachineBlockEntity.PRICE_SLOT_START + row,
					POS_PRICE_SLOT_X,
					POS_STOCK_PRICE_SLOT_Y + row * SIZE_SLOT
			));
		}
	}

	private void addVaultSlots(Container vendorInventory) {
		for (int vaultSlotIndex = 0; vaultSlotIndex < VendorMachineBlockEntity.VAULT_SLOT_COUNT; vaultSlotIndex++) {
			int column = vaultSlotIndex % LAYOUT_VAULT_COLUMNS;
			int row = vaultSlotIndex / LAYOUT_VAULT_COLUMNS;

			this.addSlot(new Slot(
					vendorInventory,
					VendorMachineBlockEntity.VAULT_SLOT_START + vaultSlotIndex,
					POS_VAULT_SLOT_X + column * SIZE_SLOT,
					POS_VAULT_SLOT_Y + row * SIZE_SLOT
			));
		}
	}

	private void addPlayerInventorySlots(Inventory playerInventory) {
		this.addPlayerMainInventorySlots(playerInventory);
		this.addPlayerHotbarSlots(playerInventory);
	}

	private void addPlayerMainInventorySlots(Inventory playerInventory) {
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

	private void addPlayerHotbarSlots(Inventory playerInventory) {
		for (int column = 0; column < LAYOUT_PLAYER_INVENTORY_COLUMNS; column++) {
			this.addSlot(new Slot(
					playerInventory,
					INDEX_PLAYER_HOTBAR_START + column,
					POS_PLAYER_INVENTORY_X + column * SIZE_SLOT,
					POS_PLAYER_HOTBAR_Y
			));
		}
	}

	public VendorMachineBlockEntity getVendorMachineBlockEntity() {
		return this.vendorMachineBlockEntity;
	}

	@Override
	public void removed(Player player) {
		super.removed(player);

		ModSounds.playMachineControlClose(player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot sourceSlot = this.slots.get(index);

		if (!sourceSlot.hasItem()) {
			return ItemStack.EMPTY;
		}

		ItemStack sourceStack = sourceSlot.getItem();
		ItemStack originalStack = sourceStack.copy();

		if (index >= MENU_MACHINE_START && index < MENU_MACHINE_END) {
			if (!this.moveItemStackTo(sourceStack, MENU_PLAYER_INVENTORY_START, MENU_PLAYER_INVENTORY_END, true)) {
				return ItemStack.EMPTY;
			}
		} else if (index >= MENU_PLAYER_INVENTORY_START && index < MENU_PLAYER_INVENTORY_END) {
			if (!this.moveItemStackTo(sourceStack, MENU_VAULT_START, MENU_VAULT_END, false)) {
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
	public boolean stillValid(Player player) {
		if (this.vendorMachineBlockEntity == null) {
			return true;
		}

		return this.vendorMachineBlockEntity.stillValid(player);
	}
}