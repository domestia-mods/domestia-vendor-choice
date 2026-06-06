package domestia_vendor_choice.client;

import domestia_vendor_choice.VendorHopperMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class VendorHopperScreen extends AbstractContainerScreen<VendorHopperMenu> {
	// Screen and vanilla hopper texture dimensions.
	private static final int SIZE_SCREEN_WIDTH = 176;
	private static final int SIZE_SCREEN_HEIGHT = 133;
	private static final int SIZE_TEXTURE_WIDTH = 256;
	private static final int SIZE_TEXTURE_HEIGHT = 256;

	// Text colors.
	private static final int COLOR_TEXT = 0xFF404040;

	// Label positions.
	private static final int POS_TITLE_LABEL_X = 8;
	private static final int POS_TITLE_LABEL_Y = 6;
	private static final int POS_INVENTORY_LABEL_X = 8;
	private static final int POS_INVENTORY_LABEL_Y = SIZE_SCREEN_HEIGHT - 94;

	// Vanilla hopper texture. Temporary MVP UI, matching the vanilla hopper layout.
	private static final Identifier TEXTURE_HOPPER = Identifier.fromNamespaceAndPath(
			"minecraft",
			"textures/gui/container/hopper.png"
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
				TEXTURE_HOPPER,
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
		graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, COLOR_TEXT, false);
	}
}
