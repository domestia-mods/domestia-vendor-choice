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
	// Vendor Hopper control layout.
	private static final int POS_FILTER_SLOTS_X = 62;
	private static final int POS_FILTER_SLOTS_Y = 30;
	private static final int POS_BUFFER_SLOTS_Y = 58;

	// Player inventory layout.
	private static final int POS_PLAYER_INVENTORY_X = 8;
	private static final int POS_PLAYER_INVENTORY_Y = 85;
	private static final int POS_PLAYER_HOTBAR_Y = 143;

	// Menu slot index ranges.
	// Registration order is: Template slots -> Buffer slots -> Player main inventory -> Player hotbar.
	private static final int MENU_TEMPLATE_START = 0;
	private static final int MENU_TEMPLATE_END = MENU_TEMPLATE_START + VendorHopperBlockEntity.TEMPLATE_SLOT_COUNT;

	private static final int MENU_BUFFER_START = MENU_TEMPLATE_END;
	private static final int MENU_BUFFER_END = MENU_BUFFER_START + VendorHopperBlockEntity.BUFFER_SLOT_COUNT;

	private static final int MENU_PLAYER_MAIN_START = MENU_BUFFER_END;
	private static final int MENU_PLAYER_MAIN_END = MENU_PLAYER_MAIN_START + VendorMenuSlots.PLAYER_MAIN_SLOT_COUNT;

	private static final int MENU_PLAYER_HOTBAR_START = MENU_PLAYER_MAIN_END;
	private static final int MENU_PLAYER_HOTBAR_END = MENU_PLAYER_HOTBAR_START + VendorMenuSlots.PLAYER_HOTBAR_SLOT_COUNT;

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

		this.addVendorHopperTemplateSlots(hopperInventory);
		this.addVendorHopperBufferSlots(hopperInventory);
		this.addPlayerInventorySlots(playerInventory);
	}

	private void addVendorHopperTemplateSlots(final @NotNull Container hopperInventory) {
		for (int slot = 0; slot < VendorHopperBlockEntity.TEMPLATE_SLOT_COUNT; slot++) {
			this.addSlot(new TemplateSlot(
					hopperInventory,
					VendorHopperBlockEntity.TEMPLATE_SLOT_START + slot,
					POS_FILTER_SLOTS_X + slot * VendorMenuSlots.SLOT_SIZE,
					POS_FILTER_SLOTS_Y
			));
		}
	}

	private void addVendorHopperBufferSlots(final @NotNull Container hopperInventory) {
		for (int slot = 0; slot < VendorHopperBlockEntity.BUFFER_SLOT_COUNT; slot++) {
			this.addSlot(new Slot(
					hopperInventory,
					VendorHopperBlockEntity.BUFFER_SLOT_START + slot,
					POS_FILTER_SLOTS_X + slot * VendorMenuSlots.SLOT_SIZE,
					POS_BUFFER_SLOTS_Y
			));
		}
	}

	private void addPlayerInventorySlots(final Inventory playerInventory) {
		VendorMenuSlots.addPlayerInventorySlots(
				this::addSlot,
				playerInventory,
				POS_PLAYER_INVENTORY_X,
				POS_PLAYER_INVENTORY_Y,
				POS_PLAYER_HOTBAR_Y
		);
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

		if (index >= MENU_TEMPLATE_START && index < MENU_BUFFER_END) {
			if (!moveItemStackTo(sourceStack, MENU_PLAYER_INVENTORY_START, MENU_PLAYER_INVENTORY_END, true)) {
				return ItemStack.EMPTY;
			}
		} else if (index >= MENU_PLAYER_INVENTORY_START && index < MENU_PLAYER_INVENTORY_END) {
			// Shift-click from player inventory feeds only Buffer slots.
			// Template slots are owner-managed manually to avoid accidental filter changes.
			if (!moveItemStackTo(sourceStack, MENU_BUFFER_START, MENU_BUFFER_END, false)) {
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

	private static class TemplateSlot extends Slot {
		public TemplateSlot(Container container, int slot, int x, int y) {
			super(container, slot, x, y);
		}

		@Override
		public int getMaxStackSize() {
			return 1;
		}

		@Override
		public int getMaxStackSize(ItemStack stack) {
			return 1;
		}
	}
}
