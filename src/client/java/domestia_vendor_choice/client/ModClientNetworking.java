package domestia_vendor_choice.client;

import domestia_vendor_choice.VendorNoteOpenPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

public final class ModClientNetworking {
	private ModClientNetworking() {
	}

	public static void initialize() {
		ClientPlayNetworking.registerGlobalReceiver(VendorNoteOpenPayload.TYPE, (payload, context) -> {
			Minecraft minecraft = context.client();
			minecraft.setScreen(new VendorNoteScreen(payload));
		});
	}
}
