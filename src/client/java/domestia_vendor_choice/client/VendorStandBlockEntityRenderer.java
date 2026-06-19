package domestia_vendor_choice.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import domestia_vendor_choice.VendorStandBlock;
import domestia_vendor_choice.VendorStandBlockEntity;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class VendorStandBlockEntityRenderer implements BlockEntityRenderer<VendorStandBlockEntity, VendorStandBlockEntityRenderState> {
	private static final float BLOCK_CENTER = 0.5F;
	private static final float SURFACE_GAP = 0.004F;
	private static final float FLOOR_PANEL_MODEL_ANGLE_DEGREES = -17.5F;
	private static final float FLOOR_TEXT_PLANE_ANGLE_DEGREES = 90.0F + FLOOR_PANEL_MODEL_ANGLE_DEGREES;
	private static final float FLOOR_PANEL_SURFACE_Y = 0.79018F;
	private static final float FLOOR_PANEL_SURFACE_Z = 0.46915F;
	private static final float WALL_PANEL_SURFACE_Z = 15.0F / 16.0F;

	private static final float OWNER_TEXT_SCALE = 0.0055F;
	private static final float TITLE_TEXT_SCALE = 0.0070F;

	private static final float FLOOR_OWNER_LINE_Y = -0.195F;
	private static final float FLOOR_TITLE_LINE_Y = -0.135F;
	private static final float WALL_OWNER_LINE_Y = -0.260F;
	private static final float WALL_TITLE_LINE_Y = -0.200F;

	private static final int OWNER_MAX_VISIBLE_CHARS = 24;
	private static final int TITLE_MAX_VISIBLE_CHARS = 14;
	private static final String TRIM_SUFFIX = "...";

	private static final int COLOR_OWNER = 0xFFCCFF66;
	private static final int COLOR_TITLE = 0xFF99FF00;
	private static final int BACKGROUND_COLOR_NONE = 0;
	private static final int OUTLINE_COLOR_NONE = 0;

	private final Font font;

	public VendorStandBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
		this.font = context.font();
	}

	@Override
	public VendorStandBlockEntityRenderState createRenderState() {
		return new VendorStandBlockEntityRenderState();
	}

	@Override
	public void extractRenderState(
			VendorStandBlockEntity blockEntity,
			VendorStandBlockEntityRenderState renderState,
			float tickProgress,
			Vec3 cameraPos,
			@Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
	) {
		BlockEntityRenderer.super.extractRenderState(blockEntity, renderState, tickProgress, cameraPos, crumblingOverlay);

		renderState.setFacing(blockEntity.getBlockState().getValue(VendorStandBlock.FACING));
		renderState.setWall(blockEntity.getBlockState().getValue(VendorStandBlock.WALL));
		renderState.setOwnerName(blockEntity.getOwnerName());
		renderState.setTitle(blockEntity.getTitleText());
	}

	@Override
	public void submit(
			VendorStandBlockEntityRenderState renderState,
			PoseStack poseStack,
			SubmitNodeCollector collector,
			CameraRenderState cameraState
	) {
		String ownerText = trim(renderState.getOwnerName(), OWNER_MAX_VISIBLE_CHARS);
		String titleText = trim(renderState.getTitle(), TITLE_MAX_VISIBLE_CHARS);

		if (ownerText.isBlank() && titleText.isBlank()) {
			return;
		}

		boolean wall = renderState.isWall();
		float ownerLineY = wall ? WALL_OWNER_LINE_Y : FLOOR_OWNER_LINE_Y;
		float titleLineY = wall ? WALL_TITLE_LINE_Y : FLOOR_TITLE_LINE_Y;

		poseStack.pushPose();
		this.applyPanelTransform(poseStack, renderState.getFacing(), wall);
		this.submitTextLine(poseStack, collector, renderState, ownerText, ownerLineY, OWNER_TEXT_SCALE, COLOR_OWNER);
		this.submitTextLine(poseStack, collector, renderState, titleText, titleLineY, TITLE_TEXT_SCALE, COLOR_TITLE);
		poseStack.popPose();
	}

	private void submitTextLine(
			PoseStack poseStack,
			SubmitNodeCollector collector,
			VendorStandBlockEntityRenderState renderState,
			String text,
			float localY,
			float scale,
			int color
	) {
		if (text.isBlank()) {
			return;
		}

		poseStack.pushPose();
		poseStack.translate(0.0F, localY, 0.0F);
		poseStack.scale(scale, scale, scale);

		float width = this.font.width(text);
		collector.submitText(
				poseStack,
				-width / 2.0F,
				0.0F,
				Component.literal(text).getVisualOrderText(),
				false,
				Font.DisplayMode.NORMAL,
				renderState.lightCoords,
				color,
				BACKGROUND_COLOR_NONE,
				OUTLINE_COLOR_NONE
		);
		poseStack.popPose();
	}

	private void applyPanelTransform(PoseStack poseStack, Direction facing, boolean wall) {
		poseStack.translate(BLOCK_CENTER, BLOCK_CENTER, BLOCK_CENTER);
		poseStack.mulPose(Axis.YP.rotationDegrees(getFacingRotationDegrees(facing)));

		if (wall) {
			// The wall model sits against the local south side and faces local north.
			// Its writing surface is recessed two model units behind the outer frame.
			poseStack.translate(0.0F, 0.0F, WALL_PANEL_SURFACE_Z - BLOCK_CENTER - SURFACE_GAP);
			poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
			return;
		}

		// Place the text on the actual upper surface of the panel. The font starts
		// in a vertical plane, so the required rotation is 90 degrees plus the
		// model panel angle rather than the panel angle itself.
		poseStack.translate(
				0.0F,
				FLOOR_PANEL_SURFACE_Y - BLOCK_CENTER,
				FLOOR_PANEL_SURFACE_Z - BLOCK_CENTER
		);
		poseStack.mulPose(Axis.XP.rotationDegrees(FLOOR_TEXT_PLANE_ANGLE_DEGREES));
		poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
	}

	private static String trim(String text, int maxVisibleChars) {
		if (text == null || text.isBlank()) {
			return "";
		}

		if (text.length() <= maxVisibleChars) {
			return text;
		}

		return text.substring(0, maxVisibleChars) + TRIM_SUFFIX;
	}

	private static float getFacingRotationDegrees(Direction facing) {
		return switch (facing) {
			case NORTH -> 0.0F;
			case EAST -> -90.0F;
			case SOUTH -> 180.0F;
			case WEST -> 90.0F;
			default -> 0.0F;
		};
	}
}
