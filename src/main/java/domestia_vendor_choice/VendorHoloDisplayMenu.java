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

public class VendorHoloDisplayMenu extends AbstractContainerMenu {
	private static final int POS_DISPLAY_SLOT_X = 20;
	private static final int POS_DISPLAY_SLOT_Y = 38;
	private static final int POS_DYE_SLOTS_X = 130;
	private static final int POS_FIRST_DYE_SLOT_Y = 60;
	private static final int POS_DYE_ROW_STEP = 18;

	private static final int POS_PLAYER_INVENTORY_X = 20;
	private static final int POS_PLAYER_INVENTORY_Y = 163;
	private static final int POS_PLAYER_HOTBAR_Y = 221;

	private static final int MENU_HOLO_DISPLAY_START = 0;
	private static final int MENU_HOLO_DISPLAY_END = MENU_HOLO_DISPLAY_START + VendorHoloDisplayBlockEntity.HOLO_DISPLAY_SLOT_COUNT;

	private static final int MENU_PLAYER_MAIN_START = MENU_HOLO_DISPLAY_END;
	private static final int MENU_PLAYER_MAIN_END = MENU_PLAYER_MAIN_START + VendorMenuSlots.PLAYER_MAIN_SLOT_COUNT;

	private static final int MENU_PLAYER_HOTBAR_START = MENU_PLAYER_MAIN_END;
	private static final int MENU_PLAYER_HOTBAR_END = MENU_PLAYER_HOTBAR_START + VendorMenuSlots.PLAYER_HOTBAR_SLOT_COUNT;

	private static final int MENU_PLAYER_INVENTORY_START = MENU_PLAYER_MAIN_START;
	private static final int MENU_PLAYER_INVENTORY_END = MENU_PLAYER_HOTBAR_END;

	private final @Nullable VendorHoloDisplayBlockEntity vendorHoloDisplayBlockEntity;

	public VendorHoloDisplayMenu(final int containerId, final Inventory playerInventory) {
		this(containerId, playerInventory, null);
	}

	public VendorHoloDisplayMenu(final int containerId, final Inventory playerInventory, final @Nullable VendorHoloDisplayBlockEntity vendorHoloDisplayBlockEntity) {
		super(ModMenus.VENDOR_HOLO_DISPLAY_CONTROL, containerId);

		this.vendorHoloDisplayBlockEntity = vendorHoloDisplayBlockEntity;

		final @NotNull Container holoDisplayInventory;
		if (vendorHoloDisplayBlockEntity != null) {
			holoDisplayInventory = vendorHoloDisplayBlockEntity;
		} else {
			holoDisplayInventory = new SimpleContainer(VendorHoloDisplayBlockEntity.HOLO_DISPLAY_SLOT_COUNT);
		}

		this.addHoloDisplaySlots(holoDisplayInventory);
		this.addPlayerInventorySlots(playerInventory);
	}

	private void addHoloDisplaySlots(final @NotNull Container holoDisplayInventory) {
		this.addSlot(new SingleItemSlot(
				holoDisplayInventory,
				VendorHoloDisplayBlockEntity.DISPLAY_SLOT,
				POS_DISPLAY_SLOT_X,
				POS_DISPLAY_SLOT_Y
		));

		for (int index = 0; index < VendorHoloDisplayBlockEntity.DYE_SLOT_COUNT; index++) {
			this.addSlot(new DyeSlot(
					holoDisplayInventory,
					VendorHoloDisplayBlockEntity.DYE_SLOT_START + index,
					POS_DYE_SLOTS_X,
					POS_FIRST_DYE_SLOT_Y + index * POS_DYE_ROW_STEP
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
		final Slot sourceSlot = this.slots.get(index);

		if (!sourceSlot.hasItem()) {
			return ItemStack.EMPTY;
		}

		final ItemStack sourceStack = sourceSlot.getItem();
		final ItemStack originalStack = sourceStack.copy();

		if (index >= MENU_HOLO_DISPLAY_START && index < MENU_HOLO_DISPLAY_END) {
			if (!this.moveItemStackTo(sourceStack, MENU_PLAYER_INVENTORY_START, MENU_PLAYER_INVENTORY_END, true)) {
				return ItemStack.EMPTY;
			}
		} else if (index >= MENU_PLAYER_INVENTORY_START && index < MENU_PLAYER_INVENTORY_END) {
			if (VendorHoloDisplayBlockEntity.isDyeStack(sourceStack)) {
				if (!this.moveOneItemToSlots(sourceStack, VendorHoloDisplayBlockEntity.DYE_SLOT_START, VendorHoloDisplayBlockEntity.DYE_SLOT_START + VendorHoloDisplayBlockEntity.DYE_SLOT_COUNT)) {
					return ItemStack.EMPTY;
				}
			} else {
				if (!this.moveOneItemToSlots(sourceStack, VendorHoloDisplayBlockEntity.DISPLAY_SLOT, VendorHoloDisplayBlockEntity.DISPLAY_SLOT + 1)) {
					return ItemStack.EMPTY;
				}
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

	private boolean moveOneItemToSlots(ItemStack sourceStack, int startIndex, int endIndex) {
		for (int index = startIndex; index < endIndex; index++) {
			Slot targetSlot = this.slots.get(index);

			if (targetSlot.hasItem() || !targetSlot.mayPlace(sourceStack)) {
				continue;
			}

			ItemStack movedStack = sourceStack.copy();
			movedStack.setCount(1);
			targetSlot.set(movedStack);
			targetSlot.setChanged();
			sourceStack.shrink(1);
			return true;
		}

		return false;
	}

	@Override
	public boolean stillValid(final Player player) {
		return this.vendorHoloDisplayBlockEntity == null || this.vendorHoloDisplayBlockEntity.stillValid(player);
	}

	public String getPackedLines() {
		if (this.vendorHoloDisplayBlockEntity == null) {
			return "\n\n\n\n";
		}

		return this.vendorHoloDisplayBlockEntity.getPackedLines();
	}

	public int getPackedLineSizes() {
		if (this.vendorHoloDisplayBlockEntity == null) {
			return 0;
		}

		return this.vendorHoloDisplayBlockEntity.getPackedLineSizes();
	}

	public int getBoardSize() {
		if (this.vendorHoloDisplayBlockEntity == null) {
			return VendorHoloDisplayBlockEntity.DEFAULT_BOARD_SIZE;
		}

		return this.vendorHoloDisplayBlockEntity.getBoardSize();
	}

	public net.minecraft.core.BlockPos getBlockPos() {
		if (this.vendorHoloDisplayBlockEntity == null) {
			return net.minecraft.core.BlockPos.ZERO;
		}

		return this.vendorHoloDisplayBlockEntity.getBlockPos();
	}

	private static class SingleItemSlot extends Slot {
		public SingleItemSlot(Container container, int slot, int x, int y) {
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

	private static class DyeSlot extends SingleItemSlot {
		public DyeSlot(Container container, int slot, int x, int y) {
			super(container, slot, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return VendorHoloDisplayBlockEntity.isDyeStack(stack);
		}
	}
}
