package domestia_vendor_choice;

import java.util.function.Consumer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public final class VendorMenuSlots {
	public static final int SLOT_SIZE = 18;

	public static final int PLAYER_INVENTORY_COLUMNS = 9;
	public static final int PLAYER_INVENTORY_ROWS = 3;
	public static final int PLAYER_MAIN_SLOT_COUNT = PLAYER_INVENTORY_COLUMNS * PLAYER_INVENTORY_ROWS;
	public static final int PLAYER_HOTBAR_SLOT_COUNT = PLAYER_INVENTORY_COLUMNS;
	public static final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_MAIN_SLOT_COUNT + PLAYER_HOTBAR_SLOT_COUNT;

	public static final int PLAYER_HOTBAR_INVENTORY_INDEX_START = 0;
	public static final int PLAYER_MAIN_INVENTORY_INDEX_START = 9;

	private VendorMenuSlots() {
	}

	public static void addPlayerInventorySlots(
			Consumer<Slot> slotConsumer,
			Inventory playerInventory,
			int left,
			int mainInventoryTop,
			int hotbarTop
	) {
		addPlayerMainInventorySlots(slotConsumer, playerInventory, left, mainInventoryTop);
		addPlayerHotbarSlots(slotConsumer, playerInventory, left, hotbarTop);
	}

	private static void addPlayerMainInventorySlots(
			Consumer<Slot> slotConsumer,
			Inventory playerInventory,
			int left,
			int top
	) {
		for (int row = 0; row < PLAYER_INVENTORY_ROWS; row++) {
			for (int column = 0; column < PLAYER_INVENTORY_COLUMNS; column++) {
				slotConsumer.accept(new Slot(
						playerInventory,
						PLAYER_MAIN_INVENTORY_INDEX_START + column + row * PLAYER_INVENTORY_COLUMNS,
						left + column * SLOT_SIZE,
						top + row * SLOT_SIZE
				));
			}
		}
	}

	private static void addPlayerHotbarSlots(
			Consumer<Slot> slotConsumer,
			Inventory playerInventory,
			int left,
			int top
	) {
		for (int column = 0; column < PLAYER_INVENTORY_COLUMNS; column++) {
			slotConsumer.accept(new Slot(
					playerInventory,
					PLAYER_HOTBAR_INVENTORY_INDEX_START + column,
					left + column * SLOT_SIZE,
					top
			));
		}
	}
}
