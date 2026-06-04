package domestia_vendor_choice.client;

import domestia_vendor_choice.VendorSafeBlockEntity;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;

public class VendorSafeBlockEntityRenderState extends BlockEntityRenderState {
	private Direction facing = Direction.NORTH;
	private String ownerName = VendorSafeBlockEntity.DEFAULT_OWNER_NAME;
	private String displayName = VendorSafeBlockEntity.DEFAULT_DISPLAY_NAME;

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
		this.ownerName = normalizeText(ownerName, VendorSafeBlockEntity.DEFAULT_OWNER_NAME);
	}

	public String getDisplayName() {
		return this.displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = normalizeText(displayName, VendorSafeBlockEntity.DEFAULT_DISPLAY_NAME);
	}

	private static String normalizeText(String text, String fallback) {
		if (text == null || text.isBlank()) {
			return fallback;
		}

		return text;
	}
}