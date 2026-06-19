package domestia_vendor_choice;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record VendorStandSavePayload(
		BlockPos pos,
		String title,
		String body
) implements CustomPacketPayload {
	public static final Type<VendorStandSavePayload> TYPE = new Type<>(
			Identifier.fromNamespaceAndPath(DomestiaVendorChoice.MOD_ID, "vendor_stand_save")
	);

	public static final StreamCodec<RegistryFriendlyByteBuf, VendorStandSavePayload> CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC,
			VendorStandSavePayload::pos,
			ByteBufCodecs.stringUtf8(VendorStandBlockEntity.MAX_TITLE_LENGTH),
			VendorStandSavePayload::title,
			ByteBufCodecs.stringUtf8(VendorStandBlockEntity.MAX_BODY_LENGTH),
			VendorStandSavePayload::body,
			VendorStandSavePayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
