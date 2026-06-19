package domestia_vendor_choice;

import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * Shared ownership and administrator access checks for protected vendor blocks.
 */
public final class VendorAccess {
	private static final Permission ADMINISTRATOR_PERMISSION = Permissions.COMMANDS_GAMEMASTER;

	private VendorAccess() {
	}

	public static boolean hasOwner(UUID ownerUuid) {
		return ownerUuid != null;
	}

	public static boolean isOwner(UUID ownerUuid, UUID playerUuid) {
		return ownerUuid != null && ownerUuid.equals(playerUuid);
	}

	public static boolean canManage(UUID ownerUuid, Player player) {
		return isOwner(ownerUuid, player.getUUID());
	}

	public static boolean isAdministrator(Player player) {
		return player.permissions().hasPermission(ADMINISTRATOR_PERMISSION);
	}
}
