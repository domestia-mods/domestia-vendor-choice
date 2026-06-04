package domestia_vendor_choice.client;

import domestia_vendor_choice.VendorSafeMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class VendorSafeScreen extends AbstractContainerScreen<VendorSafeMenu> {
	// Screen and vanilla generic container texture dimensions.
	private static final int SIZE_SCREEN_WIDTH = 176;
	private static final int SIZE_SCREEN_HEIGHT = 168;
	private static final int SIZE_TEXTURE_WIDTH = 256;
	private static final int SIZE_TEXTURE_HEIGHT = 256;

	// Safe inventory layout.
	private static final int COUNT_CONTAINER_ROWS = 3;
	private static final int SIZE_SLOT = 18;

	// Vanilla generic_54 layout sections.
	private static final int SIZE_UPPER_SECTION_HEIGHT = COUNT_CONTAINER_ROWS * SIZE_SLOT + 17;
	private static final int SIZE_LOWER_SECTION_HEIGHT = 96;
	private static final int POS_LOWER_SECTION_TEXTURE_Y = 126;

	// Text colors.
	private static final int COLOR_TEXT = 0xFF404040;

	// Label positions.
	private static final int POS_TITLE_LABEL_X = 8;
	private static final int POS_TITLE_LABEL_Y = 6;
	private static final int POS_INVENTORY_LABEL_X = 8;
	private static final int POS_INVENTORY_LABEL_Y = SIZE_SCREEN_HEIGHT - 94;

	// Vanilla chest/barrel-like texture.
	private static final Identifier TEXTURE_GENERIC_CONTAINER = Identifier.fromNamespaceAndPath(
			"minecraft",
			"textures/gui/container/generic_54.png"
	);

	public VendorSafeScreen(VendorSafeMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title, SIZE_SCREEN_WIDTH, SIZE_SCREEN_HEIGHT);

		this.titleLabelX = POS_TITLE_LABEL_X;
		this.titleLabelY = POS_TITLE_LABEL_Y;
		this.inventoryLabelX = POS_INVENTORY_LABEL_X;
		this.inventoryLabelY = POS_INVENTORY_LABEL_Y;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		this.renderUpperSection(graphics);
		this.renderLowerSection(graphics);
	}

	private void renderUpperSection(GuiGraphicsExtractor graphics) {
		graphics.blit(
				RenderPipelines.GUI_TEXTURED,
				TEXTURE_GENERIC_CONTAINER,
				this.leftPos,
				this.topPos,
				0.0F,
				0.0F,
				this.imageWidth,
				SIZE_UPPER_SECTION_HEIGHT,
				SIZE_TEXTURE_WIDTH,
				SIZE_TEXTURE_HEIGHT
		);
	}

	private void renderLowerSection(GuiGraphicsExtractor graphics) {
		graphics.blit(
				RenderPipelines.GUI_TEXTURED,
				TEXTURE_GENERIC_CONTAINER,
				this.leftPos,
				this.topPos + SIZE_UPPER_SECTION_HEIGHT,
				0.0F,
				POS_LOWER_SECTION_TEXTURE_Y,
				this.imageWidth,
				SIZE_LOWER_SECTION_HEIGHT,
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