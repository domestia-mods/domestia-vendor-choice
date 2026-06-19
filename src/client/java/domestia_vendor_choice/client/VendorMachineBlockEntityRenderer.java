package domestia_vendor_choice.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import domestia_vendor_choice.VendorMachineBlock;
import domestia_vendor_choice.VendorMachineBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class VendorMachineBlockEntityRenderer implements BlockEntityRenderer<VendorMachineBlockEntity, VendorMachineBlockEntityRenderState> {
	// Front display item size.
	private static final float SIZE_DISPLAY_ITEM_SCALE = 0.33F;

	// Surface gaps in block units.
	// 1 block = 16 texture pixels.
	private static final float POS_DISPLAY_SURFACE_GAP = 0.0625F;
	private static final float POS_BACK_TEXT_SURFACE_GAP = 0.005F;

	// Block-local center.
	private static final float POS_BLOCK_CENTER_X = 0.5F;
	private static final float POS_BLOCK_CENTER_Y = 0.5F;
	private static final float POS_BLOCK_CENTER_Z = 0.5F;

	// Text plane orientation.
	private static final float ROTATION_BACK_TEXT_FACE_DEGREES = 180.0F;
	private static final float ROTATION_BACK_TEXT_UPRIGHT_DEGREES = 180.0F;

	// Item layout: plus sign pattern.
	// Order corresponds to stock slots 0..4:
	// top, left, center, right, bottom
	private static final float POS_ITEM_LEFT_X = -0.31F;
	private static final float POS_ITEM_RIGHT_X = 0.31F;
	private static final float POS_ITEM_TOP_Y = 0.31F;
	private static final float POS_ITEM_BOTTOM_Y = -0.31F;
	private static final float POS_ITEM_CENTER = 0.0F;

	private static final float[][] POS_DISPLAY_ITEMS = {
			{ POS_ITEM_CENTER, POS_ITEM_TOP_Y },
			{ POS_ITEM_LEFT_X, POS_ITEM_CENTER },
			{ POS_ITEM_CENTER, POS_ITEM_CENTER },
			{ POS_ITEM_RIGHT_X, POS_ITEM_CENTER },
			{ POS_ITEM_CENTER, POS_ITEM_BOTTOM_Y }
	};

	// Back text vertical positions.
	// 0.0 = top edge of the back face.
	// 1.0 = bottom edge of the back face.
	// Smaller value means higher text.
	private static final float POS_BACK_TEXT_OWNER_TOP_Y = 0.12F;
	private static final float POS_BACK_TEXT_LABEL_TOP_Y = 0.20F;

	// Back text scale per line.
	private static final float SIZE_BACK_TEXT_OWNER_SCALE = 0.006F;
	private static final float SIZE_BACK_TEXT_LABEL_SCALE = 0.008F;

	// Text trimming.
	private static final int TEXT_MAX_VISIBLE_CHARS = 16;

	// Text colors.
	private static final int COLOR_BACK_TEXT_OWNER = 0xFFCCFF66;
	private static final int COLOR_BACK_TEXT_LABEL = 0xFF99FF00;

	// Rendering constants.
	private static final int OVERLAY_NONE = 0;
	private static final int BACKGROUND_COLOR_NONE = 0;

	// Full brightness packed light. This gives front stock items a glow-frame-like appearance.
	private static final boolean USE_FULL_BRIGHT_FRONT_ITEMS = true;
	private static final int LIGHT_FULL_BRIGHT = 0x00F000F0;

	// IMPORTANT:
	// 0 means no outline. Do not use -1 here: -1 is white ARGB and creates solid white see-through silhouettes.
	private static final int OUTLINE_COLOR_NONE = 0;

	private final Font font;
	private final ItemModelResolver itemModelResolver;

	public VendorMachineBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
		this.font = context.font();
		this.itemModelResolver = context.itemModelResolver();
	}

	@Override
	public VendorMachineBlockEntityRenderState createRenderState() {
		return new VendorMachineBlockEntityRenderState();
	}

	@Override
	public void extractRenderState(
			VendorMachineBlockEntity blockEntity,
			VendorMachineBlockEntityRenderState renderState,
			float tickProgress,
			Vec3 cameraPos,
			@Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
	) {
		BlockEntityRenderer.super.extractRenderState(blockEntity, renderState, tickProgress, cameraPos, crumblingOverlay);

		Direction facing = blockEntity.getBlockState().getValue(VendorMachineBlock.FACING);
		renderState.setFacing(facing);
		renderState.setOwnerName(blockEntity.getOwnerName());
		renderState.setDisplayName(blockEntity.getDisplayName());

		for (int index = 0; index < VendorMachineBlockEntity.STOCK_SLOT_COUNT; index++) {
			renderState.setDisplayStack(index, blockEntity.getFrontDisplayStack(index));
		}
	}

	@Override
	public void submit(
			VendorMachineBlockEntityRenderState renderState,
			PoseStack poseStack,
			SubmitNodeCollector collector,
			CameraRenderState cameraState
	) {
		this.submitFrontDisplayItems(renderState, poseStack, collector);
		this.submitBackText(renderState, poseStack, collector);
	}

	private void submitFrontDisplayItems(
			VendorMachineBlockEntityRenderState renderState,
			PoseStack poseStack,
			SubmitNodeCollector collector
	) {
		Entity renderEntity = this.getRenderEntity();

		if (renderEntity == null) {
			return;
		}

		for (int index = 0; index < VendorMachineBlockEntity.STOCK_SLOT_COUNT; index++) {
			ItemStack displayStack = renderState.getDisplayStack(index);

			if (displayStack.isEmpty()) {
				continue;
			}

			this.submitDisplayItem(renderState, poseStack, collector, displayStack, index, renderEntity);
		}
	}

	private Entity getRenderEntity() {
		return Minecraft.getInstance().player;
	}

	private void submitDisplayItem(
			VendorMachineBlockEntityRenderState renderState,
			PoseStack poseStack,
			SubmitNodeCollector collector,
			ItemStack stack,
			int displayIndex,
			Entity renderEntity
	) {
		float[] itemPosition = POS_DISPLAY_ITEMS[displayIndex];

		poseStack.pushPose();

		this.applyFrontFaceTransform(poseStack, renderState.getFacing());
		this.applyItemPositionTransform(poseStack, itemPosition[0], itemPosition[1]);

		ItemStackRenderState itemRenderState = new ItemStackRenderState();

		this.itemModelResolver.updateForNonLiving(
				itemRenderState,
				stack,
				ItemDisplayContext.FIXED,
				renderEntity
		);

		itemRenderState.submit(
				poseStack,
				collector,
				this.getFrontItemLightCoords(renderState),
				OVERLAY_NONE,
				OUTLINE_COLOR_NONE
		);

		poseStack.popPose();
	}

	private int getFrontItemLightCoords(VendorMachineBlockEntityRenderState renderState) {
		if (USE_FULL_BRIGHT_FRONT_ITEMS) {
			return LIGHT_FULL_BRIGHT;
		}

		return renderState.lightCoords;
	}

	private void submitBackText(
			VendorMachineBlockEntityRenderState renderState,
			PoseStack poseStack,
			SubmitNodeCollector collector
	) {
		String ownerText = LocalizedRenderText.trim(
				LocalizedRenderText.resolve(renderState.getOwnerName(), VendorMachineBlockEntity.DEFAULT_OWNER_NAME_KEY),
				TEXT_MAX_VISIBLE_CHARS
		);
		String labelText = LocalizedRenderText.trim(
				LocalizedRenderText.resolve(renderState.getDisplayName(), VendorMachineBlockEntity.DEFAULT_DISPLAY_NAME_KEY),
				TEXT_MAX_VISIBLE_CHARS
		);

		poseStack.pushPose();

		this.applyBackFaceTransform(poseStack, renderState.getFacing());

		this.submitBackTextLine(
				poseStack,
				collector,
				renderState,
				ownerText,
				POS_BACK_TEXT_OWNER_TOP_Y,
				SIZE_BACK_TEXT_OWNER_SCALE,
				COLOR_BACK_TEXT_OWNER
		);

		this.submitBackTextLine(
				poseStack,
				collector,
				renderState,
				labelText,
				POS_BACK_TEXT_LABEL_TOP_Y,
				SIZE_BACK_TEXT_LABEL_SCALE,
				COLOR_BACK_TEXT_LABEL
		);

		poseStack.popPose();
	}

	private void submitBackTextLine(
			PoseStack poseStack,
			SubmitNodeCollector collector,
			VendorMachineBlockEntityRenderState renderState,
			String text,
			float topY,
			float scale,
			int color
	) {
		if (text.isBlank()) {
			return;
		}

		poseStack.pushPose();

		poseStack.translate(0.0F, getBackTextLocalY(topY), 0.0F);
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
		poseStack.translate(0.0F, 0.0F, -0.5F - POS_DISPLAY_SURFACE_GAP);
	}

	private void applyBackFaceTransform(PoseStack poseStack, Direction facing) {
		poseStack.translate(POS_BLOCK_CENTER_X, POS_BLOCK_CENTER_Y, POS_BLOCK_CENTER_Z);

		// Renderer local front is north, so local back is south.
		// Rotate local north to the block state's facing direction.
		poseStack.mulPose(Axis.YP.rotationDegrees(getFacingRotationDegrees(facing)));

		// Move just outside the back face.
		poseStack.translate(0.0F, 0.0F, 0.5F + POS_BACK_TEXT_SURFACE_GAP);

		// Turn the text plane outward from the back side.
		poseStack.mulPose(Axis.YP.rotationDegrees(ROTATION_BACK_TEXT_FACE_DEGREES));

		// Rotate the text inside its own plane so it is upright.
		poseStack.mulPose(Axis.ZP.rotationDegrees(ROTATION_BACK_TEXT_UPRIGHT_DEGREES));
	}

	private void applyItemPositionTransform(PoseStack poseStack, float xOffset, float yOffset) {
		poseStack.translate(xOffset, yOffset, 0.0F);

		// FIXED context uses item-frame-like orientation.
		// Rotate so the item plane faces outward from the machine front.
		//poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

		poseStack.scale(SIZE_DISPLAY_ITEM_SCALE, SIZE_DISPLAY_ITEM_SCALE, SIZE_DISPLAY_ITEM_SCALE);
	}

	private static float getBackTextLocalY(float topY) {
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