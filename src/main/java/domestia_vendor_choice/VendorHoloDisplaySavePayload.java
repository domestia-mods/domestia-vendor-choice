package domestia_vendor_choice;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record VendorHoloDisplaySavePayload(
		BlockPos pos,
		String lines,
		int lineSizes,
		int boardSize
) implements CustomPacketPayload {
	private static final int MAX_PACKED_LINES_LENGTH = VendorHoloDisplayBlockEntity.LINE_COUNT * VendorHoloDisplayBlockEntity.MAX_LINE_LENGTH
			+ VendorHoloDisplayBlockEntity.LINE_COUNT - 1;

	public static final Type<VendorHoloDisplaySavePayload> TYPE = new Type<>(
			Identifier.fromNamespaceAndPath(DomestiaVendorChoice.MOD_ID, "vendor_holo_display_save")
	);

	public static final StreamCodec<RegistryFriendlyByteBuf, VendorHoloDisplaySavePayload> CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC,
			VendorHoloDisplaySavePayload::pos,
			ByteBufCodecs.stringUtf8(MAX_PACKED_LINES_LENGTH),
			VendorHoloDisplaySavePayload::lines,
			ByteBufCodecs.VAR_INT,
			VendorHoloDisplaySavePayload::lineSizes,
			ByteBufCodecs.VAR_INT,
			VendorHoloDisplaySavePayload::boardSize,
			VendorHoloDisplaySavePayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
