package domestia_vendor_choice;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Explicit public deposit endpoint for Vendor Receiver.
 *
 * This is intentionally not a general container access contract: the target block controls
 * exactly which internal slots, if any, can accept public one-item deposits.
 */
public interface VendorPublicDepositTarget {
	ItemStack insertForPublicDeposit(ItemStack sourceStack, Player sender);
}
