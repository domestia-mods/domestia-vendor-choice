package domestia_vendor_choice;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VendorReceiverBlockEntity extends BlockEntity {
	private static final String KEY_OWNER_UUID = "OwnerUuid";
	private static final String KEY_OWNER_NAME = "OwnerName";

	public static final String DEFAULT_OWNER_NAME_KEY = "display.domestia_vendor_choice.owner.fallback";
	public static final String DEFAULT_OWNER_NAME = "";

	public static final int TICKS_DEPOSIT_COOLDOWN = 20;

	private UUID ownerUuid;
	private String ownerName = DEFAULT_OWNER_NAME;

	private final Map<UUID, Long> lastUseGameTimes = new HashMap<>();

	public VendorReceiverBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.VENDOR_RECEIVER, pos, state);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		this.saveOwner(output);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		this.loadOwner(input);
		this.lastUseGameTimes.clear();
	}

	private void saveOwner(ValueOutput output) {
		if (this.ownerUuid != null) {
			output.putString(KEY_OWNER_UUID, this.ownerUuid.toString());
		}

		output.putString(KEY_OWNER_NAME, this.ownerName);
	}

	private void loadOwner(ValueInput input) {
		String ownerUuidString = input.getStringOr(KEY_OWNER_UUID, "");

		if (ownerUuidString.isEmpty()) {
			this.ownerUuid = null;
		} else {
			try {
				this.ownerUuid = UUID.fromString(ownerUuidString);
			} catch (IllegalArgumentException exception) {
				this.ownerUuid = null;
			}
		}

		this.ownerName = normalizeStoredText(
				input.getStringOr(KEY_OWNER_NAME, DEFAULT_OWNER_NAME),
				DEFAULT_OWNER_NAME
		);
	}

	private static String normalizeStoredText(String value, String fallback) {
		if (value == null || value.isBlank()) {
			return fallback;
		}

		return value;
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registryLookup) {
		return this.saveWithoutMetadata(registryLookup);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public void setChanged() {
		super.setChanged();

		if (this.level == null) {
			return;
		}

		BlockState state = this.getBlockState();

		this.level.sendBlockUpdated(
				this.worldPosition,
				state,
				state,
				Block.UPDATE_ALL
		);
	}

	public boolean canUseNow(Player player, long gameTime) {
		Long lastUseGameTime = this.lastUseGameTimes.get(player.getUUID());

		if (lastUseGameTime == null) {
			return true;
		}

		return gameTime - lastUseGameTime >= TICKS_DEPOSIT_COOLDOWN;
	}

	public void markUsed(Player player, long gameTime) {
		this.lastUseGameTimes.put(player.getUUID(), gameTime);
	}

	public UUID getOwnerUuid() {
		return this.ownerUuid;
	}

	public String getOwnerName() {
		return this.ownerName;
	}

	public void setOwner(Player player) {
		this.ownerUuid = player.getUUID();
		this.ownerName = getPlayerName(player);
		this.setChanged();
	}

	public void setOwnerAndDisplayName(Player player, ItemStack placedStack) {
		this.ownerUuid = player.getUUID();
		this.ownerName = getPlayerName(player);
		this.setChanged();
	}

	private void updateOwnerNameFromPlayerIfNeeded(Player player) {
		if (!this.isOwner(player.getUUID())) {
			return;
		}

		if (this.ownerName != null && !this.ownerName.isBlank()) {
			return;
		}

		this.ownerName = getPlayerName(player);
		this.setChanged();
	}

	private static String getPlayerName(Player player) {
		return normalizeStoredText(player.getName().getString(), DEFAULT_OWNER_NAME);
	}

	public boolean hasOwner() {
		return VendorAccess.hasOwner(this.ownerUuid);
	}

	public boolean isOwner(UUID playerUuid) {
		return VendorAccess.isOwner(this.ownerUuid, playerUuid);
	}

	public boolean canBreak(Player player) {
		if (VendorAccess.canManage(this.ownerUuid, player)) {
			this.updateOwnerNameFromPlayerIfNeeded(player);
			return true;
		}

		return VendorAccess.isAdministrator(player);
	}
}
