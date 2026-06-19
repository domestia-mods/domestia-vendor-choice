package domestia_vendor_choice.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import domestia_vendor_choice.VendorSafeBlock;
import domestia_vendor_choice.VendorSafeBlockEntity;
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

public class VendorSafeBlockEntityRenderer implements BlockEntityRenderer<VendorSafeBlockEntity, VendorSafeBlockEntityRenderState> {
	// Text surface gap in block units.
	// 1 block = 16 texture pixels.
	private static final float POS_FRONT_TEXT_SURFACE_GAP = 0.005F;

	// Block-local center.
	private static final float POS_BLOCK_CENTER_X = 0.5F;
	private static final float POS_BLOCK_CENTER_Y = 0.5F;
	private static final float POS_BLOCK_CENTER_Z = 0.5F;

	// Text plane orientation.
	// Safe label is rendered on the front face.
	// Keep this at 0.0F. Rotating the front text plane by 180 degrees around Y hides the text.
	private static final float ROTATION_FRONT_TEXT_FACE_DEGREES = 0.0F;

	// In-plane rotation that makes the text upright on the front face.
	private static final float ROTATION_FRONT_TEXT_UPRIGHT_DEGREES = 180.0F;

	// Front text vertical positions.
	// 0.0 = top edge of the front face.
	// 1.0 = bottom edge of the front face.
	// Smaller value means higher text.
	private static final float POS_FRONT_TEXT_OWNER_TOP_Y = 0.12F;
	private static final float POS_FRONT_TEXT_LABEL_TOP_Y = 0.20F;

	// Front text scale per line.
	private static final float SIZE_FRONT_TEXT_OWNER_SCALE = 0.006F;
	private static final float SIZE_FRONT_TEXT_LABEL_SCALE = 0.008F;

	// Text trimming.
	private static final int TEXT_MAX_VISIBLE_CHARS = 16;

	// Text colors.
	private static final int COLOR_FRONT_TEXT_OWNER = 0xFFCCFF66;
	private static final int COLOR_FRONT_TEXT_LABEL = 0xFF99FF00;

	// Rendering constants.
	private static final int BACKGROUND_COLOR_NONE = 0;
	private static final int LIGHT_FULL_BRIGHT = 0x00F000F0;

	// IMPORTANT:
	// 0 means no outline. Do not use -1 here: -1 is white ARGB and creates solid white see-through silhouettes.
	private static final int OUTLINE_COLOR_NONE = 0;

	private final Font font;

	public VendorSafeBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
		this.font = context.font();
	}

	@Override
	public VendorSafeBlockEntityRenderState createRenderState() {
		return new VendorSafeBlockEntityRenderState();
	}

	@Override
	public void extractRenderState(
			VendorSafeBlockEntity blockEntity,
			VendorSafeBlockEntityRenderState renderState,
			float tickProgress,
			Vec3 cameraPos,
			@Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
	) {
		BlockEntityRenderer.super.extractRenderState(blockEntity, renderState, tickProgress, cameraPos, crumblingOverlay);

		Direction facing = blockEntity.getBlockState().getValue(VendorSafeBlock.FACING);
		renderState.setFacing(facing);
		renderState.setOwnerName(blockEntity.getOwnerName());
		renderState.setDisplayName(blockEntity.getDisplayName());
	}

	@Override
	public void submit(
			VendorSafeBlockEntityRenderState renderState,
			PoseStack poseStack,
			SubmitNodeCollector collector,
			CameraRenderState cameraState
	) {
		this.submitFrontText(renderState, poseStack, collector);
	}

	private void submitFrontText(
			VendorSafeBlockEntityRenderState renderState,
			PoseStack poseStack,
			SubmitNodeCollector collector
	) {
		String ownerText = LocalizedRenderText.trim(
				LocalizedRenderText.resolve(renderState.getOwnerName(), VendorSafeBlockEntity.DEFAULT_OWNER_NAME_KEY),
				TEXT_MAX_VISIBLE_CHARS
		);
		String labelText = LocalizedRenderText.trim(
				LocalizedRenderText.resolve(renderState.getDisplayName(), VendorSafeBlockEntity.DEFAULT_DISPLAY_NAME_KEY),
				TEXT_MAX_VISIBLE_CHARS
		);

		poseStack.pushPose();

		this.applyFrontFaceTransform(poseStack, renderState.getFacing());

		this.submitFrontTextLine(
				poseStack,
				collector,
				ownerText,
				POS_FRONT_TEXT_OWNER_TOP_Y,
				SIZE_FRONT_TEXT_OWNER_SCALE,
				COLOR_FRONT_TEXT_OWNER
		);

		this.submitFrontTextLine(
				poseStack,
				collector,
				labelText,
				POS_FRONT_TEXT_LABEL_TOP_Y,
				SIZE_FRONT_TEXT_LABEL_SCALE,
				COLOR_FRONT_TEXT_LABEL
		);

		poseStack.popPose();
	}

	private void submitFrontTextLine(
			PoseStack poseStack,
			SubmitNodeCollector collector,
			String text,
			float topY,
			float scale,
			int color
	) {
		if (text.isBlank()) {
			return;
		}

		poseStack.pushPose();

		poseStack.translate(0.0F, getTextLocalY(topY), 0.0F);
		poseStack.scale(scale, scale, scale);

		float width = this.font.width(text);

		collector.submitText(
				poseStack,
				-width / 2.0F,
				0.0F,
				Component.literal(text).getVisualOrderText(),
				false,
				Font.DisplayMode.NORMAL,
				LIGHT_FULL_BRIGHT,
				color,
				BACKGROUND_COLOR_NONE,
				OUTLINE_COLOR_NONE
		);

		poseStack.popPose();
	}

	private void applyFrontFaceTransform(PoseStack poseStack, Direction facing) {
		poseStack.translate(POS_BLOCK_CENTER_X, POS_BLOCK_CENTER_Y, POS_BLOCK_CENTER_Z);

		// Renderer local front is north. Rotate local north to the block state's facing direction.
		poseStack.mulPose(Axis.YP.rotationDegrees(getFacingRotationDegrees(facing)));

		// Move just outside the front face.
		poseStack.translate(0.0F, 0.0F, -0.5F - POS_FRONT_TEXT_SURFACE_GAP);

		// Keep the text plane visible on the front face.
		poseStack.mulPose(Axis.YP.rotationDegrees(ROTATION_FRONT_TEXT_FACE_DEGREES));

		// Rotate the text inside its own plane so it is upright.
		poseStack.mulPose(Axis.ZP.rotationDegrees(ROTATION_FRONT_TEXT_UPRIGHT_DEGREES));
	}

	private static float getTextLocalY(float topY) {
		return topY - 0.5F;
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