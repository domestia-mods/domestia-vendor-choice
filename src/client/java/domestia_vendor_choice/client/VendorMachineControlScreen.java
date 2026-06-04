package domestia_vendor_choice.client;

import domestia_vendor_choice.DomestiaVendorChoice;
import domestia_vendor_choice.VendorMachineControlMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class VendorMachineControlScreen extends AbstractContainerScreen<VendorMachineControlMenu> {
	// Screen and texture dimensions.
	private static final int SIZE_SCREEN_WIDTH = 176;
	private static final int SIZE_SCREEN_HEIGHT = 221;
	private static final int SIZE_TEXTURE_WIDTH = 176;
	private static final int SIZE_TEXTURE_HEIGHT = 221;

	// Text colors.
	private static final int COLOR_TEXT = 0xFF404040;

	// Label positions.
	private static final int POS_TITLE_LABEL_X = 8;
	private static final int POS_TITLE_LABEL_Y = 8;
	private static final int POS_STOCK_LABEL_X = 8;
	private static final int POS_STOCK_LABEL_Y = 24;
	private static final int POS_PRICE_LABEL_X = 42;
	private static final int POS_PRICE_LABEL_Y = 24;
	private static final int POS_VAULT_LABEL_X = 116;
	private static final int POS_VAULT_LABEL_Y = 24;
	private static final int POS_INVENTORY_LABEL_X = 8;
	private static final int POS_INVENTORY_LABEL_Y = 128;

	// Translation keys.
	private static final String ID_TITLE = "screen.domestia_vendor_choice.vendor_machine_control";
	private static final String ID_LABEL_STOCK = "screen.domestia_vendor_choice.vendor_machine_control.stock";
	private static final String ID_LABEL_PRICE = "screen.domestia_vendor_choice.vendor_machine_control.price";
	private static final String ID_LABEL_VAULT = "screen.domestia_vendor_choice.vendor_machine_control.vault";
	private static final String ID_LABEL_INVENTORY = "screen.domestia_vendor_choice.vendor_machine_control.inventory";

	// GUI texture.
	private static final Identifier TEXTURE_CONTROL = Identifier.fromNamespaceAndPath(
			DomestiaVendorChoice.MOD_ID,
			"textures/gui/vendor_machine_control.png"
	);

	public VendorMachineControlScreen(VendorMachineControlMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, Component.translatable(ID_TITLE), SIZE_SCREEN_WIDTH, SIZE_SCREEN_HEIGHT);

		this.titleLabelX = POS_TITLE_LABEL_X;
		this.titleLabelY = POS_TITLE_LABEL_Y;
		this.inventoryLabelX = POS_INVENTORY_LABEL_X;
		this.inventoryLabelY = POS_INVENTORY_LABEL_Y;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		graphics.blit(
				RenderPipelines.GUI_TEXTURED,
				TEXTURE_CONTROL,
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
	}

	private void renderLabels(GuiGraphicsExtractor graphics) {
		graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, COLOR_TEXT, false);

		graphics.text(this.font, Component.translatable(ID_LABEL_STOCK), POS_STOCK_LABEL_X, POS_STOCK_LABEL_Y, COLOR_TEXT, false);
		graphics.text(this.font, Component.translatable(ID_LABEL_PRICE), POS_PRICE_LABEL_X, POS_PRICE_LABEL_Y, COLOR_TEXT, false);
		graphics.text(this.font, Component.translatable(ID_LABEL_VAULT), POS_VAULT_LABEL_X, POS_VAULT_LABEL_Y, COLOR_TEXT, false);
		graphics.text(this.font, Component.translatable(ID_LABEL_INVENTORY), this.inventoryLabelX, this.inventoryLabelY, COLOR_TEXT, false);
	}
}