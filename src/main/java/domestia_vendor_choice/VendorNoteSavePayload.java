package domestia_vendor_choice;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record VendorNoteSavePayload(
		BlockPos pos,
		String title,
		String body
) implements CustomPacketPayload {
	public static final Type<VendorNoteSavePayload> TYPE = new Type<>(
			Identifier.fromNamespaceAndPath(DomestiaVendorChoice.MOD_ID, "vendor_note_save")
	);

	public static final StreamCodec<RegistryFriendlyByteBuf, VendorNoteSavePayload> CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC,
			VendorNoteSavePayload::pos,
			ByteBufCodecs.stringUtf8(VendorNoteBlockEntity.MAX_TITLE_LENGTH),
			VendorNoteSavePayload::title,
			ByteBufCodecs.stringUtf8(VendorNoteBlockEntity.MAX_BODY_LENGTH),
			VendorNoteSavePayload::body,
			VendorNoteSavePayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
