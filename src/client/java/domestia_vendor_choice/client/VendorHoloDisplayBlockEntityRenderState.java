package domestia_vendor_choice.client;

import domestia_vendor_choice.VendorHoloDisplayBlockEntity;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

public class VendorHoloDisplayBlockEntityRenderState extends BlockEntityRenderState {
	private BlockPos blockPos = BlockPos.ZERO;
	private Direction facing = Direction.NORTH;
	private boolean renderBackSide;
	private int boardSize = VendorHoloDisplayBlockEntity.DEFAULT_BOARD_SIZE;
	private ItemStack displayStack = ItemStack.EMPTY;
	private final String[] lines = new String[VendorHoloDisplayBlockEntity.LINE_COUNT];
	private final int[] lineSizes = new int[VendorHoloDisplayBlockEntity.LINE_COUNT];
	private final int[] lineColors = new int[VendorHoloDisplayBlockEntity.LINE_COUNT];

	public VendorHoloDisplayBlockEntityRenderState() {
		for (int index = 0; index < VendorHoloDisplayBlockEntity.LINE_COUNT; index++) {
			this.lines[index] = "";
			this.lineSizes[index] = VendorHoloDisplayBlockEntity.DEFAULT_FONT_SIZE;
			this.lineColors[index] = 0xFFFFFFFF;
		}
	}

	public BlockPos getBlockPos() {
		return this.blockPos;
	}

	public void setBlockPos(BlockPos blockPos) {
		this.blockPos = blockPos == null ? BlockPos.ZERO : blockPos.immutable();
	}

	public Direction getFacing() {
		return this.facing;
	}

	public void setFacing(Direction facing) {
		this.facing = facing;
	}

	public boolean shouldRenderBackSide() {
		return this.renderBackSide;
	}

	public void setRenderBackSide(boolean renderBackSide) {
		this.renderBackSide = renderBackSide;
	}

	public int getBoardSize() {
		return this.boardSize;
	}

	public void setBoardSize(int boardSize) {
		this.boardSize = VendorHoloDisplayBlockEntity.normalizeBoardSize(boardSize);
	}

	public ItemStack getDisplayStack() {
		return this.displayStack;
	}

	public void setDisplayStack(ItemStack displayStack) {
		this.displayStack = displayStack == null ? ItemStack.EMPTY : displayStack.copy();
	}

	public String getLine(int index) {
		if (index < 0 || index >= this.lines.length) {
			return "";
		}

		return this.lines[index];
	}

	public void setLine(int index, String line) {
		if (index < 0 || index >= this.lines.length) {
			return;
		}

		this.lines[index] = VendorHoloDisplayBlockEntity.normalizeLine(line);
	}

	public int getLineSize(int index) {
		if (index < 0 || index >= this.lineSizes.length) {
			return VendorHoloDisplayBlockEntity.DEFAULT_FONT_SIZE;
		}

		return this.lineSizes[index];
	}

	public void setLineSize(int index, int lineSize) {
		if (index < 0 || index >= this.lineSizes.length) {
			return;
		}

		this.lineSizes[index] = VendorHoloDisplayBlockEntity.normalizeFontSize(lineSize);
	}

	public int getLineColor(int index) {
		if (index < 0 || index >= this.lineColors.length) {
			return 0xFFFFFFFF;
		}

		return this.lineColors[index];
	}

	public void setLineColor(int index, int lineColor) {
		if (index < 0 || index >= this.lineColors.length) {
			return;
		}

		this.lineColors[index] = lineColor;
	}
}
