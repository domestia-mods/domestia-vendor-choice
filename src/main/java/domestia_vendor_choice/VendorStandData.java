package domestia_vendor_choice;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.UUID;

public record VendorStandData(
		String ownerUuid,
		String ownerName,
		String title,
		String body
) {
	public static final VendorStandData EMPTY = new VendorStandData("", "", "", "");

	public static final Codec<VendorStandData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.optionalFieldOf("owner_uuid", "").forGetter(VendorStandData::ownerUuid),
			Codec.STRING.optionalFieldOf("owner_name", "").forGetter(VendorStandData::ownerName),
			Codec.STRING.optionalFieldOf("title", "").forGetter(VendorStandData::title),
			Codec.STRING.optionalFieldOf("body", "").forGetter(VendorStandData::body)
	).apply(instance, VendorStandData::new));

	public VendorStandData {
		ownerUuid = normalizeSingleLine(ownerUuid, 64);
		ownerName = normalizeSingleLine(ownerName, VendorStandBlockEntity.MAX_OWNER_NAME_LENGTH);
		title = normalizeSingleLine(title, VendorStandBlockEntity.MAX_TITLE_LENGTH);
		body = normalizeBody(body, VendorStandBlockEntity.MAX_BODY_LENGTH);
	}

	public boolean hasOwner() {
		return this.ownerUuidValue() != null;
	}

	public UUID ownerUuidValue() {
		if (this.ownerUuid.isEmpty()) {
			return null;
		}

		try {
			return UUID.fromString(this.ownerUuid);
		} catch (IllegalArgumentException exception) {
			return null;
		}
	}

	private static String normalizeSingleLine(String value, int maxLength) {
		if (value == null || value.isEmpty()) {
			return "";
		}

		String normalized = value.replace('\r', ' ').replace('\n', ' ');
		return truncate(normalized, maxLength);
	}

	private static String normalizeBody(String value, int maxLength) {
		if (value == null || value.isEmpty()) {
			return "";
		}

		String normalized = value.replace("\r\n", "\n").replace('\r', '\n');
		return truncate(normalized, maxLength);
	}

	private static String truncate(String value, int maxLength) {
		if (value.length() <= maxLength) {
			return value;
		}

		return value.substring(0, maxLength);
	}
}
