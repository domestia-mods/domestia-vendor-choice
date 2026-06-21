package domestia_vendor_choice.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import domestia_vendor_choice.DomestiaVendorChoice;
import domestia_vendor_choice.VendorHoloDisplayBlock;
import domestia_vendor_choice.VendorHoloDisplayBlockEntity;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public final class VendorHoloDisplayTextWorldRenderer {
	private static final float BLOCK_CENTER = 0.5F;
	private static final float PANEL_BOTTOM_Y = 1.18F;
	private static final float PANEL_CEILING_TOP_Y = -0.18F;
	private static final float PANEL_FORWARD_OFFSET = 0.04F;
	private static final float PANEL_Z = 0.0F;
	private static final float ITEM_Z = -0.010F;
	private static final float TEXT_Z = -0.035F;
	private static final float PANEL_HORIZONTAL_PADDING_RATIO = 0.03F;
	private static final float PANEL_VERTICAL_PADDING_RATIO = 0.03F;
	private static final float PANEL_LINE_SPACING_RATIO = 0.04F;
	private static final float IMAGE_SIZE_RATIO = 0.50F;
	private static final double ACTIVE_RENDER_DISTANCE = 128.0D;
	private static final double ACTIVE_RENDER_DISTANCE_SQUARED = ACTIVE_RENDER_DISTANCE * ACTIVE_RENDER_DISTANCE;
	private static final float MEDIUM_FONT_SCALE_RATIO = 0.75F;
	private static final float SMALL_FONT_SCALE_RATIO = 0.75F * 0.75F;
	private static final float LINE_HEIGHT_UNITS = 7.0F;
	private static final float GLYPH_GAP_UNITS = 1.0F;
	private static final float MAX_LINE_UNITS = VendorHoloDisplayBlockEntity.MAX_LINE_LENGTH * 6.0F - GLYPH_GAP_UNITS;
	private static final float OUTLINE_OFFSET_UNITS = 0.35F;

	private static final int LIGHT_FULL_BRIGHT = 0x00F000F0;
	private static final int OVERLAY_NONE = 0;
	private static final int HOLOGRAM_PANEL_COLOR = 0xFFFFFFFF;

	private static final float OUTLINE_DARKEN_FACTOR = 0.45F;
	private static final int COLORED_CORE_MAX_CHANNEL = 190;
	private static final int BLACK_TEXT_OUTLINE_COLOR = 0xFFFFFFFF;
	private static final int TEXT_PASS_OUTLINE = 0;
	private static final int TEXT_PASS_COLORED_CORE = 1;
	private static final int TEXT_PASS_BLACK_CORE = 2;

	private static final Identifier HOLOGRAM_TEXTURE = Identifier.fromNamespaceAndPath(
			DomestiaVendorChoice.MOD_ID,
			"textures/block/vendor_holo_display_hologram.png"
	);
	private static final Identifier TEXT_PIXEL_TEXTURE = Identifier.fromNamespaceAndPath(
			DomestiaVendorChoice.MOD_ID,
			"textures/block/vendor_holo_display_text_pixel.png"
	);

	private static final int[][] OUTLINE_OFFSETS = new int[][] {
			{-1, -1}, {0, -1}, {1, -1},
			{-1, 0},           {1, 0},
			{-1, 1},  {0, 1},  {1, 1}
	};

	private static final Set<VendorHoloDisplayBlockEntity> ACTIVE_DISPLAYS = Collections.newSetFromMap(new IdentityHashMap<>());
	private static final List<QueuedDisplay> QUEUED_DISPLAYS = new ArrayList<>();
	private static final SubmitNodeStorage ITEM_SUBMIT_NODES = new SubmitNodeStorage();
	private static ItemModelResolver itemModelResolver;

	private VendorHoloDisplayTextWorldRenderer() {
	}

	public static void setItemModelResolver(ItemModelResolver resolver) {
		itemModelResolver = resolver;
	}

	public static void initialize() {
		ClientBlockEntityEvents.BLOCK_ENTITY_LOAD.register((blockEntity, world) -> {
			if (blockEntity instanceof VendorHoloDisplayBlockEntity vendorHoloDisplayBlockEntity) {
				ACTIVE_DISPLAYS.add(vendorHoloDisplayBlockEntity);
			}
		});
		ClientBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((blockEntity, world) -> {
			if (blockEntity instanceof VendorHoloDisplayBlockEntity vendorHoloDisplayBlockEntity) {
				ACTIVE_DISPLAYS.remove(vendorHoloDisplayBlockEntity);
			}
		});
		LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(VendorHoloDisplayTextWorldRenderer::renderQueuedDisplays);
	}

	public static float getLineHeight(float boardWidth, int lineSize) {
		return getLineHeight(boardWidth, lineSize, "");
	}

	private static float getLineHeight(float boardWidth, int lineSize, String text) {
		return getPixelSize(boardWidth, lineSize, text) * LINE_HEIGHT_UNITS;
	}

	private static void queueActiveDisplays(Minecraft minecraft, Vec3 camera) {
		if (minecraft.level == null || ACTIVE_DISPLAYS.isEmpty()) {
			return;
		}

		Iterator<VendorHoloDisplayBlockEntity> iterator = ACTIVE_DISPLAYS.iterator();
		while (iterator.hasNext()) {
			VendorHoloDisplayBlockEntity blockEntity = iterator.next();

			if (blockEntity.isRemoved()) {
				iterator.remove();
				continue;
			}

			BlockPos blockPos = blockEntity.getBlockPos();
			if (Vec3.atCenterOf(blockPos).distanceToSqr(camera) > ACTIVE_RENDER_DISTANCE_SQUARED) {
				continue;
			}

			Direction facing = blockEntity.getBlockState().getValue(VendorHoloDisplayBlock.FACING);
			boolean ceiling = blockEntity.getBlockState().getValue(VendorHoloDisplayBlock.CEILING);
			List<TextLine> lines = collectTextLines(blockEntity);
			ItemStack displayStack = blockEntity.getDisplayStack();
			boolean hasItem = !displayStack.isEmpty();
			boolean hasText = !lines.isEmpty();

			if (!hasItem && !hasText) {
				continue;
			}

			float boardWidth = blockEntity.getBoardSize();
			LayoutMetrics metrics = computeLayout(boardWidth, hasItem, lines);

			queueDisplay(
					blockPos,
					facing,
					ceiling,
					shouldRenderBackSide(blockPos, facing, camera),
					boardWidth,
					metrics.boardHeight(),
					metrics.verticalPadding(),
					metrics.lineSpacing(),
					metrics.itemRowHeight(),
					metrics.imageBottomPadding(),
					displayStack.copy(),
					lines
			);
		}
	}

	private static List<TextLine> collectTextLines(VendorHoloDisplayBlockEntity blockEntity) {
		List<TextLine> lines = new ArrayList<>();

		for (int index = 0; index < VendorHoloDisplayBlockEntity.LINE_COUNT; index++) {
			String text = blockEntity.getLine(index);
			if (text == null || text.isBlank()) {
				continue;
			}

			lines.add(new TextLine(text, blockEntity.getLineSize(index), normalizeArgb(blockEntity.getLineColor(index))));
		}

		return lines;
	}

	private static LayoutMetrics computeLayout(float boardWidth, boolean hasItem, List<TextLine> lines) {
		float verticalPadding = boardWidth * PANEL_VERTICAL_PADDING_RATIO;
		float lineSpacing = boardWidth * PANEL_LINE_SPACING_RATIO;
		float itemRowHeight = hasItem ? boardWidth * IMAGE_SIZE_RATIO : 0.0F;
		float imageBottomPadding = hasItem && !lines.isEmpty() ? verticalPadding : 0.0F;
		float textHeight = 0.0F;

		for (TextLine line : lines) {
			textHeight += getLineHeight(boardWidth, line.size(), line.text());
		}

		if (lines.size() > 1) {
			textHeight += lineSpacing * (lines.size() - 1);
		}

		float boardHeight = verticalPadding * 2.0F + itemRowHeight + imageBottomPadding + textHeight;
		return new LayoutMetrics(verticalPadding, lineSpacing, itemRowHeight, imageBottomPadding, boardHeight);
	}

	private static boolean shouldRenderBackSide(BlockPos blockPos, Direction facing, Vec3 camera) {
		Vec3 blockCenter = Vec3.atCenterOf(blockPos);
		Vec3 cameraOffset = camera.subtract(blockCenter);
		double facingDot = cameraOffset.x * facing.getStepX()
				+ cameraOffset.y * facing.getStepY()
				+ cameraOffset.z * facing.getStepZ();
		return facingDot < 0.0D;
	}

	private static void queueDisplay(
			BlockPos blockPos,
			Direction facing,
			boolean ceiling,
			boolean backSide,
			float boardWidth,
			float boardHeight,
			float verticalPadding,
			float lineSpacing,
			float itemRowHeight,
			float imageBottomPadding,
			ItemStack displayStack,
			List<TextLine> lines
	) {
		QUEUED_DISPLAYS.add(new QueuedDisplay(
				blockPos.immutable(),
				facing,
				ceiling,
				backSide,
				boardWidth,
				boardHeight,
				verticalPadding,
				lineSpacing,
				itemRowHeight,
				imageBottomPadding,
				displayStack,
				List.copyOf(lines)
		));
	}

	private static void renderQueuedDisplays(LevelRenderContext context) {
		Minecraft minecraft = Minecraft.getInstance();
		MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
		PoseStack poseStack = context.poseStack();
		Vec3 camera = context.levelState().cameraRenderState.pos;

		queueActiveDisplays(minecraft, camera);

		if (QUEUED_DISPLAYS.isEmpty()) {
			return;
		}

		for (QueuedDisplay queuedDisplay : QUEUED_DISPLAYS) {
			renderPanel(bufferSource, poseStack, camera, queuedDisplay);
		}

		bufferSource.endBatch();

		for (QueuedDisplay queuedDisplay : QUEUED_DISPLAYS) {
			renderDisplayItem(bufferSource, poseStack, camera, queuedDisplay);
		}

		bufferSource.endBatch();

		for (QueuedDisplay queuedDisplay : QUEUED_DISPLAYS) {
			renderTextBlock(bufferSource, poseStack, camera, queuedDisplay, TEXT_PASS_OUTLINE);
		}

		bufferSource.endBatch();

		for (QueuedDisplay queuedDisplay : QUEUED_DISPLAYS) {
			renderTextBlock(bufferSource, poseStack, camera, queuedDisplay, TEXT_PASS_COLORED_CORE);
		}

		bufferSource.endBatch();

		for (QueuedDisplay queuedDisplay : QUEUED_DISPLAYS) {
			renderTextBlock(bufferSource, poseStack, camera, queuedDisplay, TEXT_PASS_BLACK_CORE);
		}

		bufferSource.endBatch();
		QUEUED_DISPLAYS.clear();
	}

	private static void renderPanel(
			MultiBufferSource bufferSource,
			PoseStack poseStack,
			Vec3 camera,
			QueuedDisplay queuedDisplay
	) {
		poseStack.pushPose();
		poseStack.translate(
				queuedDisplay.blockPos().getX() - camera.x,
				queuedDisplay.blockPos().getY() - camera.y,
				queuedDisplay.blockPos().getZ() - camera.z
		);
		applyPanelTransform(poseStack, queuedDisplay.facing(), queuedDisplay.ceiling(), queuedDisplay.backSide(), queuedDisplay.boardHeight());

		float left = -queuedDisplay.boardWidth() / 2.0F;
		float right = queuedDisplay.boardWidth() / 2.0F;
		float bottom = 0.0F;
		float top = queuedDisplay.boardHeight();

		VertexConsumer vertices = bufferSource.getBuffer(RenderTypes.entityTranslucentEmissive(HOLOGRAM_TEXTURE));
		PoseStack.Pose pose = poseStack.last();

		submitTexturedVertex(vertices, pose, left, bottom, PANEL_Z, 0.0F, 1.0F, HOLOGRAM_PANEL_COLOR);
		submitTexturedVertex(vertices, pose, right, bottom, PANEL_Z, 1.0F, 1.0F, HOLOGRAM_PANEL_COLOR);
		submitTexturedVertex(vertices, pose, right, top, PANEL_Z, 1.0F, 0.0F, HOLOGRAM_PANEL_COLOR);
		submitTexturedVertex(vertices, pose, left, top, PANEL_Z, 0.0F, 0.0F, HOLOGRAM_PANEL_COLOR);

		poseStack.popPose();
	}

	private static void renderDisplayItem(
			MultiBufferSource.BufferSource bufferSource,
			PoseStack poseStack,
			Vec3 camera,
			QueuedDisplay queuedDisplay
	) {
		if (queuedDisplay.displayStack().isEmpty() || queuedDisplay.itemRowHeight() <= 0.0F) {
			return;
		}

		boolean flatBlockProjection = queuedDisplay.displayStack().getItem() instanceof BlockItem;
		renderDisplayItemModel(bufferSource, poseStack, camera, queuedDisplay, flatBlockProjection);
	}

	private static void renderDisplayItemModel(
			MultiBufferSource.BufferSource bufferSource,
			PoseStack poseStack,
			Vec3 camera,
			QueuedDisplay queuedDisplay,
			boolean flatBlockProjection
	) {
		if (itemModelResolver == null) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		Entity renderEntity = minecraft.player;
		if (renderEntity == null || minecraft.level == null) {
			return;
		}

		float itemVisualSize = queuedDisplay.itemRowHeight();
		float itemCenterY = queuedDisplay.boardHeight() - queuedDisplay.verticalPadding() - itemVisualSize / 2.0F;

		poseStack.pushPose();
		poseStack.translate(
				queuedDisplay.blockPos().getX() - camera.x,
				queuedDisplay.blockPos().getY() - camera.y,
				queuedDisplay.blockPos().getZ() - camera.z
		);
		applyPanelTransform(poseStack, queuedDisplay.facing(), queuedDisplay.ceiling(), queuedDisplay.backSide(), queuedDisplay.boardHeight());
		poseStack.translate(0.0F, itemCenterY, ITEM_Z);

		if (flatBlockProjection) {
			// Rotate the block model before flattening so the captured projection uses the
			// intended model viewpoint, then rotate the final flattened image in-plane to fix
			// the upside-down result without reintroducing depth.
			poseStack.scale(itemVisualSize, itemVisualSize, 0.0001F);
			poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
			poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
		} else {
			poseStack.scale(itemVisualSize, itemVisualSize, itemVisualSize);
		}

		ItemStackRenderState itemRenderState = new ItemStackRenderState();
		itemModelResolver.updateForNonLiving(
				itemRenderState,
				queuedDisplay.displayStack(),
				flatBlockProjection ? ItemDisplayContext.GUI : ItemDisplayContext.FIXED,
				renderEntity
		);
		itemRenderState.submit(poseStack, ITEM_SUBMIT_NODES, LIGHT_FULL_BRIGHT, OVERLAY_NONE, 0);

		FeatureRenderDispatcher dispatcher = new FeatureRenderDispatcher(
				ITEM_SUBMIT_NODES,
				minecraft.getModelManager(),
				bufferSource,
				minecraft.getAtlasManager(),
				minecraft.renderBuffers().outlineBufferSource(),
				bufferSource,
				minecraft.font,
				minecraft.gameRenderer.getGameRenderState()
		);
		dispatcher.renderAllFeatures();
		ITEM_SUBMIT_NODES.endFrame();
		poseStack.popPose();
	}


	private static void renderTextBlock(
			MultiBufferSource bufferSource,
			PoseStack poseStack,
			Vec3 camera,
			QueuedDisplay queuedDisplay,
			int textPass
	) {
		if (queuedDisplay.lines().isEmpty()) {
			return;
		}

		poseStack.pushPose();
		poseStack.translate(
				queuedDisplay.blockPos().getX() - camera.x,
				queuedDisplay.blockPos().getY() - camera.y,
				queuedDisplay.blockPos().getZ() - camera.z
		);
		applyPanelTransform(poseStack, queuedDisplay.facing(), queuedDisplay.ceiling(), queuedDisplay.backSide(), queuedDisplay.boardHeight());

		VertexConsumer vertices = bufferSource.getBuffer(textPass == TEXT_PASS_BLACK_CORE
				? RenderTypes.entityCutout(TEXT_PIXEL_TEXTURE)
				: RenderTypes.entityTranslucentEmissive(TEXT_PIXEL_TEXTURE));
		float y = queuedDisplay.boardHeight()
				- queuedDisplay.verticalPadding()
				- queuedDisplay.itemRowHeight()
				- queuedDisplay.imageBottomPadding();

		for (TextLine line : queuedDisplay.lines()) {
			float pixelSize = getPixelSize(queuedDisplay.boardWidth(), line.size(), line.text());
			float lineHeight = pixelSize * LINE_HEIGHT_UNITS;
			float lineWidth = getLineWidth(line.text(), pixelSize);

			y -= lineHeight;

			int textColor = getTextColor(line.color());
			boolean blackText = isBlackTextColor(textColor);
			int coreColor = getCoreTextColor(textColor);
			int outlineColor = getAdaptiveOutlineColor(textColor);
			float left = -lineWidth / 2.0F;
			float top = y + lineHeight;

			if (textPass == TEXT_PASS_OUTLINE) {
				renderPixelText(vertices, poseStack.last(), line.text(), left, top, pixelSize, outlineColor, true);
			} else if (textPass == TEXT_PASS_COLORED_CORE && !blackText) {
				renderPixelText(vertices, poseStack.last(), line.text(), left, top, pixelSize, coreColor, false);
			} else if (textPass == TEXT_PASS_BLACK_CORE && blackText) {
				renderPixelText(vertices, poseStack.last(), line.text(), left, top, pixelSize, textColor, false);
			}

			y -= queuedDisplay.lineSpacing();
		}

		poseStack.popPose();
	}

	private static void renderPixelText(
			VertexConsumer vertices,
			PoseStack.Pose pose,
			String text,
			float left,
			float top,
			float pixelSize,
			int color,
			boolean outline
	) {
		if (outline) {
			float outlineOffset = pixelSize * OUTLINE_OFFSET_UNITS;
			for (int[] offset : OUTLINE_OFFSETS) {
				renderPixelTextAt(vertices, pose, text, left + offset[0] * outlineOffset, top + offset[1] * outlineOffset, pixelSize, color);
			}
			return;
		}

		renderPixelTextAt(vertices, pose, text, left, top, pixelSize, color);
	}

	private static void renderPixelTextAt(
			VertexConsumer vertices,
			PoseStack.Pose pose,
			String text,
			float left,
			float top,
			float pixelSize,
			int color
	) {
		float cursor = left;

		for (int index = 0; index < text.length(); index++) {
			String[] glyph = getGlyph(text.charAt(index));
			int glyphWidth = getGlyphWidth(glyph);

			for (int row = 0; row < glyph.length; row++) {
				String glyphRow = glyph[row];
				for (int column = 0; column < glyphRow.length(); column++) {
					if (glyphRow.charAt(column) != '1') {
						continue;
					}

					float logicalX0 = cursor + column * pixelSize;
					float logicalX1 = logicalX0 + pixelSize;
					float x0 = -logicalX1;
					float x1 = -logicalX0;
					float y1 = top - row * pixelSize;
					float y0 = y1 - pixelSize;
					submitTexturedVertex(vertices, pose, x0, y0, TEXT_Z, 0.0F, 1.0F, color);
					submitTexturedVertex(vertices, pose, x1, y0, TEXT_Z, 1.0F, 1.0F, color);
					submitTexturedVertex(vertices, pose, x1, y1, TEXT_Z, 1.0F, 0.0F, color);
					submitTexturedVertex(vertices, pose, x0, y1, TEXT_Z, 0.0F, 0.0F, color);
				}
			}

			cursor += (glyphWidth + getGlyphGapUnits(text, index)) * pixelSize;
		}
	}

	private static float getGlyphGapUnits(String text, int index) {
		if (text == null || index + 1 >= text.length()) {
			return 0.0F;
		}

		return isTilingGlyph(text.charAt(index)) && isTilingGlyph(text.charAt(index + 1)) ? 0.0F : GLYPH_GAP_UNITS;
	}

	private static boolean isTilingGlyph(char value) {
		return switch (value) {
			case '\u2500', '\u2502', '\u250C', '\u2510', '\u2514', '\u2518', '\u251C', '\u2524', '\u252C', '\u2534', '\u253C',
					'\u2550', '\u2551', '\u2554', '\u2557', '\u255A', '\u255D', '\u2560', '\u2563', '\u2566', '\u2569', '\u256C',
					'\u256D', '\u256E', '\u2570', '\u256F', '\u2591', '\u2592', '\u2593', '\u2588' -> true;
			default -> false;
		};
	}

	private static void submitTexturedVertex(
			VertexConsumer vertices,
			PoseStack.Pose pose,
			float x,
			float y,
			float z,
			float u,
			float v,
			int color
	) {
		vertices.addVertex(pose.pose(), x, y, z)
				.setColor(color)
				.setUv(u, v)
				.setOverlay(OVERLAY_NONE)
				.setLight(LIGHT_FULL_BRIGHT)
				.setNormal(pose, 0.0F, 0.0F, -1.0F);
	}

	private static float getPixelSize(float boardWidth, int lineSize) {
		return getPixelSize(boardWidth, lineSize, "");
	}

	private static float getPixelSize(float boardWidth, int lineSize, String text) {
		float usableWidth = boardWidth - boardWidth * PANEL_HORIZONTAL_PADDING_RATIO * 2.0F;
		float largePixelSize = usableWidth / MAX_LINE_UNITS;

		return switch (VendorHoloDisplayBlockEntity.normalizeFontSize(lineSize)) {
			case VendorHoloDisplayBlockEntity.FONT_SIZE_SMALL -> largePixelSize * SMALL_FONT_SCALE_RATIO;
			case VendorHoloDisplayBlockEntity.FONT_SIZE_LARGE -> largePixelSize;
			case VendorHoloDisplayBlockEntity.FONT_SIZE_AUTO -> getAutoPixelSize(usableWidth, largePixelSize, text);
			default -> largePixelSize * MEDIUM_FONT_SCALE_RATIO;
		};
	}

	private static float getAutoPixelSize(float usableWidth, float fallbackPixelSize, String text) {
		float lineWidthUnits = getLineWidthUnits(text);
		if (lineWidthUnits <= 0.0F) {
			return fallbackPixelSize;
		}

		return usableWidth / lineWidthUnits;
	}

	private static float getLineWidth(String text, float pixelSize) {
		return getLineWidthUnits(text) * pixelSize;
	}

	private static float getLineWidthUnits(String text) {
		if (text == null || text.isEmpty()) {
			return 0.0F;
		}

		float width = 0.0F;

		for (int index = 0; index < text.length(); index++) {
			width += getGlyphWidth(getGlyph(text.charAt(index)));
			if (index + 1 < text.length()) {
				width += getGlyphGapUnits(text, index);
			}
		}

		return width;
	}

	private static int getGlyphWidth(String[] glyph) {
		int width = 0;

		for (String row : glyph) {
			width = Math.max(width, row.length());
		}

		return Math.max(1, width);
	}

	private static String[] getGlyph(char value) {
		return switch (value) {
			case 'A' -> new String[] {"01110", "10001", "10001", "11111", "10001", "10001", "10001"};
			case 'B' -> new String[] {"11110", "10001", "10001", "11110", "10001", "10001", "11110"};
			case 'C' -> new String[] {"01111", "10000", "10000", "10000", "10000", "10000", "01111"};
			case 'D' -> new String[] {"11110", "10001", "10001", "10001", "10001", "10001", "11110"};
			case 'E' -> new String[] {"11111", "10000", "10000", "11110", "10000", "10000", "11111"};
			case 'F' -> new String[] {"11111", "10000", "10000", "11110", "10000", "10000", "10000"};
			case 'G' -> new String[] {"01111", "10000", "10000", "10011", "10001", "10001", "01111"};
			case 'H' -> new String[] {"10001", "10001", "10001", "11111", "10001", "10001", "10001"};
			case 'I' -> new String[] {"11111", "00100", "00100", "00100", "00100", "00100", "11111"};
			case 'J' -> new String[] {"00111", "00010", "00010", "00010", "10010", "10010", "01100"};
			case 'K' -> new String[] {"10001", "10010", "10100", "11000", "10100", "10010", "10001"};
			case 'L' -> new String[] {"10000", "10000", "10000", "10000", "10000", "10000", "11111"};
			case 'M' -> new String[] {"10001", "11011", "10101", "10101", "10001", "10001", "10001"};
			case 'N' -> new String[] {"10001", "11001", "10101", "10011", "10001", "10001", "10001"};
			case 'O' -> new String[] {"01110", "10001", "10001", "10001", "10001", "10001", "01110"};
			case 'P' -> new String[] {"11110", "10001", "10001", "11110", "10000", "10000", "10000"};
			case 'Q' -> new String[] {"01110", "10001", "10001", "10001", "10101", "10010", "01101"};
			case 'R' -> new String[] {"11110", "10001", "10001", "11110", "10100", "10010", "10001"};
			case 'S' -> new String[] {"01111", "10000", "10000", "01110", "00001", "00001", "11110"};
			case 'T' -> new String[] {"11111", "00100", "00100", "00100", "00100", "00100", "00100"};
			case 'U' -> new String[] {"10001", "10001", "10001", "10001", "10001", "10001", "01110"};
			case 'V' -> new String[] {"10001", "10001", "10001", "10001", "10001", "01010", "00100"};
			case 'W' -> new String[] {"10001", "10001", "10001", "10101", "10101", "10101", "01010"};
			case 'X' -> new String[] {"10001", "10001", "01010", "00100", "01010", "10001", "10001"};
			case 'Y' -> new String[] {"10001", "10001", "01010", "00100", "00100", "00100", "00100"};
			case 'Z' -> new String[] {"11111", "00001", "00010", "00100", "01000", "10000", "11111"};
			case 'a' -> new String[] {"00000", "00000", "01110", "00001", "01111", "10001", "01111"};
			case 'b' -> new String[] {"10000", "10000", "11110", "10001", "10001", "10001", "11110"};
			case 'c' -> new String[] {"00000", "00000", "01111", "10000", "10000", "10000", "01111"};
			case 'd' -> new String[] {"00001", "00001", "01111", "10001", "10001", "10001", "01111"};
			case 'e' -> new String[] {"00000", "00000", "01110", "10001", "11111", "10000", "01111"};
			case 'f' -> new String[] {"00110", "01000", "01000", "11110", "01000", "01000", "01000"};
			case 'g' -> new String[] {"00000", "01111", "10001", "10001", "01111", "00001", "01110"};
			case 'h' -> new String[] {"10000", "10000", "11110", "10001", "10001", "10001", "10001"};
			case 'i' -> new String[] {"1", "0", "1", "1", "1", "1", "1"};
			case 'j' -> new String[] {"00010", "00000", "00010", "00010", "00010", "10010", "01100"};
			case 'k' -> new String[] {"10000", "10000", "10010", "10100", "11000", "10100", "10010"};
			case 'l' -> new String[] {"1", "1", "1", "1", "1", "1", "1"};
			case 'm' -> new String[] {"00000", "00000", "11010", "10101", "10101", "10101", "10101"};
			case 'n' -> new String[] {"00000", "00000", "11110", "10001", "10001", "10001", "10001"};
			case 'o' -> new String[] {"00000", "00000", "01110", "10001", "10001", "10001", "01110"};
			case 'p' -> new String[] {"00000", "11110", "10001", "10001", "11110", "10000", "10000"};
			case 'q' -> new String[] {"00000", "01111", "10001", "10001", "01111", "00001", "00001"};
			case 'r' -> new String[] {"00000", "00000", "10110", "11001", "10000", "10000", "10000"};
			case 's' -> new String[] {"00000", "00000", "01111", "10000", "01110", "00001", "11110"};
			case 't' -> new String[] {"01000", "01000", "11110", "01000", "01000", "01000", "00110"};
			case 'u' -> new String[] {"00000", "00000", "10001", "10001", "10001", "10011", "01101"};
			case 'v' -> new String[] {"00000", "00000", "10001", "10001", "10001", "01010", "00100"};
			case 'w' -> new String[] {"00000", "00000", "10001", "10001", "10101", "10101", "01010"};
			case 'x' -> new String[] {"00000", "00000", "10001", "01010", "00100", "01010", "10001"};
			case 'y' -> new String[] {"00000", "10001", "10001", "10001", "01111", "00001", "01110"};
			case 'z' -> new String[] {"00000", "00000", "11111", "00010", "00100", "01000", "11111"};
			case '0' -> new String[] {"01110", "10001", "10011", "10101", "11001", "10001", "01110"};
			case '1' -> new String[] {"00100", "01100", "00100", "00100", "00100", "00100", "01110"};
			case '2' -> new String[] {"01110", "10001", "00001", "00010", "00100", "01000", "11111"};
			case '3' -> new String[] {"11110", "00001", "00001", "01110", "00001", "00001", "11110"};
			case '4' -> new String[] {"00010", "00110", "01010", "10010", "11111", "00010", "00010"};
			case '5' -> new String[] {"11111", "10000", "10000", "11110", "00001", "00001", "11110"};
			case '6' -> new String[] {"01110", "10000", "10000", "11110", "10001", "10001", "01110"};
			case '7' -> new String[] {"11111", "00001", "00010", "00100", "01000", "01000", "01000"};
			case '8' -> new String[] {"01110", "10001", "10001", "01110", "10001", "10001", "01110"};
			case '9' -> new String[] {"01110", "10001", "10001", "01111", "00001", "00001", "01110"};
			case ' ', '\u00A0' -> new String[] {"000", "000", "000", "000", "000", "000", "000"};
			case '-' -> new String[] {"00000", "00000", "00000", "11111", "00000", "00000", "00000"};
			case '_' -> new String[] {"00000", "00000", "00000", "00000", "00000", "00000", "11111"};
			case '.' -> new String[] {"0", "0", "0", "0", "0", "0", "1"};
			case ',' -> new String[] {"00", "00", "00", "00", "00", "01", "10"};
			case ':' -> new String[] {"0", "1", "0", "0", "0", "1", "0"};
			case '!' -> new String[] {"1", "1", "1", "1", "1", "0", "1"};
			case '?' -> new String[] {"11110", "00001", "00001", "00110", "00100", "00000", "00100"};
			case '#' -> new String[] {"01010", "11111", "01010", "01010", "11111", "01010", "01010"};
			case '\'' -> new String[] {"1", "1", "0", "0", "0", "0", "0"};
			case '"' -> new String[] {"101", "101", "000", "000", "000", "000", "000"};
			case '/' -> new String[] {"00001", "00010", "00010", "00100", "01000", "01000", "10000"};
			case '\\' -> new String[] {"10000", "01000", "01000", "00100", "00010", "00010", "00001"};
			case ';' -> new String[] {"00", "01", "00", "00", "00", "01", "10"};
			case '`', '\u2018', '\u2019', '\u201B' -> new String[] {"10", "01", "00", "00", "00", "00", "00"};
			case '\u201A' -> new String[] {"00", "00", "00", "00", "00", "01", "10"};
			case '\u201C', '\u201D', '\u201E', '\u201F' -> new String[] {"101", "101", "000", "000", "000", "000", "000"};
			case '\u00AB' -> new String[] {"00101", "01010", "10100", "01010", "00101", "00000", "00000"};
			case '\u00BB' -> new String[] {"10100", "01010", "00101", "01010", "10100", "00000", "00000"};
			case '(', '[' -> new String[] {"001", "010", "100", "100", "100", "010", "001"};
			case ')', ']' -> new String[] {"100", "010", "001", "001", "001", "010", "100"};
			case '{' -> new String[] {"0011", "0100", "0100", "1000", "0100", "0100", "0011"};
			case '}' -> new String[] {"1100", "0010", "0010", "0001", "0010", "0010", "1100"};
			case '<' -> new String[] {"0001", "0010", "0100", "1000", "0100", "0010", "0001"};
			case '>' -> new String[] {"1000", "0100", "0010", "0001", "0010", "0100", "1000"};
			case '+' -> new String[] {"00000", "00100", "00100", "11111", "00100", "00100", "00000"};
			case '=' -> new String[] {"00000", "00000", "11111", "00000", "11111", "00000", "00000"};
			case '*' -> new String[] {"00000", "10101", "01110", "11111", "01110", "10101", "00000"};
			case '$' -> new String[] {"00100", "01111", "10100", "01110", "00101", "11110", "00100"};
			case '%' -> new String[] {"11001", "11010", "00100", "01000", "10110", "00110", "00000"};
			case '&' -> new String[] {"01100", "10010", "10100", "01000", "10101", "10010", "01101"};
			case '@' -> new String[] {"01110", "10001", "10111", "10101", "10111", "10000", "01111"};
			case '|' -> new String[] {"1", "1", "1", "1", "1", "1", "1"};
			case '^' -> new String[] {"00100", "01010", "10001", "00000", "00000", "00000", "00000"};
			case '~' -> new String[] {"00000", "00000", "01001", "10110", "00000", "00000", "00000"};
			case '\u00A7' -> new String[] {"01111", "10000", "01110", "10001", "01110", "00001", "11110"};
			case '\u00B0' -> new String[] {"0110", "1001", "1001", "0110", "0000", "0000", "0000"};
			case '\u00B1' -> new String[] {"00100", "00100", "11111", "00100", "00100", "00000", "11111"};
			case '\u00D7' -> new String[] {"00000", "10001", "01010", "00100", "01010", "10001", "00000"};
			case '\u00F7' -> new String[] {"00000", "00100", "00000", "11111", "00000", "00100", "00000"};
			case '\u2022', '\u00B7' -> new String[] {"000", "000", "000", "010", "000", "000", "000"};
			case '\u2026' -> new String[] {"00000", "00000", "00000", "00000", "00000", "00000", "10101"};
			case '\u2010', '\u2011', '\u2012', '\u2013', '\u2014', '\u2212' -> new String[] {"00000", "00000", "00000", "11111", "00000", "00000", "00000"};
			case '\u2116' -> new String[] {"10001", "11001", "10101", "10011", "10001", "00000", "11111"};
			case '\u2190' -> new String[] {"00100", "01000", "11111", "01000", "00100", "00000", "00000"};
			case '\u2191' -> new String[] {"00100", "01110", "10101", "00100", "00100", "00100", "00100"};
			case '\u2192' -> new String[] {"00100", "00010", "11111", "00010", "00100", "00000", "00000"};
			case '\u2193' -> new String[] {"00100", "00100", "00100", "00100", "10101", "01110", "00100"};
			case '\u26A0' -> new String[] {"0001000", "0010100", "0010100", "0100010", "0101010", "1000001", "1111111"};
			case '\u24D8', '\u24BE' -> new String[] {"0011100", "0100010", "1001001", "1000001", "1001001", "1001001", "0111110"};
			case '\u2665' -> new String[] {"0000000", "0110110", "1001001", "1000001", "0100010", "0010100", "0001000"};
			case '\u2500' -> new String[] {"00000", "00000", "00000", "11111", "00000", "00000", "00000"};
			case '\u2502' -> new String[] {"00100", "00100", "00100", "00100", "00100", "00100", "00100"};
			case '\u250C' -> new String[] {"00000", "00000", "00000", "00111", "00100", "00100", "00100"};
			case '\u2510' -> new String[] {"00000", "00000", "00000", "11100", "00100", "00100", "00100"};
			case '\u2514' -> new String[] {"00100", "00100", "00100", "00111", "00000", "00000", "00000"};
			case '\u2518' -> new String[] {"00100", "00100", "00100", "11100", "00000", "00000", "00000"};
			case '\u251C' -> new String[] {"00100", "00100", "00100", "00111", "00100", "00100", "00100"};
			case '\u2524' -> new String[] {"00100", "00100", "00100", "11100", "00100", "00100", "00100"};
			case '\u252C' -> new String[] {"00000", "00000", "00000", "11111", "00100", "00100", "00100"};
			case '\u2534' -> new String[] {"00100", "00100", "00100", "11111", "00000", "00000", "00000"};
			case '\u253C' -> new String[] {"00100", "00100", "00100", "11111", "00100", "00100", "00100"};
			case '\u2550' -> new String[] {"00000", "00000", "11111", "00000", "11111", "00000", "00000"};
			case '\u2551' -> new String[] {"01010", "01010", "01010", "01010", "01010", "01010", "01010"};
			case '\u2554' -> new String[] {"00000", "00000", "01111", "00010", "01011", "01010", "01010"};
			case '\u2557' -> new String[] {"00000", "00000", "11110", "01000", "11010", "01010", "01010"};
			case '\u255A' -> new String[] {"01010", "01010", "01011", "00010", "01111", "00000", "00000"};
			case '\u255D' -> new String[] {"01010", "01010", "11010", "01000", "11110", "00000", "00000"};
			case '\u2560' -> new String[] {"01010", "01010", "01011", "00010", "01011", "01010", "01010"};
			case '\u2563' -> new String[] {"01010", "01010", "11010", "01000", "11010", "01010", "01010"};
			case '\u2566' -> new String[] {"00000", "00000", "11111", "00000", "11111", "01010", "01010"};
			case '\u2569' -> new String[] {"01010", "01010", "11111", "00000", "11111", "00000", "00000"};
			case '\u256C' -> new String[] {"01010", "01010", "11111", "00000", "11111", "01010", "01010"};
			case '\u2591' -> new String[] {"10010", "00100", "01001", "10010", "00100", "01001", "10010"};
			case '\u2592' -> new String[] {"10101", "01010", "10101", "01010", "10101", "01010", "10101"};
			case '\u2593' -> new String[] {"11101", "10111", "11101", "10111", "11101", "10111", "11101"};
			case '\u2588' -> new String[] {"11111", "11111", "11111", "11111", "11111", "11111", "11111"};
			case '\u256D' -> new String[] {"00000", "00000", "00011", "00100", "00100", "00100", "00100"};
			case '\u256E' -> new String[] {"00000", "00000", "11000", "00100", "00100", "00100", "00100"};
			case '\u2570' -> new String[] {"00100", "00100", "00100", "00100", "00011", "00000", "00000"};
			case '\u256F' -> new String[] {"00100", "00100", "00100", "00100", "11000", "00000", "00000"};
			default -> new String[] {"11110", "00001", "00001", "00110", "00100", "00000", "00100"};
		};
	}

	private static int getTextColor(int color) {
		return normalizeArgb(color);
	}

	private static boolean isBlackTextColor(int textColor) {
		int red = (textColor >> 16) & 0xFF;
		int green = (textColor >> 8) & 0xFF;
		int blue = textColor & 0xFF;
		return red < 32 && green < 32 && blue < 32;
	}

	private static int getCoreTextColor(int textColor) {
		int red = (textColor >> 16) & 0xFF;
		int green = (textColor >> 8) & 0xFF;
		int blue = textColor & 0xFF;

		if (red > 224 && green > 224 && blue > 224) {
			return 0xFFFFFFFF;
		}

		int maxChannel = Math.max(red, Math.max(green, blue));
		if (maxChannel <= COLORED_CORE_MAX_CHANNEL || maxChannel <= 0) {
			return textColor;
		}

		float scale = COLORED_CORE_MAX_CHANNEL / (float) maxChannel;
		int adjustedRed = Math.max(0, Math.min(255, Math.round(red * scale)));
		int adjustedGreen = Math.max(0, Math.min(255, Math.round(green * scale)));
		int adjustedBlue = Math.max(0, Math.min(255, Math.round(blue * scale)));

		return 0xFF000000 | (adjustedRed << 16) | (adjustedGreen << 8) | adjustedBlue;
	}

	private static int getAdaptiveOutlineColor(int textColor) {
		int red = (textColor >> 16) & 0xFF;
		int green = (textColor >> 8) & 0xFF;
		int blue = textColor & 0xFF;

		if (isBlackTextColor(textColor)) {
			return BLACK_TEXT_OUTLINE_COLOR;
		}

		int outlineRed = Math.max(24, Math.round(red * OUTLINE_DARKEN_FACTOR));
		int outlineGreen = Math.max(24, Math.round(green * OUTLINE_DARKEN_FACTOR));
		int outlineBlue = Math.max(24, Math.round(blue * OUTLINE_DARKEN_FACTOR));

		return 0xFF000000 | (outlineRed << 16) | (outlineGreen << 8) | outlineBlue;
	}

	private static int normalizeArgb(int color) {
		return (color >>> 24) == 0 ? (color | 0xFF000000) : color;
	}

	private static void applyPanelTransform(PoseStack poseStack, Direction facing, boolean ceiling, boolean backSide, float boardHeight) {
		float panelBottomY = ceiling ? PANEL_CEILING_TOP_Y - boardHeight : PANEL_BOTTOM_Y;
		poseStack.translate(BLOCK_CENTER, panelBottomY, BLOCK_CENTER);
		poseStack.mulPose(Axis.YP.rotationDegrees(getFacingRotationDegrees(facing)));
		poseStack.translate(0.0F, 0.0F, -PANEL_FORWARD_OFFSET);

		if (backSide) {
			poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
		}
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

	private record LayoutMetrics(
			float verticalPadding,
			float lineSpacing,
			float itemRowHeight,
			float imageBottomPadding,
			float boardHeight
	) {
	}

	private record QueuedDisplay(
			BlockPos blockPos,
			Direction facing,
			boolean ceiling,
			boolean backSide,
			float boardWidth,
			float boardHeight,
			float verticalPadding,
			float lineSpacing,
			float itemRowHeight,
			float imageBottomPadding,
			ItemStack displayStack,
			List<TextLine> lines
	) {
	}

	private record TextLine(String text, int size, int color) {
	}
}
