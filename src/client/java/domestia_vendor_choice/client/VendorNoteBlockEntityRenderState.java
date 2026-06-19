package domestia_vendor_choice.client;

import domestia_vendor_choice.VendorNoteBlockEntity;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;

public class VendorNoteBlockEntityRenderState extends BlockEntityRenderState {
	private Direction facing = Direction.NORTH;
	private boolean wall;
	private String ownerName = VendorNoteBlockEntity.DEFAULT_OWNER_NAME;
	private String title = "";

	public Direction getFacing() {
		return this.facing;
	}

	public void setFacing(Direction facing) {
		this.facing = facing;
	}

	public boolean isWall() {
		return this.wall;
	}

	public void setWall(boolean wall) {
		this.wall = wall;
	}

	public String getOwnerName() {
		return this.ownerName;
	}

	public void setOwnerName(String ownerName) {
		this.ownerName = ownerName == null || ownerName.isBlank()
				? VendorNoteBlockEntity.DEFAULT_OWNER_NAME
				: ownerName;
	}

	public String getTitle() {
		return this.title;
	}

	public void setTitle(String title) {
		this.title = title == null ? "" : title;
	}
}
