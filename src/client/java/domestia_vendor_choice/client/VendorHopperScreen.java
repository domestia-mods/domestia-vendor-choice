package domestia_vendor_choice.client;

import domestia_vendor_choice.DomestiaVendorChoice;
import domestia_vendor_choice.VendorHopperMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class VendorHopperScreen extends AbstractContainerScreen<VendorHopperMenu> {
	// Screen and texture dimensions.
	// The GUI texture is already authored at the final vanilla GUI size.
	private static final int SIZE_SCREEN_WIDTH = 176;
	private static final int SIZE_SCREEN_HEIGHT = 167;
	private static final int SIZE_TEXTURE_WIDTH = 176;
	private static final int SIZE_TEXTURE_HEIGHT = 167;

	// Text colors.
	private static final int COLOR_TEXT = 0xFF404040;

	// Label positions.
	private static final int POS_TITLE_LABEL_X = 8;
	private static final int POS_TITLE_LABEL_Y = 6;
	private static final int POS_TEMPLATE_LABEL_X = 8;
	private static final int POS_TEMPLATE_LABEL_Y = 34;
	private static final int POS_BUFFER_LABEL_X = 26;
	private static final int POS_BUFFER_LABEL_Y = 62;
	private static final int POS_INVENTORY_LABEL_X = 8;
	private static final int POS_INVENTORY_LABEL_Y = 75;

	private static final Component LABEL_TEMPLATES = Component.translatable("screen.domestia_vendor_choice.vendor_hopper.templates");
	private static final Component LABEL_BUFFER = Component.translatable("screen.domestia_vendor_choice.vendor_hopper.buffer");

	// Vendor Hopper control texture.
	private static final Identifier TEXTURE_VENDOR_HOPPER_CONTROL = Identifier.fromNamespaceAndPath(
			DomestiaVendorChoice.MOD_ID,
			"textures/gui/vendor_hopper_control.png"
	);

	public VendorHopperScreen(VendorHopperMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title, SIZE_SCREEN_WIDTH, SIZE_SCREEN_HEIGHT);

		this.titleLabelX = POS_TITLE_LABEL_X;
		this.titleLabelY = POS_TITLE_LABEL_Y;
		this.inventoryLabelX = POS_INVENTORY_LABEL_X;
		this.inventoryLabelY = POS_INVENTORY_LABEL_Y;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		graphics.blit(
				RenderPipelines.GUI_TEXTURED,
				TEXTURE_VENDOR_HOPPER_CONTROL,
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
		graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, COLOR_TEXT, false);
		graphics.text(this.font, LABEL_TEMPLATES, POS_TEMPLATE_LABEL_X, POS_TEMPLATE_LABEL_Y, COLOR_TEXT, false);
		graphics.text(this.font, LABEL_BUFFER, POS_BUFFER_LABEL_X, POS_BUFFER_LABEL_Y, COLOR_TEXT, false);
		graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, COLOR_TEXT, false);
	}
}