package domestia_vendor_choice.client;

import domestia_vendor_choice.DomestiaVendorChoice;
import domestia_vendor_choice.VendorHoloDisplayBlockEntity;
import domestia_vendor_choice.VendorHoloDisplayMenu;
import domestia_vendor_choice.VendorHoloDisplayOpenPayload;
import domestia_vendor_choice.VendorHoloDisplaySavePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class VendorHoloDisplayScreen extends AbstractContainerScreen<VendorHoloDisplayMenu> {
	private static VendorHoloDisplayOpenPayload pendingInitialData;
	private static final int SIZE_SCREEN_WIDTH = 200;
	private static final int SIZE_SCREEN_HEIGHT = 245;
	private static final int SIZE_TEXTURE_WIDTH = 200;
	private static final int SIZE_TEXTURE_HEIGHT = 245;

	private static final int COLOR_TEXT = 0xFF404040;

	private static final int POS_INVENTORY_LABEL_X = 19;
	private static final int POS_INVENTORY_LABEL_Y = 152;
	private static final int POS_IMAGE_LABEL_X = 19;
	private static final int POS_IMAGE_LABEL_Y = 26;
	private static final int POS_BOARD_SIZE_LABEL_X = 42;
	private static final int POS_BOARD_SIZE_LABEL_Y = 26;
	private static final int POS_DYE_LABEL_X = 127;
	private static final int POS_DYE_LABEL_Y = 48;
	private static final int POS_ROW_SIZE_LABEL_X = 151;
	private static final int POS_ROW_SIZE_LABEL_Y = 48;

	private static final int POS_TEXT_FIELD_X = 28;
	private static final int POS_TEXT_FIELD_WIDTH = 104;
	private static final int POS_TEXT_FIELD_HEIGHT = 12;
	private static final int POS_TEXT_FIELD_PADDING_X = 0;
	private static final int POS_TEXT_FIELD_PADDING_Y = 0;
	private static final int POS_FIRST_ROW_Y = 65;
	private static final int POS_ROW_STEP = 18;

	private static final int POS_LINE_SIZE_SLIDER_X = 150;
	private static final int POS_LINE_SIZE_SLIDER_FIRST_Y = 62;
	private static final int POS_LINE_SIZE_SLIDER_STEP = 18;
	private static final int POS_LINE_SIZE_SLIDER_WIDTH = 42;
	private static final int POS_LINE_SIZE_SLIDER_HEIGHT = 12;
	private static final int LINE_SIZE_SLIDER_STEPS = 3;

	private static final int POS_BOARD_SLIDER_X = 40;
	private static final int POS_BOARD_SLIDER_Y = 38;
	private static final int POS_BOARD_SLIDER_WIDTH = 65;
	private static final int POS_BOARD_SLIDER_HEIGHT = 16;
	private static final int BOARD_SIZE_SLIDER_STEPS = (VendorHoloDisplayBlockEntity.BOARD_SIZE_MAX - VendorHoloDisplayBlockEntity.BOARD_SIZE_MIN)
			/ VendorHoloDisplayBlockEntity.BOARD_SIZE_STEP;

	private static final int POS_SAVE_BUTTON_X = 114;
	private static final int POS_CANCEL_BUTTON_X = 154;
	private static final int POS_ACTION_BUTTON_Y = 26;
	private static final int POS_ACTION_BUTTON_WIDTH = 38;
	private static final int POS_ACTION_BUTTON_HEIGHT = 14;

	private static final String ID_TITLE = "screen.domestia_vendor_choice.vendor_holo_display_control";
	private static final String ID_LABEL_IMAGE = "screen.domestia_vendor_choice.vendor_holo_display_control.image";
	private static final String ID_LABEL_BOARD_SIZE = "screen.domestia_vendor_choice.vendor_holo_display_control.board_size";
	private static final String ID_LABEL_ROW_SIZE = "screen.domestia_vendor_choice.vendor_holo_display_control.row_size";
	private static final String ID_LABEL_DYE = "screen.domestia_vendor_choice.vendor_holo_display_control.dye";
	private static final String ID_TEXT_LINE = "screen.domestia_vendor_choice.vendor_holo_display_control.line";
	private static final String ID_BUTTON_FONT_SMALL = "screen.domestia_vendor_choice.vendor_holo_display_control.font_size.small";
	private static final String ID_BUTTON_FONT_NORMAL = "screen.domestia_vendor_choice.vendor_holo_display_control.font_size.normal";
	private static final String ID_BUTTON_FONT_LARGE = "screen.domestia_vendor_choice.vendor_holo_display_control.font_size.large";
	private static final String ID_BUTTON_FONT_AUTO = "screen.domestia_vendor_choice.vendor_holo_display_control.font_size.auto";
	private static final String ID_BUTTON_SAVE = "screen.domestia_vendor_choice.vendor_holo_display_control.save";
	private static final String ID_BUTTON_CANCEL = "screen.domestia_vendor_choice.vendor_holo_display_control.cancel";

	private static final Identifier TEXTURE_VENDOR_HOLO_DISPLAY_CONTROL = Identifier.fromNamespaceAndPath(
			DomestiaVendorChoice.MOD_ID,
			"textures/gui/vendor_holo_display_control.png"
	);

	private final String[] lines = new String[VendorHoloDisplayBlockEntity.LINE_COUNT];
	private final int[] lineSizes = new int[VendorHoloDisplayBlockEntity.LINE_COUNT];
	private final EditBox[] lineFields = new EditBox[VendorHoloDisplayBlockEntity.LINE_COUNT];
	private final LineSizeSlider[] lineSizeSliders = new LineSizeSlider[VendorHoloDisplayBlockEntity.LINE_COUNT];
	private BoardSizeSlider boardSizeSlider;
	private int boardSize;
	private boolean settingsCommitted;
	private BlockPos blockPos = BlockPos.ZERO;
	private boolean receivedInitialData;

	public static void storePendingInitialData(VendorHoloDisplayOpenPayload payload) {
		pendingInitialData = payload;
	}

	public VendorHoloDisplayScreen(VendorHoloDisplayMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, Component.translatable(ID_TITLE), SIZE_SCREEN_WIDTH, SIZE_SCREEN_HEIGHT);

		String[] initialLines = VendorHoloDisplayBlockEntity.unpackLines(menu.getPackedLines());
		int[] initialLineSizes = VendorHoloDisplayBlockEntity.unpackLineSizes(menu.getPackedLineSizes());

		for (int index = 0; index < VendorHoloDisplayBlockEntity.LINE_COUNT; index++) {
			this.lines[index] = initialLines[index];
			this.lineSizes[index] = initialLineSizes[index];
		}

		this.boardSize = VendorHoloDisplayBlockEntity.normalizeBoardSize(menu.getBoardSize());
		this.titleLabelX = 0;
		this.titleLabelY = 0;
		this.inventoryLabelX = POS_INVENTORY_LABEL_X;
		this.inventoryLabelY = POS_INVENTORY_LABEL_Y;

		if (pendingInitialData != null) {
			this.applyInitialData(pendingInitialData);
			pendingInitialData = null;
		}
	}

	public void applyInitialData(VendorHoloDisplayOpenPayload payload) {
		this.blockPos = payload.pos();
		this.receivedInitialData = true;

		String[] payloadLines = VendorHoloDisplayBlockEntity.unpackLines(payload.lines());
		int[] payloadLineSizes = VendorHoloDisplayBlockEntity.unpackLineSizes(payload.lineSizes());

		for (int index = 0; index < VendorHoloDisplayBlockEntity.LINE_COUNT; index++) {
			this.lines[index] = payloadLines[index];
			this.lineSizes[index] = payloadLineSizes[index];
		}

		this.boardSize = VendorHoloDisplayBlockEntity.normalizeBoardSize(payload.boardSize());
		this.updateLineFieldsIfReady();
		this.updateSelectorButtonsIfReady();
	}

	@Override
	protected void init() {
		super.init();

		this.addLineFields();
		this.addBoardSizeSlider();
		this.addLineSizeSliders();
		this.addActionButtons();
		this.updateSelectorButtons();
	}

	private void addLineFields() {
		for (int lineIndex = 0; lineIndex < VendorHoloDisplayBlockEntity.LINE_COUNT; lineIndex++) {
			final int capturedLineIndex = lineIndex;
			EditBox field = new EditBox(
					this.font,
					this.leftPos + POS_TEXT_FIELD_X + POS_TEXT_FIELD_PADDING_X,
					this.topPos + this.getRowY(lineIndex) + POS_TEXT_FIELD_PADDING_Y,
					POS_TEXT_FIELD_WIDTH - POS_TEXT_FIELD_PADDING_X * 2,
					POS_TEXT_FIELD_HEIGHT,
					Component.translatable(ID_TEXT_LINE, lineIndex + 1)
			);

			field.setBordered(false);
			field.setMaxLength(VendorHoloDisplayBlockEntity.MAX_LINE_LENGTH);
			field.setValue(this.lines[lineIndex]);
			field.setResponder(value -> this.lines[capturedLineIndex] = VendorHoloDisplayBlockEntity.normalizeLine(value));
			this.lineFields[lineIndex] = this.addRenderableWidget(field);
		}
	}

	private void addBoardSizeSlider() {
		this.boardSizeSlider = this.addRenderableWidget(new BoardSizeSlider(
				this.leftPos + POS_BOARD_SLIDER_X,
				this.topPos + POS_BOARD_SLIDER_Y,
				POS_BOARD_SLIDER_WIDTH,
				POS_BOARD_SLIDER_HEIGHT,
				this.boardSize
		));
	}

	private void addLineSizeSliders() {
		for (int lineIndex = 0; lineIndex < VendorHoloDisplayBlockEntity.LINE_COUNT; lineIndex++) {
			this.lineSizeSliders[lineIndex] = this.addRenderableWidget(new LineSizeSlider(
					this.leftPos + POS_LINE_SIZE_SLIDER_X,
					this.topPos + this.getLineSizeSliderY(lineIndex),
					POS_LINE_SIZE_SLIDER_WIDTH,
					POS_LINE_SIZE_SLIDER_HEIGHT,
					lineIndex,
					this.lineSizes[lineIndex]
			));
		}
	}

	private void addActionButtons() {
		this.addRenderableWidget(Button.builder(
				Component.translatable(ID_BUTTON_SAVE),
				button -> this.saveAndClose()
		).bounds(this.leftPos + POS_SAVE_BUTTON_X, this.topPos + POS_ACTION_BUTTON_Y, POS_ACTION_BUTTON_WIDTH, POS_ACTION_BUTTON_HEIGHT).build());

		this.addRenderableWidget(Button.builder(
				Component.translatable(ID_BUTTON_CANCEL),
				button -> this.cancelAndClose()
		).bounds(this.leftPos + POS_CANCEL_BUTTON_X, this.topPos + POS_ACTION_BUTTON_Y, POS_ACTION_BUTTON_WIDTH, POS_ACTION_BUTTON_HEIGHT).build());
	}

	private void setBoardSize(int boardSize) {
		this.boardSize = VendorHoloDisplayBlockEntity.normalizeBoardSize(boardSize);
		this.updateSelectorButtons();
	}

	private void setLineSize(int lineIndex, int lineSize) {
		this.lineSizes[lineIndex] = VendorHoloDisplayBlockEntity.normalizeFontSize(lineSize);
		this.updateSelectorButtons();
	}

	private void updateLineFieldsIfReady() {
		if (this.lineFields[0] == null) {
			return;
		}

		for (int index = 0; index < VendorHoloDisplayBlockEntity.LINE_COUNT; index++) {
			this.lineFields[index].setValue(this.lines[index]);
		}
	}

	private void updateSelectorButtonsIfReady() {
		if (this.boardSizeSlider == null || this.lineSizeSliders[0] == null) {
			return;
		}

		this.updateSelectorButtons();
	}

	private void updateSelectorButtons() {
		if (this.boardSizeSlider != null) {
			this.boardSizeSlider.setBoardSize(this.boardSize);
		}

		for (int lineIndex = 0; lineIndex < VendorHoloDisplayBlockEntity.LINE_COUNT; lineIndex++) {
			if (this.lineSizeSliders[lineIndex] != null) {
				this.lineSizeSliders[lineIndex].setLineSize(this.lineSizes[lineIndex]);
			}
		}
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.isEscape()) {
			this.cancelAndClose();
			return true;
		}

		for (EditBox field : this.lineFields) {
			if (field != null && field.canConsumeInput()) {
				field.keyPressed(event);
				return true;
			}
		}

		return super.keyPressed(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		for (EditBox field : this.lineFields) {
			if (field != null && field.canConsumeInput()) {
				field.charTyped(event);
				return true;
			}
		}

		return super.charTyped(event);
	}

	@Override
	public void onClose() {
		this.cancelAndClose();
	}

	private void saveAndClose() {
		this.saveSettings();
		this.closeWithoutFurtherActions();
	}

	private void cancelAndClose() {
		this.closeWithoutFurtherActions();
	}

	private void closeWithoutFurtherActions() {
		super.onClose();
	}

	private void saveSettings() {
		if (this.settingsCommitted || !this.receivedInitialData) {
			return;
		}

		this.settingsCommitted = true;
		ClientPlayNetworking.send(new VendorHoloDisplaySavePayload(
				this.blockPos,
				this.getPackedLines(),
				this.getPackedLineSizes(),
				this.boardSize
		));
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		graphics.blit(
				RenderPipelines.GUI_TEXTURED,
				TEXTURE_VENDOR_HOLO_DISPLAY_CONTROL,
				this.leftPos,
				this.topPos,
				0.0F,
				0.0F,
				this.imageWidth,
				this.imageHeight,
				SIZE_TEXTURE_WIDTH,
				SIZE_TEXTURE_HEIGHT
		);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		graphics.text(this.font, Component.translatable(ID_LABEL_IMAGE), POS_IMAGE_LABEL_X, POS_IMAGE_LABEL_Y, COLOR_TEXT, false);
		graphics.text(this.font, Component.translatable(ID_LABEL_BOARD_SIZE), POS_BOARD_SIZE_LABEL_X, POS_BOARD_SIZE_LABEL_Y, COLOR_TEXT, false);
		graphics.text(this.font, Component.translatable(ID_LABEL_DYE), POS_DYE_LABEL_X, POS_DYE_LABEL_Y, COLOR_TEXT, false);
		graphics.text(this.font, Component.translatable(ID_LABEL_ROW_SIZE), POS_ROW_SIZE_LABEL_X, POS_ROW_SIZE_LABEL_Y, COLOR_TEXT, false);
		graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, COLOR_TEXT, false);
	}

	private int getRowY(int lineIndex) {
		return POS_FIRST_ROW_Y + lineIndex * POS_ROW_STEP;
	}

	private int getLineSizeSliderY(int lineIndex) {
		return POS_LINE_SIZE_SLIDER_FIRST_Y + lineIndex * POS_LINE_SIZE_SLIDER_STEP;
	}

	private String getPackedLines() {
		StringBuilder builder = new StringBuilder();
		for (int index = 0; index < VendorHoloDisplayBlockEntity.LINE_COUNT; index++) {
			if (index > 0) {
				builder.append('\n');
			}
			builder.append(VendorHoloDisplayBlockEntity.normalizeLine(this.getLineValue(index)));
		}
		return builder.toString();
	}

	private String getLineValue(int index) {
		if (index < 0 || index >= VendorHoloDisplayBlockEntity.LINE_COUNT) {
			return "";
		}

		EditBox field = this.lineFields[index];
		return field == null ? this.lines[index] : field.getValue();
	}

	private int getPackedLineSizes() {
		int packed = 0;
		for (int index = 0; index < VendorHoloDisplayBlockEntity.LINE_COUNT; index++) {
			packed |= (VendorHoloDisplayBlockEntity.normalizeFontSize(this.lineSizes[index]) & 3) << (index * 2);
		}
		return packed;
	}
	private static double boardSizeToSliderValue(int boardSize) {
		int normalizedBoardSize = VendorHoloDisplayBlockEntity.normalizeBoardSize(boardSize);
		int index = (normalizedBoardSize - VendorHoloDisplayBlockEntity.BOARD_SIZE_MIN) / VendorHoloDisplayBlockEntity.BOARD_SIZE_STEP;
		return index / (double) BOARD_SIZE_SLIDER_STEPS;
	}

	private static int sliderValueToBoardSize(double value) {
		int index = Math.max(0, Math.min(BOARD_SIZE_SLIDER_STEPS, (int) Math.round(value * BOARD_SIZE_SLIDER_STEPS)));
		return VendorHoloDisplayBlockEntity.BOARD_SIZE_MIN + index * VendorHoloDisplayBlockEntity.BOARD_SIZE_STEP;
	}

	private static double lineSizeToSliderValue(int lineSize) {
		int normalizedLineSize = VendorHoloDisplayBlockEntity.normalizeFontSize(lineSize);
		return normalizedLineSize / (double) LINE_SIZE_SLIDER_STEPS;
	}

	private static int sliderValueToLineSize(double value) {
		return Math.max(
				VendorHoloDisplayBlockEntity.FONT_SIZE_SMALL,
				Math.min(VendorHoloDisplayBlockEntity.FONT_SIZE_AUTO, (int) Math.round(value * LINE_SIZE_SLIDER_STEPS))
		);
	}

	private static Component lineSizeMessage(int lineSize) {
		return switch (VendorHoloDisplayBlockEntity.normalizeFontSize(lineSize)) {
			case VendorHoloDisplayBlockEntity.FONT_SIZE_SMALL -> Component.translatable(ID_BUTTON_FONT_SMALL);
			case VendorHoloDisplayBlockEntity.FONT_SIZE_LARGE -> Component.translatable(ID_BUTTON_FONT_LARGE);
			case VendorHoloDisplayBlockEntity.FONT_SIZE_AUTO -> Component.translatable(ID_BUTTON_FONT_AUTO);
			default -> Component.translatable(ID_BUTTON_FONT_NORMAL);
		};
	}

	private final class BoardSizeSlider extends AbstractSliderButton {
		private BoardSizeSlider(int x, int y, int width, int height, int initialBoardSize) {
			super(x, y, width, height, Component.empty(), boardSizeToSliderValue(initialBoardSize));
			this.updateMessage();
		}

		private void setBoardSize(int boardSize) {
			this.value = boardSizeToSliderValue(boardSize);
			this.updateMessage();
		}

		@Override
		protected void updateMessage() {
			this.setMessage(Component.literal(Integer.toString(sliderValueToBoardSize(this.value))));
		}

		@Override
		protected void applyValue() {
			int selectedBoardSize = sliderValueToBoardSize(this.value);
			this.value = boardSizeToSliderValue(selectedBoardSize);
			VendorHoloDisplayScreen.this.setBoardSize(selectedBoardSize);
			this.updateMessage();
		}
	}

	private final class LineSizeSlider extends AbstractSliderButton {
		private final int lineIndex;

		private LineSizeSlider(int x, int y, int width, int height, int lineIndex, int initialLineSize) {
			super(x, y, width, height, Component.empty(), lineSizeToSliderValue(initialLineSize));
			this.lineIndex = lineIndex;
			this.updateMessage();
		}

		private void setLineSize(int lineSize) {
			this.value = lineSizeToSliderValue(lineSize);
			this.updateMessage();
		}

		@Override
		protected void updateMessage() {
			this.setMessage(lineSizeMessage(sliderValueToLineSize(this.value)));
		}

		@Override
		protected void applyValue() {
			int selectedLineSize = sliderValueToLineSize(this.value);
			this.value = lineSizeToSliderValue(selectedLineSize);
			VendorHoloDisplayScreen.this.setLineSize(this.lineIndex, selectedLineSize);
			this.updateMessage();
		}
	}

}
