package domestia_vendor_choice;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record VendorStandOpenPayload(
		BlockPos pos,
		String ownerName,
		String title,
		String body,
		boolean editable
) implements CustomPacketPayload {
	public static final Type<VendorStandOpenPayload> TYPE = new Type<>(
			Identifier.fromNamespaceAndPath(DomestiaVendorChoice.MOD_ID, "vendor_stand_open")
	);

	public static final StreamCodec<RegistryFriendlyByteBuf, VendorStandOpenPayload> CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC,
			VendorStandOpenPayload::pos,
			ByteBufCodecs.stringUtf8(VendorStandBlockEntity.MAX_OWNER_NAME_LENGTH),
			VendorStandOpenPayload::ownerName,
			ByteBufCodecs.stringUtf8(VendorStandBlockEntity.MAX_TITLE_LENGTH),
			VendorStandOpenPayload::title,
			ByteBufCodecs.stringUtf8(VendorStandBlockEntity.MAX_BODY_LENGTH),
			VendorStandOpenPayload::body,
			ByteBufCodecs.BOOL,
			VendorStandOpenPayload::editable,
			VendorStandOpenPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
