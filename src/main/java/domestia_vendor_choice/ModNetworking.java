package domestia_vendor_choice;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class ModNetworking {
	private static final double MAX_INTERACTION_DISTANCE_SQUARED = 64.0D;

	private ModNetworking() {
	}

	public static void initialize() {
		PayloadTypeRegistry.clientboundPlay().register(VendorNoteOpenPayload.TYPE, VendorNoteOpenPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(VendorHoloDisplayOpenPayload.TYPE, VendorHoloDisplayOpenPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(VendorNoteSavePayload.TYPE, VendorNoteSavePayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(VendorHoloDisplaySavePayload.TYPE, VendorHoloDisplaySavePayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(VendorHoloDisplaySavePayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			BlockPos pos = payload.pos();

			if (!isWithinInteractionDistance(player, pos)) {
				return;
			}

			BlockEntity blockEntity = player.level().getBlockEntity(pos);

			if (!(blockEntity instanceof VendorHoloDisplayBlockEntity vendorHoloDisplayBlockEntity)) {
				return;
			}

			vendorHoloDisplayBlockEntity.updateSettings(
					player,
					VendorHoloDisplayBlockEntity.unpackLines(payload.lines()),
					VendorHoloDisplayBlockEntity.unpackLineSizes(payload.lineSizes()),
					payload.boardSize()
			);
		});

		ServerPlayNetworking.registerGlobalReceiver(VendorNoteSavePayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			BlockPos pos = payload.pos();

			if (!isWithinInteractionDistance(player, pos)) {
				return;
			}

			BlockEntity blockEntity = player.level().getBlockEntity(pos);

			if (!(blockEntity instanceof VendorNoteBlockEntity vendorNoteBlockEntity)) {
				return;
			}

			vendorNoteBlockEntity.updateText(player, payload.title(), payload.body());
		});

		DomestiaVendorChoice.LOGGER.info("Registering Domestia Vendor Choice networking.");
	}

	public static void openVendorHoloDisplay(ServerPlayer player, VendorHoloDisplayBlockEntity vendorHoloDisplayBlockEntity) {
		ServerPlayNetworking.send(
				player,
				new VendorHoloDisplayOpenPayload(
						vendorHoloDisplayBlockEntity.getBlockPos(),
						vendorHoloDisplayBlockEntity.getPackedLines(),
						vendorHoloDisplayBlockEntity.getPackedLineSizes(),
						vendorHoloDisplayBlockEntity.getBoardSize()
				)
		);
	}

	public static void openVendorNote(ServerPlayer player, VendorNoteBlockEntity vendorNoteBlockEntity) {
		boolean editable = vendorNoteBlockEntity.canManage(player);

		ServerPlayNetworking.send(
				player,
				new VendorNoteOpenPayload(
						vendorNoteBlockEntity.getBlockPos(),
						vendorNoteBlockEntity.getOwnerName(),
						vendorNoteBlockEntity.getTitleText(),
						vendorNoteBlockEntity.getBodyText(),
						editable
				)
		);
	}

	private static boolean isWithinInteractionDistance(ServerPlayer player, BlockPos pos) {
		double deltaX = player.getX() - (pos.getX() + 0.5D);
		double deltaY = player.getY() - (pos.getY() + 0.5D);
		double deltaZ = player.getZ() - (pos.getZ() + 0.5D);
		return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ <= MAX_INTERACTION_DISTANCE_SQUARED;
	}
}
