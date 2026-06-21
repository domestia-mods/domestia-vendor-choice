package domestia_vendor_choice.client;

import net.minecraft.client.resources.language.I18n;

final class LocalizedRenderText {
	private static final String ID_TRIM_SUFFIX = "display.domestia_vendor_choice.trim_suffix";

	private LocalizedRenderText() {
	}

	public static String resolve(String text, String fallbackTranslationKey) {
		if (text == null || text.isBlank()) {
			return I18n.get(fallbackTranslationKey);
		}

		return text;
	}

	public static String ellipsis() {
		return I18n.get(ID_TRIM_SUFFIX);
	}

	public static String trim(String text, int maxVisibleChars) {
		if (text == null || text.isBlank()) {
			return "";
		}

		if (text.length() <= maxVisibleChars) {
			return text;
		}

		return text.substring(0, maxVisibleChars) + I18n.get(ID_TRIM_SUFFIX);
	}
}
