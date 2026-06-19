package domestia_vendor_choice.client;

import domestia_vendor_choice.ModBlockEntities;
import domestia_vendor_choice.ModMenus;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

public class DomestiaVendorChoiceClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModClientNetworking.initialize();
		this.registerScreens();
		this.registerBlockEntityRenderers();
	}

	private void registerScreens() {
		MenuScreens.register(
				ModMenus.VENDOR_MACHINE_CONTROL,
				VendorMachineControlScreen::new
		);

		MenuScreens.register(
				ModMenus.VENDOR_MACHINE_SALES,
				VendorMachineSalesScreen::new
		);

		MenuScreens.register(
				ModMenus.VENDOR_SAFE,
				VendorSafeScreen::new
		);

		MenuScreens.register(
				ModMenus.VENDOR_HOPPER,
				VendorHopperScreen::new
		);
	}

	private void registerBlockEntityRenderers() {
		BlockEntityRenderers.register(
				ModBlockEntities.VENDOR_MACHINE,
				VendorMachineBlockEntityRenderer::new
		);

		BlockEntityRenderers.register(
				ModBlockEntities.VENDOR_SAFE,
				VendorSafeBlockEntityRenderer::new
		);

		BlockEntityRenderers.register(
				ModBlockEntities.VENDOR_STAND,
				VendorStandBlockEntityRenderer::new
		);
	}
}