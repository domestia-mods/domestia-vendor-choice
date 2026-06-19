package domestia_vendor_choice;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.UUID;

public record VendorNoteData(
		String ownerUuid,
		String ownerName,
		String title,
		String body
) {
	public static final VendorNoteData EMPTY = new VendorNoteData("", "", "", "");

	public static final Codec<VendorNoteData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.optionalFieldOf("owner_uuid", "").forGetter(VendorNoteData::ownerUuid),
			Codec.STRING.optionalFieldOf("owner_name", "").forGetter(VendorNoteData::ownerName),
			Codec.STRING.optionalFieldOf("title", "").forGetter(VendorNoteData::title),
			Codec.STRING.optionalFieldOf("body", "").forGetter(VendorNoteData::body)
	).apply(instance, VendorNoteData::new));

	public VendorNoteData {
		ownerUuid = normalizeSingleLine(ownerUuid, 64);
		ownerName = normalizeSingleLine(ownerName, VendorNoteBlockEntity.MAX_OWNER_NAME_LENGTH);
		title = normalizeSingleLine(title, VendorNoteBlockEntity.MAX_TITLE_LENGTH);
		body = normalizeBody(body, VendorNoteBlockEntity.MAX_BODY_LENGTH);
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
