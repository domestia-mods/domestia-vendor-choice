package domestia_vendor_choice.client;

import domestia_vendor_choice.DomestiaVendorChoice;
import domestia_vendor_choice.VendorNoteBlockEntity;
import domestia_vendor_choice.VendorNoteOpenPayload;
import domestia_vendor_choice.VendorNoteSavePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class VendorNoteScreen extends Screen {
	private static final int PANEL_MAX_WIDTH = 320;
	private static final int PANEL_MAX_HEIGHT = 260;
	private static final int PANEL_MARGIN = 12;
	private static final int PANEL_PADDING = 16;
	private static final int TEXTURE_WIDTH = 320;
	private static final int TEXTURE_HEIGHT = 260;

	private static final int BUTTON_WIDTH = 90;
	private static final int BUTTON_HEIGHT = 20;
	private static final int BUTTON_GAP = 8;

	private static final int TITLE_AREA_X = 29;
	private static final int TITLE_AREA_Y = 13;
	private static final int TITLE_AREA_WIDTH = 265;
	private static final int TITLE_AREA_HEIGHT = 21;
	private static final int AUTHOR_AREA_X = 29;
	private static final int AUTHOR_AREA_Y = 30;
	private static final int AUTHOR_AREA_WIDTH = 265;
	private static final int AUTHOR_AREA_HEIGHT = 11;
	private static final int BODY_AREA_X = 28;
	private static final int BODY_AREA_Y = 49;
	private static final int BODY_AREA_WIDTH = 265;
	private static final int BODY_COUNTER_HEIGHT = 9;
	private static final int BODY_COUNTER_BODY_GAP = 7;
	private static final int BODY_COUNTER_BUTTON_GAP = 5;
	private static final int BUTTON_AREA_Y = TEXTURE_HEIGHT - PANEL_PADDING - BUTTON_HEIGHT;
	private static final int BODY_COUNTER_Y = BUTTON_AREA_Y - BODY_COUNTER_BUTTON_GAP - BODY_COUNTER_HEIGHT;
	private static final int BODY_AREA_HEIGHT = BODY_COUNTER_Y - BODY_COUNTER_BODY_GAP - BODY_AREA_Y;
	private static final int BODY_SCROLLBAR_WIDTH = 2;
	private static final int BODY_SCROLLBAR_GAP = 3;
	private static final int BODY_SCROLL_LINES_PER_WHEEL = 3;
	private static final int CARET_BLINK_TICKS = 12;

	private static final float TITLE_TEXT_SCALE = 1.25F;
	private static final float AUTHOR_TEXT_SCALE = 0.75F;

	private static final int COLOR_TEXT = 0xFF000000;
	private static final int COLOR_CARET = 0xFF000000;
	private static final int COLOR_SELECTION_BACKGROUND = 0x663399FF;
	private static final int COLOR_SCROLLBAR_THUMB = 0xFF000000;
	private static final String BODY_COUNTER_SEPARATOR = "/";

	private static final Component SCREEN_TITLE = Component.translatable("screen.domestia_vendor_choice.vendor_note");
	private static final Component BUTTON_SAVE = Component.translatable("screen.domestia_vendor_choice.vendor_note.save");
	private static final Component BUTTON_CANCEL = Component.translatable("screen.domestia_vendor_choice.vendor_note.cancel");
	private static final Component BUTTON_CLOSE = Component.translatable("screen.domestia_vendor_choice.vendor_note.close");
	private static final String ID_AUTHOR_BY = "screen.domestia_vendor_choice.vendor_note.by";

	private static final Identifier TEXTURE_VENDOR_NOTE_CONTROL = Identifier.fromNamespaceAndPath(
			DomestiaVendorChoice.MOD_ID,
			"textures/gui/vendor_note_control.png"
	);

	private final VendorNoteOpenPayload payload;
	private String titleValue;
	private String bodyValue;
	private EditorFocus editorFocus = EditorFocus.NONE;
	private int titleCaret;
	private int titleSelectionAnchor;
	private int titleVisibleStart;
	private int bodyCaret;
	private int bodySelectionAnchor;
	private int bodyScrollLine;
	private int panelLeft;
	private int panelTop;
	private int panelWidth;
	private int panelHeight;
	private int cursorTicks;

	public VendorNoteScreen(VendorNoteOpenPayload payload) {
		super(SCREEN_TITLE);
		this.payload = payload;
		this.titleValue = trimToMaxLength(payload.title(), VendorNoteBlockEntity.MAX_TITLE_LENGTH);
		this.bodyValue = trimToMaxLength(payload.body(), VendorNoteBlockEntity.MAX_BODY_LENGTH);
		this.titleCaret = this.titleValue.length();
		this.titleSelectionAnchor = this.titleCaret;
		this.bodyCaret = this.bodyValue.length();
		this.bodySelectionAnchor = this.bodyCaret;
	}

	@Override
	protected void init() {
		this.panelWidth = Math.min(PANEL_MAX_WIDTH, this.width - PANEL_MARGIN * 2);
		this.panelHeight = Math.min(PANEL_MAX_HEIGHT, this.height - PANEL_MARGIN * 2);
		this.panelLeft = (this.width - this.panelWidth) / 2;
		this.panelTop = (this.height - this.panelHeight) / 2;
		this.editorFocus = this.payload.editable() ? EditorFocus.TITLE : EditorFocus.NONE;
		this.titleSelectionAnchor = this.titleCaret;
		this.bodySelectionAnchor = this.bodyCaret;

		int buttonTop = this.toScreenY(BUTTON_AREA_Y);

		if (this.payload.editable()) {
			int totalButtonWidth = BUTTON_WIDTH * 2 + BUTTON_GAP;
			int firstButtonLeft = this.panelLeft + (this.panelWidth - totalButtonWidth) / 2;

			this.addRenderableWidget(
					Button.builder(BUTTON_SAVE, button -> this.saveAndClose())
							.bounds(firstButtonLeft, buttonTop, BUTTON_WIDTH, BUTTON_HEIGHT)
							.build()
			);
			this.addRenderableWidget(
					Button.builder(BUTTON_CANCEL, button -> this.onClose())
							.bounds(firstButtonLeft + BUTTON_WIDTH + BUTTON_GAP, buttonTop, BUTTON_WIDTH, BUTTON_HEIGHT)
							.build()
			);
		} else {
			this.addRenderableWidget(
					Button.builder(BUTTON_CLOSE, button -> this.onClose())
							.bounds(this.panelLeft + (this.panelWidth - BUTTON_WIDTH) / 2, buttonTop, BUTTON_WIDTH, BUTTON_HEIGHT)
							.build()
			);
		}
	}

	private int toScreenX(int textureX) {
		return this.panelLeft + this.scaleX(textureX);
	}

	private int toScreenY(int textureY) {
		return this.panelTop + this.scaleY(textureY);
	}

	private int scaleX(int textureValue) {
		return Math.round(textureValue * this.panelWidth / (float) TEXTURE_WIDTH);
	}

	private int scaleY(int textureValue) {
		return Math.round(textureValue * this.panelHeight / (float) TEXTURE_HEIGHT);
	}

	private void saveAndClose() {
		ClientPlayNetworking.send(
				new VendorNoteSavePayload(
						this.payload.pos(),
						trimToMaxLength(this.titleValue, VendorNoteBlockEntity.MAX_TITLE_LENGTH),
						trimToMaxLength(this.bodyValue, VendorNoteBlockEntity.MAX_BODY_LENGTH)
				)
		);
		this.onClose();
	}

	@Override
	public void tick() {
		super.tick();
		this.cursorTicks++;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
		int mouseX = (int) click.x();
		int mouseY = (int) click.y();

		if (this.payload.editable() && this.isInsideTitleArea(mouseX, mouseY)) {
			int targetCaret = this.titleCaretFromMouseX(mouseX);
			boolean extendSelection = this.editorFocus == EditorFocus.TITLE && this.isShiftDown();
			this.editorFocus = EditorFocus.TITLE;
			this.moveTitleCaretTo(targetCaret, extendSelection);
			return true;
		}

		if (this.isInsideBodyArea(mouseX, mouseY)) {
			if (this.payload.editable()) {
				int targetCaret = this.bodyCaretFromMouse(mouseX, mouseY);
				boolean extendSelection = this.editorFocus == EditorFocus.BODY && this.isShiftDown();
				this.editorFocus = EditorFocus.BODY;
				this.moveBodyCaretTo(targetCaret, extendSelection);
			}
			return true;
		}

		return super.mouseClicked(click, doubled);
	}


	public boolean mouseDragged(MouseButtonEvent click, double dragX, double dragY) {
		if (!this.payload.editable()) {
			return false;
		}

		int mouseX = (int) click.x();
		int mouseY = (int) click.y();

		if (this.editorFocus == EditorFocus.TITLE && this.isInsideTitleArea(mouseX, mouseY)) {
			this.moveTitleCaretTo(this.titleCaretFromMouseX(mouseX), true);
			return true;
		}

		if (this.editorFocus == EditorFocus.BODY && this.isInsideBodyArea(mouseX, mouseY)) {
			this.moveBodyCaretTo(this.bodyCaretFromMouse(mouseX, mouseY), true);
			return true;
		}

		return false;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (this.isInsideBodyArea((int) mouseX, (int) mouseY)) {
			if (verticalAmount > 0.0D) {
				this.scrollBody(-BODY_SCROLL_LINES_PER_WHEEL);
			} else if (verticalAmount < 0.0D) {
				this.scrollBody(BODY_SCROLL_LINES_PER_WHEEL);
			}
			return true;
		}

		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.isEscape()) {
			return super.keyPressed(event);
		}

		if (!this.payload.editable()) {
			return false;
		}

		if (event.key() == GLFW.GLFW_KEY_TAB) {
			this.editorFocus = this.editorFocus == EditorFocus.TITLE ? EditorFocus.BODY : EditorFocus.TITLE;
			this.clearSelectionForFocus(this.editorFocus);
			this.cursorTicks = 0;
			return true;
		}

		if (this.isControlDown()) {
			if (event.key() == GLFW.GLFW_KEY_A) {
				this.selectAllFocusedText();
				return true;
			}

			if (event.key() == GLFW.GLFW_KEY_C) {
				this.copyFocusedSelection();
				return true;
			}

			if (event.key() == GLFW.GLFW_KEY_X) {
				this.cutFocusedSelection();
				return true;
			}
		}

		if (event.isPaste()) {
			this.insertText(this.getClipboardText());
			return true;
		}

		if (this.editorFocus == EditorFocus.TITLE) {
			return this.handleTitleKeyPressed(event);
		}

		if (this.editorFocus == EditorFocus.BODY) {
			return this.handleBodyKeyPressed(event);
		}

		return super.keyPressed(event);
	}

	private boolean handleTitleKeyPressed(KeyEvent event) {
		boolean selecting = this.isShiftDown();
		boolean byWord = this.isControlDown();

		if ((event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER)) {
			this.editorFocus = EditorFocus.BODY;
			this.clearBodySelection();
			this.cursorTicks = 0;
			return true;
		}

		if (event.isLeft()) {
			int targetCaret = byWord ? findPreviousWordBoundary(this.titleValue, this.titleCaret) : this.titleCaret - 1;
			this.moveTitleCaretTo(targetCaret, selecting);
			return true;
		}

		if (event.isRight()) {
			int targetCaret = byWord ? findNextWordBoundary(this.titleValue, this.titleCaret) : this.titleCaret + 1;
			this.moveTitleCaretTo(targetCaret, selecting);
			return true;
		}

		if (event.key() == GLFW.GLFW_KEY_HOME) {
			this.moveTitleCaretTo(0, selecting);
			return true;
		}

		if (event.key() == GLFW.GLFW_KEY_END) {
			this.moveTitleCaretTo(this.titleValue.length(), selecting);
			return true;
		}

		if (event.key() == GLFW.GLFW_KEY_BACKSPACE) {
			this.deleteBeforeTitleCaret(byWord);
			return true;
		}

		if (event.key() == GLFW.GLFW_KEY_DELETE) {
			this.deleteAfterTitleCaret(byWord);
			return true;
		}

		return false;
	}

	private boolean handleBodyKeyPressed(KeyEvent event) {
		boolean selecting = this.isShiftDown();
		boolean byWord = this.isControlDown();

		if (event.isLeft()) {
			int targetCaret = byWord ? findPreviousWordBoundary(this.bodyValue, this.bodyCaret) : this.bodyCaret - 1;
			this.moveBodyCaretTo(targetCaret, selecting);
			return true;
		}

		if (event.isRight()) {
			int targetCaret = byWord ? findNextWordBoundary(this.bodyValue, this.bodyCaret) : this.bodyCaret + 1;
			this.moveBodyCaretTo(targetCaret, selecting);
			return true;
		}

		if (event.isUp()) {
			this.moveBodyCaretVertically(-1, selecting);
			return true;
		}

		if (event.isDown()) {
			this.moveBodyCaretVertically(1, selecting);
			return true;
		}

		if (event.key() == GLFW.GLFW_KEY_HOME) {
			this.moveBodyCaretTo(this.getCurrentBodyLine().start(), selecting);
			return true;
		}

		if (event.key() == GLFW.GLFW_KEY_END) {
			this.moveBodyCaretTo(this.getCurrentBodyLine().end(), selecting);
			return true;
		}

		if (event.key() == GLFW.GLFW_KEY_PAGE_UP) {
			this.scrollBody(-this.getBodyVisibleLineCount());
			return true;
		}

		if (event.key() == GLFW.GLFW_KEY_PAGE_DOWN) {
			this.scrollBody(this.getBodyVisibleLineCount());
			return true;
		}

		if (event.key() == GLFW.GLFW_KEY_BACKSPACE) {
			this.deleteBeforeBodyCaret(byWord);
			return true;
		}

		if (event.key() == GLFW.GLFW_KEY_DELETE) {
			this.deleteAfterBodyCaret(byWord);
			return true;
		}

		if ((event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER)) {
			this.insertText("\n");
			return true;
		}

		return false;
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		if (!this.payload.editable() || this.editorFocus == EditorFocus.NONE) {
			return false;
		}

		int codepoint = event.codepoint();

		if (Character.isISOControl(codepoint)) {
			return false;
		}

		this.insertText(new String(Character.toChars(codepoint)));
		return true;
	}

	@Override
	public void onClose() {
		if (this.minecraft != null) {
			this.minecraft.setScreen(null);
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		graphics.blit(
				RenderPipelines.GUI_TEXTURED,
				TEXTURE_VENDOR_NOTE_CONTROL,
				this.panelLeft,
				this.panelTop,
				0.0F,
				0.0F,
				this.panelWidth,
				this.panelHeight,
				TEXTURE_WIDTH,
				TEXTURE_HEIGHT
		);

		super.extractRenderState(graphics, mouseX, mouseY, delta);

		this.renderTitle(graphics);
		this.renderScaledCenteredText(
				graphics,
				Component.translatable(ID_AUTHOR_BY, this.getOwnerNameComponent()),
				this.toScreenX(AUTHOR_AREA_X),
				this.toScreenY(AUTHOR_AREA_Y),
				this.scaleX(AUTHOR_AREA_WIDTH),
				this.scaleY(AUTHOR_AREA_HEIGHT),
				AUTHOR_TEXT_SCALE
		);
		this.renderBody(graphics);
		this.renderBodyCounter(graphics);
		this.renderBodyScrollbar(graphics);
	}

	private Component getOwnerNameComponent() {
		String ownerName = this.payload.ownerName();

		if (ownerName == null || ownerName.isBlank()) {
			return Component.translatable(VendorNoteBlockEntity.DEFAULT_OWNER_NAME_KEY);
		}

		return Component.literal(ownerName);
	}

	private void renderTitle(GuiGraphicsExtractor graphics) {
		TitleView titleView = this.getTitleView();
		int areaX = this.toScreenX(TITLE_AREA_X);
		int areaY = this.toScreenY(TITLE_AREA_Y);
		int areaWidth = this.scaleX(TITLE_AREA_WIDTH);
		int areaHeight = this.scaleY(TITLE_AREA_HEIGHT);
		int unscaledAreaWidth = Math.round(areaWidth / TITLE_TEXT_SCALE);
		int unscaledAreaHeight = Math.round(areaHeight / TITLE_TEXT_SCALE);
		int textWidth = this.font.width(titleView.text());
		int textX = Math.round((unscaledAreaWidth - textWidth) / 2.0F);
		int textY = Math.round((unscaledAreaHeight - this.font.lineHeight) / 2.0F);

		graphics.pose().pushMatrix();
		graphics.pose().translate(areaX, areaY);
		graphics.pose().scale(TITLE_TEXT_SCALE, TITLE_TEXT_SCALE);

		this.renderTitleSelection(graphics, titleView, textX, textY);
		graphics.text(this.font, Component.literal(titleView.text()), textX, textY, COLOR_TEXT, false);

		if (this.shouldRenderCaret(EditorFocus.TITLE)) {
			int caretInView = Math.max(0, this.titleCaret - titleView.start());
			int caretTextWidth = this.font.width(titleView.text().substring(0, Math.min(caretInView, titleView.text().length())));
			graphics.fill(textX + caretTextWidth, textY, textX + caretTextWidth + 1, textY + this.font.lineHeight, COLOR_CARET);
		}

		graphics.pose().popMatrix();
	}

	private void renderTitleSelection(GuiGraphicsExtractor graphics, TitleView titleView, int textX, int textY) {
		if (this.editorFocus != EditorFocus.TITLE || !this.hasTitleSelection()) {
			return;
		}

		int selectionStart = Math.max(this.getTitleSelectionStart(), titleView.start());
		int selectionEnd = Math.min(this.getTitleSelectionEnd(), titleView.start() + titleView.text().length());

		if (selectionStart >= selectionEnd) {
			return;
		}

		int startOffset = selectionStart - titleView.start();
		int endOffset = selectionEnd - titleView.start();
		int selectionX1 = textX + this.font.width(titleView.text().substring(0, startOffset));
		int selectionX2 = textX + this.font.width(titleView.text().substring(0, endOffset));
		graphics.fill(selectionX1, textY, selectionX2, textY + this.font.lineHeight, COLOR_SELECTION_BACKGROUND);
	}

	private void renderBody(GuiGraphicsExtractor graphics) {
		List<RenderLine> lines = this.getBodyLines();
		this.clampBodyScroll(lines.size());

		int bodyX = this.toScreenX(BODY_AREA_X);
		int bodyY = this.toScreenY(BODY_AREA_Y);
		int lineHeight = this.font.lineHeight;
		int visibleLines = this.getBodyVisibleLineCount();
		int endLine = Math.min(lines.size(), this.bodyScrollLine + visibleLines);

		for (int lineIndex = this.bodyScrollLine; lineIndex < endLine; lineIndex++) {
			RenderLine line = lines.get(lineIndex);
			int y = bodyY + (lineIndex - this.bodyScrollLine) * lineHeight;
			this.renderBodySelection(graphics, line, bodyX, y, lineHeight);
			graphics.text(this.font, line.text(), bodyX, y, COLOR_TEXT, false);
		}

		if (this.shouldRenderCaret(EditorFocus.BODY)) {
			CaretPlacement caret = this.getBodyCaretPlacement(lines);
			if (caret.lineIndex() >= this.bodyScrollLine && caret.lineIndex() < this.bodyScrollLine + visibleLines) {
				RenderLine line = lines.get(caret.lineIndex());
				String beforeCaret = this.bodyValue.substring(line.start(), Math.min(caret.index(), line.end()));
				int caretX = bodyX + this.font.width(beforeCaret);
				int caretY = bodyY + (caret.lineIndex() - this.bodyScrollLine) * lineHeight;
				graphics.fill(caretX, caretY, caretX + 1, caretY + lineHeight, COLOR_CARET);
			}
		}
	}

	private void renderBodySelection(GuiGraphicsExtractor graphics, RenderLine line, int bodyX, int y, int lineHeight) {
		if (this.editorFocus != EditorFocus.BODY || !this.hasBodySelection()) {
			return;
		}

		int selectionStart = Math.max(this.getBodySelectionStart(), line.start());
		int selectionEnd = Math.min(this.getBodySelectionEnd(), line.end());

		if (selectionStart >= selectionEnd) {
			return;
		}

		int selectionX1 = bodyX + this.font.width(this.bodyValue.substring(line.start(), selectionStart));
		int selectionX2 = bodyX + this.font.width(this.bodyValue.substring(line.start(), selectionEnd));
		graphics.fill(selectionX1, y, Math.max(selectionX1 + 1, selectionX2), y + lineHeight, COLOR_SELECTION_BACKGROUND);
	}

	private void renderBodyCounter(GuiGraphicsExtractor graphics) {
		String counterText = this.bodyValue.length() + BODY_COUNTER_SEPARATOR + VendorNoteBlockEntity.MAX_BODY_LENGTH;
		int counterX = this.toScreenX(BODY_AREA_X + BODY_AREA_WIDTH) - this.font.width(counterText);
		int counterY = this.toScreenY(BODY_COUNTER_Y);

		graphics.text(this.font, counterText, counterX, counterY, COLOR_TEXT, false);
	}

	private void renderBodyScrollbar(GuiGraphicsExtractor graphics) {
		List<RenderLine> lines = this.getBodyLines();
		int visibleLines = this.getBodyVisibleLineCount();

		if (lines.size() <= visibleLines) {
			return;
		}

		int bodyX = this.toScreenX(BODY_AREA_X);
		int bodyY = this.toScreenY(BODY_AREA_Y);
		int bodyWidth = this.scaleX(BODY_AREA_WIDTH);
		int bodyHeight = this.scaleY(BODY_AREA_HEIGHT);
		int scrollbarX = bodyX + bodyWidth - BODY_SCROLLBAR_WIDTH;
		int minThumbHeight = Math.max(8, bodyHeight / 8);
		int thumbHeight = Math.max(minThumbHeight, bodyHeight * visibleLines / lines.size());
		int maxScroll = Math.max(1, lines.size() - visibleLines);
		int thumbTravel = Math.max(1, bodyHeight - thumbHeight);
		int thumbY = bodyY + thumbTravel * this.bodyScrollLine / maxScroll;

		graphics.fill(scrollbarX, thumbY, scrollbarX + BODY_SCROLLBAR_WIDTH, thumbY + thumbHeight, COLOR_SCROLLBAR_THUMB);
	}

	private void renderScaledCenteredText(
			GuiGraphicsExtractor graphics,
			Component text,
			int areaX,
			int areaY,
			int areaWidth,
			int areaHeight,
			float scale
	) {
		graphics.pose().pushMatrix();
		graphics.pose().translate(areaX, areaY);
		graphics.pose().scale(scale, scale);

		float scaledWidth = areaWidth / scale;
		float scaledHeight = areaHeight / scale;
		int textX = Math.round((scaledWidth - this.font.width(text)) / 2.0F);
		int textY = Math.round((scaledHeight - this.font.lineHeight) / 2.0F);
		graphics.text(this.font, text, textX, textY, COLOR_TEXT, false);

		graphics.pose().popMatrix();
	}

	private void insertText(String value) {
		if (value.isEmpty()) {
			return;
		}

		if (this.editorFocus == EditorFocus.TITLE) {
			String sanitized = value.replace("\r", "").replace("\n", " ");
			this.deleteTitleSelection();
			int available = VendorNoteBlockEntity.MAX_TITLE_LENGTH - this.titleValue.length();

			if (available <= 0) {
				return;
			}

			String inserted = trimToMaxLength(sanitized, available);
			this.titleValue = this.titleValue.substring(0, this.titleCaret) + inserted + this.titleValue.substring(this.titleCaret);
			this.moveTitleCaretTo(this.titleCaret + inserted.length(), false);
			return;
		}

		if (this.editorFocus == EditorFocus.BODY) {
			String normalized = value.replace("\r\n", "\n").replace('\r', '\n');
			this.deleteBodySelection();
			int available = VendorNoteBlockEntity.MAX_BODY_LENGTH - this.bodyValue.length();

			if (available <= 0) {
				return;
			}

			String inserted = trimToMaxLength(normalized, available);
			this.bodyValue = this.bodyValue.substring(0, this.bodyCaret) + inserted + this.bodyValue.substring(this.bodyCaret);
			this.moveBodyCaretTo(this.bodyCaret + inserted.length(), false);
		}
	}

	private void deleteBeforeTitleCaret(boolean byWord) {
		if (this.deleteTitleSelection()) {
			return;
		}

		int deleteStart = byWord ? findPreviousWordBoundary(this.titleValue, this.titleCaret) : this.titleCaret - 1;
		this.deleteTitleRange(deleteStart, this.titleCaret);
	}

	private void deleteAfterTitleCaret(boolean byWord) {
		if (this.deleteTitleSelection()) {
			return;
		}

		int deleteEnd = byWord ? findNextWordBoundary(this.titleValue, this.titleCaret) : this.titleCaret + 1;
		this.deleteTitleRange(this.titleCaret, deleteEnd);
	}

	private void deleteBeforeBodyCaret(boolean byWord) {
		if (this.deleteBodySelection()) {
			return;
		}

		int deleteStart = byWord ? findPreviousWordBoundary(this.bodyValue, this.bodyCaret) : this.bodyCaret - 1;
		this.deleteBodyRange(deleteStart, this.bodyCaret);
	}

	private void deleteAfterBodyCaret(boolean byWord) {
		if (this.deleteBodySelection()) {
			return;
		}

		int deleteEnd = byWord ? findNextWordBoundary(this.bodyValue, this.bodyCaret) : this.bodyCaret + 1;
		this.deleteBodyRange(this.bodyCaret, deleteEnd);
	}

	private boolean deleteTitleSelection() {
		if (!this.hasTitleSelection()) {
			return false;
		}

		this.deleteTitleRange(this.getTitleSelectionStart(), this.getTitleSelectionEnd());
		return true;
	}

	private boolean deleteBodySelection() {
		if (!this.hasBodySelection()) {
			return false;
		}

		this.deleteBodyRange(this.getBodySelectionStart(), this.getBodySelectionEnd());
		return true;
	}

	private void deleteTitleRange(int start, int end) {
		start = Math.max(0, Math.min(start, this.titleValue.length()));
		end = Math.max(0, Math.min(end, this.titleValue.length()));

		if (start >= end) {
			return;
		}

		this.titleValue = this.titleValue.substring(0, start) + this.titleValue.substring(end);
		this.moveTitleCaretTo(start, false);
	}

	private void deleteBodyRange(int start, int end) {
		start = Math.max(0, Math.min(start, this.bodyValue.length()));
		end = Math.max(0, Math.min(end, this.bodyValue.length()));

		if (start >= end) {
			return;
		}

		this.bodyValue = this.bodyValue.substring(0, start) + this.bodyValue.substring(end);
		this.moveBodyCaretTo(start, false);
	}

	private void moveBodyCaretVertically(int direction, boolean selecting) {
		List<RenderLine> lines = this.getBodyLines();
		CaretPlacement placement = this.getBodyCaretPlacement(lines);
		int targetLineIndex = Math.max(0, Math.min(lines.size() - 1, placement.lineIndex() + direction));
		RenderLine currentLine = lines.get(placement.lineIndex());
		RenderLine targetLine = lines.get(targetLineIndex);
		String currentPrefix = this.bodyValue.substring(currentLine.start(), Math.min(this.bodyCaret, currentLine.end()));
		int desiredX = this.font.width(currentPrefix);
		this.moveBodyCaretTo(this.indexFromLineAndX(targetLine, desiredX), selecting);
	}

	private void keepBodyCaretVisible() {
		List<RenderLine> lines = this.getBodyLines();
		CaretPlacement placement = this.getBodyCaretPlacement(lines);
		int visibleLines = this.getBodyVisibleLineCount();

		if (placement.lineIndex() < this.bodyScrollLine) {
			this.bodyScrollLine = placement.lineIndex();
		} else if (placement.lineIndex() >= this.bodyScrollLine + visibleLines) {
			this.bodyScrollLine = placement.lineIndex() - visibleLines + 1;
		}

		this.clampBodyScroll(lines.size());
	}

	private void scrollBody(int amount) {
		this.bodyScrollLine += amount;
		this.clampBodyScroll(this.getBodyLines().size());
	}

	private void clampBodyScroll(int lineCount) {
		int maxScroll = Math.max(0, lineCount - this.getBodyVisibleLineCount());
		this.bodyScrollLine = Math.max(0, Math.min(this.bodyScrollLine, maxScroll));
	}

	private int getBodyVisibleLineCount() {
		return Math.max(1, this.scaleY(BODY_AREA_HEIGHT) / this.font.lineHeight);
	}

	private RenderLine getCurrentBodyLine() {
		List<RenderLine> lines = this.getBodyLines();
		return lines.get(this.getBodyCaretPlacement(lines).lineIndex());
	}

	private CaretPlacement getBodyCaretPlacement(List<RenderLine> lines) {
		for (int i = 0; i < lines.size(); i++) {
			RenderLine line = lines.get(i);
			if (this.bodyCaret >= line.start() && this.bodyCaret <= line.end()) {
				return new CaretPlacement(i, this.bodyCaret);
			}
		}

		return new CaretPlacement(lines.size() - 1, lines.get(lines.size() - 1).end());
	}

	private List<RenderLine> getBodyLines() {
		List<RenderLine> lines = new ArrayList<>();
		int textWidth = this.scaleX(BODY_AREA_WIDTH) - BODY_SCROLLBAR_WIDTH - BODY_SCROLLBAR_GAP;
		int start = 0;

		while (start <= this.bodyValue.length()) {
			int newline = this.bodyValue.indexOf('\n', start);
			int paragraphEnd = newline >= 0 ? newline : this.bodyValue.length();
			this.addWrappedLines(lines, start, paragraphEnd, textWidth);

			if (newline < 0) {
				break;
			}

			start = newline + 1;
			if (start == this.bodyValue.length()) {
				lines.add(new RenderLine(start, start, ""));
				break;
			}
		}

		if (lines.isEmpty()) {
			lines.add(new RenderLine(0, 0, ""));
		}

		return lines;
	}

	private void addWrappedLines(List<RenderLine> lines, int start, int end, int maxWidth) {
		if (start == end) {
			lines.add(new RenderLine(start, end, ""));
			return;
		}

		int lineStart = start;

		while (lineStart < end) {
			int lineEnd = this.findSpaceWrappedLineEnd(lineStart, end, maxWidth);

			if (lineEnd <= lineStart) {
				lineEnd = Math.min(end, lineStart + 1);
			}

			lines.add(new RenderLine(lineStart, lineEnd, this.bodyValue.substring(lineStart, lineEnd)));
			lineStart = lineEnd;
		}
	}

	private int findSpaceWrappedLineEnd(int start, int end, int maxWidth) {
		int lastWhitespaceBreak = -1;

		for (int index = start + 1; index <= end; index++) {
			String candidate = this.bodyValue.substring(start, index);

			if (Character.isWhitespace(this.bodyValue.charAt(index - 1))) {
				lastWhitespaceBreak = index;
			}

			if (this.font.width(candidate) > maxWidth) {
				if (lastWhitespaceBreak > start) {
					return lastWhitespaceBreak;
				}

				return this.findNextWhitespaceBreak(start, end);
			}
		}

		return end;
	}

	private int findNextWhitespaceBreak(int start, int end) {
		for (int index = start + 1; index < end; index++) {
			if (Character.isWhitespace(this.bodyValue.charAt(index))) {
				return index + 1;
			}
		}

		return end;
	}

	private int bodyCaretFromMouse(int mouseX, int mouseY) {
		List<RenderLine> lines = this.getBodyLines();
		int bodyX = this.toScreenX(BODY_AREA_X);
		int bodyY = this.toScreenY(BODY_AREA_Y);
		int lineIndex = this.bodyScrollLine + Math.max(0, (mouseY - bodyY) / this.font.lineHeight);
		lineIndex = Math.max(0, Math.min(lines.size() - 1, lineIndex));
		RenderLine line = lines.get(lineIndex);
		return this.indexFromLineAndX(line, Math.max(0, mouseX - bodyX));
	}

	private int indexFromLineAndX(RenderLine line, int x) {
		int bestIndex = line.start();

		for (int index = line.start(); index <= line.end(); index++) {
			String before = this.bodyValue.substring(line.start(), index);
			int charX = this.font.width(before);

			if (charX >= x) {
				return Math.abs(charX - x) < Math.abs(this.font.width(this.bodyValue.substring(line.start(), bestIndex)) - x)
						? index
						: bestIndex;
			}

			bestIndex = index;
		}

		return line.end();
	}

	private TitleView getTitleView() {
		int maxWidth = Math.round(this.scaleX(TITLE_AREA_WIDTH) / TITLE_TEXT_SCALE);
		this.titleVisibleStart = Math.max(0, Math.min(this.titleVisibleStart, this.titleValue.length()));

		if (this.titleCaret < this.titleVisibleStart) {
			this.titleVisibleStart = this.titleCaret;
		}

		while (this.titleVisibleStart < this.titleCaret
				&& this.font.width(this.titleValue.substring(this.titleVisibleStart, this.titleCaret)) > maxWidth) {
			this.titleVisibleStart++;
		}

		int end = this.titleVisibleStart;

		while (end < this.titleValue.length()
				&& this.font.width(this.titleValue.substring(this.titleVisibleStart, end + 1)) <= maxWidth) {
			end++;
		}

		return new TitleView(this.titleVisibleStart, this.titleValue.substring(this.titleVisibleStart, end));
	}

	private int titleCaretFromMouseX(int mouseX) {
		TitleView titleView = this.getTitleView();
		int areaX = this.toScreenX(TITLE_AREA_X);
		int areaWidth = this.scaleX(TITLE_AREA_WIDTH);
		int unscaledAreaWidth = Math.round(areaWidth / TITLE_TEXT_SCALE);
		int textX = Math.round((unscaledAreaWidth - this.font.width(titleView.text())) / 2.0F);
		int localX = Math.round((mouseX - areaX) / TITLE_TEXT_SCALE) - textX;
		int bestOffset = 0;

		for (int offset = 0; offset <= titleView.text().length(); offset++) {
			int charX = this.font.width(titleView.text().substring(0, offset));

			if (charX >= localX) {
				return titleView.start() + (Math.abs(charX - localX) < Math.abs(this.font.width(titleView.text().substring(0, bestOffset)) - localX)
						? offset
						: bestOffset);
			}

			bestOffset = offset;
		}

		return titleView.start() + titleView.text().length();
	}

	private boolean shouldRenderCaret(EditorFocus focus) {
		return this.payload.editable() && this.editorFocus == focus && (this.cursorTicks / CARET_BLINK_TICKS) % 2 == 0;
	}

	private boolean isInsideTitleArea(int mouseX, int mouseY) {
		return this.isInsideArea(mouseX, mouseY, TITLE_AREA_X, TITLE_AREA_Y, TITLE_AREA_WIDTH, TITLE_AREA_HEIGHT);
	}

	private boolean isInsideBodyArea(int mouseX, int mouseY) {
		return this.isInsideArea(mouseX, mouseY, BODY_AREA_X, BODY_AREA_Y, BODY_AREA_WIDTH, BODY_AREA_HEIGHT);
	}

	private boolean isInsideArea(int mouseX, int mouseY, int textureX, int textureY, int textureWidth, int textureHeight) {
		int x = this.toScreenX(textureX);
		int y = this.toScreenY(textureY);
		int width = this.scaleX(textureWidth);
		int height = this.scaleY(textureHeight);

		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}

	private void moveTitleCaretTo(int targetCaret, boolean selecting) {
		this.titleCaret = Math.max(0, Math.min(targetCaret, this.titleValue.length()));

		if (!selecting) {
			this.titleSelectionAnchor = this.titleCaret;
		}

		this.cursorTicks = 0;
	}

	private void moveBodyCaretTo(int targetCaret, boolean selecting) {
		this.bodyCaret = Math.max(0, Math.min(targetCaret, this.bodyValue.length()));

		if (!selecting) {
			this.bodySelectionAnchor = this.bodyCaret;
		}

		this.keepBodyCaretVisible();
		this.cursorTicks = 0;
	}

	private void clearSelectionForFocus(EditorFocus focus) {
		if (focus == EditorFocus.TITLE) {
			this.clearTitleSelection();
		} else if (focus == EditorFocus.BODY) {
			this.clearBodySelection();
		}
	}

	private void clearTitleSelection() {
		this.titleSelectionAnchor = this.titleCaret;
	}

	private void clearBodySelection() {
		this.bodySelectionAnchor = this.bodyCaret;
	}

	private boolean hasTitleSelection() {
		return this.titleSelectionAnchor != this.titleCaret;
	}

	private boolean hasBodySelection() {
		return this.bodySelectionAnchor != this.bodyCaret;
	}

	private int getTitleSelectionStart() {
		return Math.min(this.titleSelectionAnchor, this.titleCaret);
	}

	private int getTitleSelectionEnd() {
		return Math.max(this.titleSelectionAnchor, this.titleCaret);
	}

	private int getBodySelectionStart() {
		return Math.min(this.bodySelectionAnchor, this.bodyCaret);
	}

	private int getBodySelectionEnd() {
		return Math.max(this.bodySelectionAnchor, this.bodyCaret);
	}

	private void selectAllFocusedText() {
		if (this.editorFocus == EditorFocus.TITLE) {
			this.titleSelectionAnchor = 0;
			this.titleCaret = this.titleValue.length();
		} else if (this.editorFocus == EditorFocus.BODY) {
			this.bodySelectionAnchor = 0;
			this.bodyCaret = this.bodyValue.length();
			this.keepBodyCaretVisible();
		}

		this.cursorTicks = 0;
	}

	private void copyFocusedSelection() {
		String selectedText = this.getFocusedSelectedText();

		if (!selectedText.isEmpty()) {
			this.setClipboardText(selectedText);
		}
	}

	private void cutFocusedSelection() {
		String selectedText = this.getFocusedSelectedText();

		if (selectedText.isEmpty()) {
			return;
		}

		this.setClipboardText(selectedText);

		if (this.editorFocus == EditorFocus.TITLE) {
			this.deleteTitleSelection();
		} else if (this.editorFocus == EditorFocus.BODY) {
			this.deleteBodySelection();
		}
	}

	private String getFocusedSelectedText() {
		if (this.editorFocus == EditorFocus.TITLE && this.hasTitleSelection()) {
			return this.titleValue.substring(this.getTitleSelectionStart(), this.getTitleSelectionEnd());
		}

		if (this.editorFocus == EditorFocus.BODY && this.hasBodySelection()) {
			return this.bodyValue.substring(this.getBodySelectionStart(), this.getBodySelectionEnd());
		}

		return "";
	}

	private void setClipboardText(String value) {
		if (this.minecraft != null) {
			this.minecraft.keyboardHandler.setClipboard(value);
		}
	}

	private boolean isShiftDown() {
		return this.isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT) || this.isKeyDown(GLFW.GLFW_KEY_RIGHT_SHIFT);
	}

	private boolean isControlDown() {
		return this.isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL) || this.isKeyDown(GLFW.GLFW_KEY_RIGHT_CONTROL);
	}

	private boolean isKeyDown(int key) {
		long windowHandle = GLFW.glfwGetCurrentContext();
		return windowHandle != 0L && GLFW.glfwGetKey(windowHandle, key) == GLFW.GLFW_PRESS;
	}

	private String getClipboardText() {
		if (this.minecraft == null) {
			return "";
		}

		return this.minecraft.keyboardHandler.getClipboard();
	}

	private static int findPreviousWordBoundary(String value, int position) {
		int index = Math.max(0, Math.min(position, value.length()));

		while (index > 0 && Character.isWhitespace(value.charAt(index - 1))) {
			index--;
		}

		while (index > 0 && !Character.isWhitespace(value.charAt(index - 1))) {
			index--;
		}

		return index;
	}

	private static int findNextWordBoundary(String value, int position) {
		int index = Math.max(0, Math.min(position, value.length()));

		while (index < value.length() && Character.isWhitespace(value.charAt(index))) {
			index++;
		}

		while (index < value.length() && !Character.isWhitespace(value.charAt(index))) {
			index++;
		}

		return index;
	}

	private static String trimToMaxLength(String value, int maxLength) {
		if (value.length() <= maxLength) {
			return value;
		}

		return value.substring(0, maxLength);
	}

	private enum EditorFocus {
		NONE,
		TITLE,
		BODY
	}

	private record RenderLine(int start, int end, String text) {
	}

	private record CaretPlacement(int lineIndex, int index) {
	}

	private record TitleView(int start, String text) {
	}
}
