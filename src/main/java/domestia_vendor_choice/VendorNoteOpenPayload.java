package domestia_vendor_choice;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record VendorNoteOpenPayload(
		BlockPos pos,
		String ownerName,
		String title,
		String body,
		boolean editable
) implements CustomPacketPayload {
	public static final Type<VendorNoteOpenPayload> TYPE = new Type<>(
			Identifier.fromNamespaceAndPath(DomestiaVendorChoice.MOD_ID, "vendor_note_open")
	);

	public static final StreamCodec<RegistryFriendlyByteBuf, VendorNoteOpenPayload> CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC,
			VendorNoteOpenPayload::pos,
			ByteBufCodecs.stringUtf8(VendorNoteBlockEntity.MAX_OWNER_NAME_LENGTH),
			VendorNoteOpenPayload::ownerName,
			ByteBufCodecs.stringUtf8(VendorNoteBlockEntity.MAX_TITLE_LENGTH),
			VendorNoteOpenPayload::title,
			ByteBufCodecs.stringUtf8(VendorNoteBlockEntity.MAX_BODY_LENGTH),
			VendorNoteOpenPayload::body,
			ByteBufCodecs.BOOL,
			VendorNoteOpenPayload::editable,
			VendorNoteOpenPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
