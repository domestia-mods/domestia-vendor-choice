package domestia_vendor_choice.client;

import domestia_vendor_choice.VendorMachineBlockEntity;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

public class VendorMachineBlockEntityRenderState extends BlockEntityRenderState {
	private Direction facing = Direction.NORTH;
	private String ownerName = VendorMachineBlockEntity.DEFAULT_OWNER_NAME;
	private String displayName = VendorMachineBlockEntity.DEFAULT_DISPLAY_NAME;

	private final ItemStack[] displayStacks = new ItemStack[VendorMachineBlockEntity.STOCK_SLOT_COUNT];

	public VendorMachineBlockEntityRenderState() {
		for (int index = 0; index < this.displayStacks.length; index++) {
			this.displayStacks[index] = ItemStack.EMPTY;
		}
	}

	public Direction getFacing() {
		return this.facing;
	}

	public void setFacing(Direction facing) {
		this.facing = facing;
	}

	public String getOwnerName() {
		return this.ownerName;
	}

	public void setOwnerName(String ownerName) {
		this.ownerName = normalizeText(ownerName, VendorMachineBlockEntity.DEFAULT_OWNER_NAME);
	}

	public String getDisplayName() {
		return this.displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = normalizeText(displayName, VendorMachineBlockEntity.DEFAULT_DISPLAY_NAME);
	}

	public ItemStack getDisplayStack(int index) {
		if (index < 0 || index >= this.displayStacks.length) {
			return ItemStack.EMPTY;
		}

		return this.displayStacks[index];
	}

	public void setDisplayStack(int index, ItemStack stack) {
		if (index < 0 || index >= this.displayStacks.length) {
			return;
		}

		this.displayStacks[index] = stack.copy();
	}

	private static String normalizeText(String text, String fallback) {
		if (text == null || text.isBlank()) {
			return fallback;
		}

		return text;
	}
}