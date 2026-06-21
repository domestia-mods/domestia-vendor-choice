package domestia_vendor_choice;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record VendorHoloDisplayOpenPayload(
		BlockPos pos,
		String lines,
		int lineSizes,
		int boardSize
) implements CustomPacketPayload {
	private static final int MAX_PACKED_LINES_LENGTH = VendorHoloDisplayBlockEntity.LINE_COUNT * VendorHoloDisplayBlockEntity.MAX_LINE_LENGTH
			+ VendorHoloDisplayBlockEntity.LINE_COUNT - 1;

	public static final Type<VendorHoloDisplayOpenPayload> TYPE = new Type<>(
			Identifier.fromNamespaceAndPath(DomestiaVendorChoice.MOD_ID, "vendor_holo_display_open")
	);

	public static final StreamCodec<RegistryFriendlyByteBuf, VendorHoloDisplayOpenPayload> CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC,
			VendorHoloDisplayOpenPayload::pos,
			ByteBufCodecs.stringUtf8(MAX_PACKED_LINES_LENGTH),
			VendorHoloDisplayOpenPayload::lines,
			ByteBufCodecs.VAR_INT,
			VendorHoloDisplayOpenPayload::lineSizes,
			ByteBufCodecs.VAR_INT,
			VendorHoloDisplayOpenPayload::boardSize,
			VendorHoloDisplayOpenPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
