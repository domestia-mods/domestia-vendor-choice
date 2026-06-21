package domestia_vendor_choice.client;

import com.mojang.blaze3d.vertex.PoseStack;
import domestia_vendor_choice.VendorHoloDisplayBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class VendorHoloDisplayBlockEntityRenderer implements BlockEntityRenderer<VendorHoloDisplayBlockEntity, VendorHoloDisplayBlockEntityRenderState> {
	public VendorHoloDisplayBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
		VendorHoloDisplayTextWorldRenderer.setItemModelResolver(context.itemModelResolver());
	}

	@Override
	public VendorHoloDisplayBlockEntityRenderState createRenderState() {
		return new VendorHoloDisplayBlockEntityRenderState();
	}

	@Override
	public void extractRenderState(
			VendorHoloDisplayBlockEntity blockEntity,
			VendorHoloDisplayBlockEntityRenderState renderState,
			float tickProgress,
			Vec3 cameraPos,
			@Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
	) {
		BlockEntityRenderer.super.extractRenderState(blockEntity, renderState, tickProgress, cameraPos, crumblingOverlay);
	}

	@Override
	public void submit(
			VendorHoloDisplayBlockEntityRenderState renderState,
			PoseStack poseStack,
			SubmitNodeCollector collector,
			CameraRenderState cameraState
	) {
		// Holo board, text, and display content are rendered by VendorHoloDisplayTextWorldRenderer.
		// Keeping this submitter empty prevents a second vanilla item/model render pass from
		// turning block IMG content into a tilted 3D object.
	}
}
