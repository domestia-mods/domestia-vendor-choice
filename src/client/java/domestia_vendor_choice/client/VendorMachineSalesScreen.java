package domestia_vendor_choice.client;

import domestia_vendor_choice.DomestiaVendorChoice;
import domestia_vendor_choice.VendorMachineBlockEntity;
import domestia_vendor_choice.VendorMachineSalesMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class VendorMachineSalesScreen extends AbstractContainerScreen<VendorMachineSalesMenu> {
	// Screen and texture dimensions.
	private static final int SIZE_SCREEN_WIDTH = 176;
	private static final int SIZE_SCREEN_HEIGHT = 221;
	private static final int SIZE_TEXTURE_WIDTH = 176;
	private static final int SIZE_TEXTURE_HEIGHT = 221;

	// Text and overlay colors.
	private static final int COLOR_TEXT = 0xFF404040;
	private static final int COLOR_MESSAGE_ERROR = 0xFFFFCC00;
	private static final int COLOR_MESSAGE_SUCCESS = 0xFF33CC33;
	private static final int COLOR_DISABLED_OVERLAY = 0xFFCC3300;

	// Product list layout.
	private static final int POS_PRICE_DISPLAY_X = 8;
	private static final int POS_STOCK_BUTTON_X = 43;
	private static final int POS_PRODUCT_ROW_Y = 35;
	private static final int POS_PRODUCT_ROW_STEP = 18;

	// Button dimensions.
	private static final int SIZE_ITEM_BUTTON = 18;

	// Selected product button layout.
	private static final int POS_SELECTED_ITEM_BUTTON_X = 133;
	private static final int POS_SELECTED_ITEM_BUTTON_Y = 69;

	// Checkout button layout.
	private static final int POS_CHECKOUT_BUTTON_X = 97;
	private static final int POS_CHECKOUT_BUTTON_Y = 108;
	private static final int SIZE_CHECKOUT_BUTTON_WIDTH = 54;
	private static final int SIZE_CHECKOUT_BUTTON_HEIGHT = 18;

	// Message area layout.
	private static final int POS_MESSAGE_X = 85;
	private static final int POS_MESSAGE_Y = 94;

	// Label positions.
	private static final int POS_TITLE_LABEL_X = 8;
	private static final int POS_TITLE_LABEL_Y = 8;
	private static final int POS_PRICE_LABEL_X = 8;
	private static final int POS_PRICE_LABEL_Y = 24;
	private static final int POS_STOCK_LABEL_X = 42;
	private static final int POS_STOCK_LABEL_Y = 24;
	private static final int POS_DEAL_LABEL_X = 82;
	private static final int POS_DEAL_LABEL_Y = 24;
	private static final int POS_INVENTORY_LABEL_X = 8;
	private static final int POS_INVENTORY_LABEL_Y = 128;

	// Item rendering offsets.
	private static final int POS_ITEM_IN_BUTTON_OFFSET_X = 1;
	private static final int POS_ITEM_IN_BUTTON_OFFSET_Y = 1;

	// Disabled overlay geometry.
	private static final int SIZE_DISABLED_OVERLAY = 16;
	private static final int SIZE_DISABLED_OVERLAY_LINE = 2;

	// Client-side checkout error sound.
	private static final float VOLUME_CLIENT_CHECKOUT_ERROR = 0.65F;
	private static final float PITCH_CLIENT_CHECKOUT_ERROR = 0.55F;

	// Translation keys.
	private static final String ID_TITLE = "screen.domestia_vendor_choice.vendor_machine_sales";
	private static final String ID_LABEL_PRICE = "screen.domestia_vendor_choice.vendor_machine_sales.price";
	private static final String ID_LABEL_STOCK = "screen.domestia_vendor_choice.vendor_machine_sales.stock";
	private static final String ID_LABEL_DEAL = "screen.domestia_vendor_choice.vendor_machine_sales.deal";
	private static final String ID_LABEL_INVENTORY = "screen.domestia_vendor_choice.vendor_machine_sales.inventory";
	private static final String ID_BUTTON_CHECKOUT = "screen.domestia_vendor_choice.vendor_machine_sales.checkout";

	private static final String ID_MSG_SELECT_ITEM = "screen.domestia_vendor_choice.vendor_machine_sales.message.select_item";
	private static final String ID_MSG_SOLD_OUT = "screen.domestia_vendor_choice.vendor_machine_sales.message.sold_out";
	private static final String ID_MSG_NO_PRICE = "screen.domestia_vendor_choice.vendor_machine_sales.message.no_price";
	private static final String ID_MSG_VAULT_FULL = "screen.domestia_vendor_choice.vendor_machine_sales.message.vault_full";
	private static final String ID_MSG_NOT_ENOUGH = "screen.domestia_vendor_choice.vendor_machine_sales.message.not_enough";
	private static final String ID_MSG_WRONG_PAY = "screen.domestia_vendor_choice.vendor_machine_sales.message.wrong_pay";
	private static final String ID_MSG_CHECKOUT_UNAVAILABLE = "screen.domestia_vendor_choice.vendor_machine_sales.message.checkout_unavailable";
	private static final String ID_MSG_NO_STOCK = "screen.domestia_vendor_choice.vendor_machine_sales.message.no_stock";
	private static final String ID_MSG_THANK_YOU = "screen.domestia_vendor_choice.vendor_machine_sales.message.thank_you";

	// GUI texture.
	private static final Identifier TEXTURE_SALES = Identifier.fromNamespaceAndPath(
			DomestiaVendorChoice.MOD_ID,
			"textures/gui/vendor_machine_sales.png"
	);

	private int selectedProductIndex = -1;
	private int selectedQuantity = 0;
	private Component currentMessage = Component.empty();
	private int currentMessageColor = COLOR_MESSAGE_ERROR;

	public VendorMachineSalesScreen(VendorMachineSalesMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, Component.translatable(ID_TITLE), SIZE_SCREEN_WIDTH, SIZE_SCREEN_HEIGHT);

		this.titleLabelX = POS_TITLE_LABEL_X;
		this.titleLabelY = POS_TITLE_LABEL_Y;
		this.inventoryLabelX = POS_INVENTORY_LABEL_X;
		this.inventoryLabelY = POS_INVENTORY_LABEL_Y;
	}

	@Override
	protected void init() {
		super.init();

		this.addProductButtons();
		this.addSelectedItemButton();
		this.addCheckoutButton();
	}

	private void addProductButtons() {
		for (int productIndex = 0; productIndex < VendorMachineBlockEntity.STOCK_SLOT_COUNT; productIndex++) {
			final int capturedProductIndex = productIndex;
			int rowY = getProductRowY(productIndex);

			this.addRenderableWidget(Button.builder(
					Component.empty(),
					button -> this.handleProductButtonClick(capturedProductIndex)
			).bounds(
					this.leftPos + POS_STOCK_BUTTON_X,
					this.topPos + rowY,
					SIZE_ITEM_BUTTON,
					SIZE_ITEM_BUTTON
			).build());
		}
	}

	private void addSelectedItemButton() {
		this.addRenderableWidget(Button.builder(
				Component.empty(),
				button -> this.handleSelectedItemButtonClick()
		).bounds(
				this.leftPos + POS_SELECTED_ITEM_BUTTON_X,
				this.topPos + POS_SELECTED_ITEM_BUTTON_Y,
				SIZE_ITEM_BUTTON,
				SIZE_ITEM_BUTTON
		).build());
	}

	private void addCheckoutButton() {
		this.addRenderableWidget(Button.builder(
				Component.translatable(ID_BUTTON_CHECKOUT),
				button -> this.handleCheckoutButtonClick()
		).bounds(
				this.leftPos + POS_CHECKOUT_BUTTON_X,
				this.topPos + POS_CHECKOUT_BUTTON_Y,
				SIZE_CHECKOUT_BUTTON_WIDTH,
				SIZE_CHECKOUT_BUTTON_HEIGHT
		).build());
	}

	private void handleProductButtonClick(int productIndex) {
		ItemStack stockStack = this.menu.getStockDisplayStack(productIndex);

		if (stockStack.isEmpty()) {
			this.clearSelection();
			this.clearMessage();
			return;
		}

		ItemStack priceStack = this.menu.getPriceDisplayStack(productIndex);

		if (priceStack.isEmpty()) {
			this.showError(ID_MSG_NO_PRICE);
			return;
		}

		int maxSelectableQuantity = this.getMaxSelectableQuantity(productIndex);

		if (maxSelectableQuantity <= 0) {
			this.showError(ID_MSG_VAULT_FULL);
			return;
		}

		if (this.selectedProductIndex != productIndex) {
			this.selectedProductIndex = productIndex;
			this.selectedQuantity = 1;
			this.clearMessage();
			return;
		}

		if (this.selectedQuantity < maxSelectableQuantity) {
			this.selectedQuantity++;
			this.clearMessage();
			return;
		}

		if (maxSelectableQuantity < stockStack.getCount()) {
			this.showError(ID_MSG_VAULT_FULL);
			return;
		}

		this.showError(ID_MSG_NO_STOCK);
	}

	private void handleSelectedItemButtonClick() {
		if (this.selectedProductIndex < 0 || this.selectedQuantity <= 0) {
			return;
		}

		this.selectedQuantity--;

		if (this.selectedQuantity <= 0) {
			this.clearSelection();
		}

		this.clearMessage();
	}

	private void handleCheckoutButtonClick() {
		if (this.selectedProductIndex < 0 || this.selectedQuantity <= 0) {
			this.showError(ID_MSG_SELECT_ITEM);
			return;
		}

		ItemStack stockStack = this.menu.getStockDisplayStack(this.selectedProductIndex);

		if (stockStack.isEmpty()) {
			this.clearSelection();
			this.showError(ID_MSG_SOLD_OUT);
			return;
		}

		if (stockStack.getCount() < this.selectedQuantity) {
			this.showError(ID_MSG_SOLD_OUT);
			return;
		}

		ItemStack priceStack = this.menu.getPriceDisplayStack(this.selectedProductIndex);

		if (priceStack.isEmpty()) {
			this.showError(ID_MSG_NO_PRICE);
			return;
		}

		int maxSelectableQuantity = this.getMaxSelectableQuantity(this.selectedProductIndex);

		if (maxSelectableQuantity < this.selectedQuantity) {
			this.showError(ID_MSG_VAULT_FULL);
			return;
		}

		ItemStack paymentStack = this.menu.getPaymentStack();

		if (paymentStack.isEmpty()) {
			this.showError(ID_MSG_NOT_ENOUGH);
			return;
		}

		if (!ItemStack.isSameItemSameComponents(paymentStack, priceStack)) {
			this.showError(ID_MSG_WRONG_PAY);
			return;
		}

		int requiredPaymentCount = priceStack.getCount() * this.selectedQuantity;

		if (paymentStack.getCount() < requiredPaymentCount) {
			this.showError(ID_MSG_NOT_ENOUGH);
			return;
		}

		if (this.minecraft == null || this.minecraft.gameMode == null) {
			this.showError(ID_MSG_CHECKOUT_UNAVAILABLE);
			return;
		}

		int buttonId = VendorMachineSalesMenu.createCheckoutButtonId(this.selectedProductIndex, this.selectedQuantity);

		this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);

		this.clearSelection();
		this.showSuccess(ID_MSG_THANK_YOU);
	}

	private void clearSelection() {
		this.selectedProductIndex = -1;
		this.selectedQuantity = 0;
	}

	private void clearMessage() {
		this.currentMessage = Component.empty();
		this.currentMessageColor = COLOR_MESSAGE_ERROR;
	}

	private void showError(String translationKey) {
		this.currentMessage = Component.translatable(translationKey);
		this.currentMessageColor = COLOR_MESSAGE_ERROR;
		this.playClientCheckoutErrorSound();
	}

	private void showSuccess(String translationKey) {
		this.currentMessage = Component.translatable(translationKey);
		this.currentMessageColor = COLOR_MESSAGE_SUCCESS;
	}

	private void playClientCheckoutErrorSound() {
		if (this.minecraft == null || this.minecraft.player == null) {
			return;
		}

		this.minecraft.player.playSound(
				SoundEvents.NOTE_BLOCK_BASS.value(),
				VOLUME_CLIENT_CHECKOUT_ERROR,
				PITCH_CLIENT_CHECKOUT_ERROR
		);
	}

	private int getMaxSelectableQuantity(int productIndex) {
		ItemStack stockStack = this.menu.getStockDisplayStack(productIndex);

		if (stockStack.isEmpty()) {
			return 0;
		}

		ItemStack priceStack = this.menu.getPriceDisplayStack(productIndex);

		if (priceStack.isEmpty()) {
			return 0;
		}

		int stockCount = stockStack.getCount();
		int vaultAcceptablePurchaseCount = this.menu.getVaultAcceptablePurchaseCount(priceStack);

		return Math.min(stockCount, vaultAcceptablePurchaseCount);
	}

	private boolean isProductDisabled(int productIndex) {
		ItemStack stockStack = this.menu.getStockDisplayStack(productIndex);

		if (stockStack.isEmpty()) {
			return false;
		}

		ItemStack priceStack = this.menu.getPriceDisplayStack(productIndex);

		if (priceStack.isEmpty()) {
			return true;
		}

		int maxSelectableQuantity = this.getMaxSelectableQuantity(productIndex);

		if (maxSelectableQuantity <= 0) {
			return true;
		}

		return this.selectedProductIndex == productIndex
				&& this.selectedQuantity >= maxSelectableQuantity
				&& maxSelectableQuantity < stockStack.getCount();
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		graphics.blit(
				RenderPipelines.GUI_TEXTURED,
				TEXTURE_SALES,
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
		this.renderLabels(graphics);
		this.renderPriceDisplays(graphics);
		this.renderStockButtonItems(graphics);
		this.renderSelectedItem(graphics);
		this.renderCurrentMessage(graphics);
	}

	@Override
	protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractTooltip(graphics, mouseX, mouseY);
		this.extractStockItemTooltip(graphics, mouseX, mouseY);
	}

	private void extractStockItemTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		int hoveredProductIndex = this.getHoveredStockProductIndex(mouseX, mouseY);

		if (hoveredProductIndex < 0) {
			return;
		}

		ItemStack stockStack = this.menu.getStockDisplayStack(hoveredProductIndex);

		if (stockStack.isEmpty()) {
			return;
		}

		graphics.setTooltipForNextFrame(this.font, stockStack, mouseX, mouseY);
	}

	private int getHoveredStockProductIndex(int mouseX, int mouseY) {
		for (int productIndex = 0; productIndex < VendorMachineBlockEntity.STOCK_SLOT_COUNT; productIndex++) {
			if (this.isMouseOverStockButton(mouseX, mouseY, productIndex)) {
				return productIndex;
			}
		}

		return -1;
	}

	private boolean isMouseOverStockButton(int mouseX, int mouseY, int productIndex) {
		int buttonX = this.leftPos + POS_STOCK_BUTTON_X;
		int buttonY = this.topPos + getProductRowY(productIndex);

		return mouseX >= buttonX
				&& mouseX < buttonX + SIZE_ITEM_BUTTON
				&& mouseY >= buttonY
				&& mouseY < buttonY + SIZE_ITEM_BUTTON;
	}

	private void renderLabels(GuiGraphicsExtractor graphics) {
		graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, COLOR_TEXT, false);

		graphics.text(this.font, Component.translatable(ID_LABEL_PRICE), POS_PRICE_LABEL_X, POS_PRICE_LABEL_Y, COLOR_TEXT, false);
		graphics.text(this.font, Component.translatable(ID_LABEL_STOCK), POS_STOCK_LABEL_X, POS_STOCK_LABEL_Y, COLOR_TEXT, false);
		graphics.text(this.font, Component.translatable(ID_LABEL_DEAL), POS_DEAL_LABEL_X, POS_DEAL_LABEL_Y, COLOR_TEXT, false);
		graphics.text(this.font, Component.translatable(ID_LABEL_INVENTORY), this.inventoryLabelX, this.inventoryLabelY, COLOR_TEXT, false);
	}

	private void renderPriceDisplays(GuiGraphicsExtractor graphics) {
		for (int productIndex = 0; productIndex < VendorMachineBlockEntity.PRICE_SLOT_COUNT; productIndex++) {
			ItemStack stockStack = this.menu.getStockDisplayStack(productIndex);

			if (stockStack.isEmpty()) {
				continue;
			}

			ItemStack priceStack = this.menu.getPriceDisplayStack(productIndex);

			if (priceStack.isEmpty()) {
				continue;
			}

			int y = getProductRowY(productIndex);

			graphics.item(priceStack, POS_PRICE_DISPLAY_X, y);
			graphics.itemDecorations(this.font, priceStack, POS_PRICE_DISPLAY_X, y);
		}
	}

	private void renderStockButtonItems(GuiGraphicsExtractor graphics) {
		for (int productIndex = 0; productIndex < VendorMachineBlockEntity.STOCK_SLOT_COUNT; productIndex++) {
			ItemStack stockStack = this.menu.getStockDisplayStack(productIndex);

			if (stockStack.isEmpty()) {
				continue;
			}

			int x = POS_STOCK_BUTTON_X + POS_ITEM_IN_BUTTON_OFFSET_X;
			int y = getProductRowY(productIndex) + POS_ITEM_IN_BUTTON_OFFSET_Y;

			graphics.item(stockStack, x, y);
			graphics.itemDecorations(this.font, stockStack, x, y);

			if (this.isProductDisabled(productIndex)) {
				this.renderDisabledOverlay(graphics, x, y);
			}
		}
	}

	private void renderSelectedItem(GuiGraphicsExtractor graphics) {
		if (this.selectedProductIndex < 0 || this.selectedQuantity <= 0) {
			return;
		}

		ItemStack selectedStack = this.menu.getStockDisplayStack(this.selectedProductIndex);

		if (selectedStack.isEmpty()) {
			this.clearSelection();
			return;
		}

		ItemStack displayStack = selectedStack.copy();
		displayStack.setCount(this.selectedQuantity);

		int x = POS_SELECTED_ITEM_BUTTON_X + POS_ITEM_IN_BUTTON_OFFSET_X;
		int y = POS_SELECTED_ITEM_BUTTON_Y + POS_ITEM_IN_BUTTON_OFFSET_Y;

		graphics.item(displayStack, x, y);
		graphics.itemDecorations(this.font, displayStack, x, y);
	}

	private void renderCurrentMessage(GuiGraphicsExtractor graphics) {
		if (this.currentMessage == null || this.currentMessage.getString().isEmpty()) {
			return;
		}

		graphics.text(this.font, this.currentMessage, POS_MESSAGE_X, POS_MESSAGE_Y, this.currentMessageColor, false);
	}

	private void renderDisabledOverlay(GuiGraphicsExtractor graphics, int x, int y) {
		for (int offset = 0; offset < SIZE_DISABLED_OVERLAY; offset++) {
			graphics.fill(
					x + offset,
					y + offset,
					x + offset + SIZE_DISABLED_OVERLAY_LINE,
					y + offset + SIZE_DISABLED_OVERLAY_LINE,
					COLOR_DISABLED_OVERLAY
			);
		}
	}

	private static int getProductRowY(int productIndex) {
		return POS_PRODUCT_ROW_Y + productIndex * POS_PRODUCT_ROW_STEP;
	}
}