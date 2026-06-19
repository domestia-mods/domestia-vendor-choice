package domestia_vendor_choice;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.UUID;

public class VendorStandBlockEntity extends BlockEntity {
	private static final String KEY_OWNER_UUID = "OwnerUuid";
	private static final String KEY_OWNER_NAME = "OwnerName";
	private static final String KEY_TITLE = "Title";
	private static final String KEY_BODY = "Body";

	public static final String DEFAULT_OWNER_NAME = "Owner";
	public static final int MAX_OWNER_NAME_LENGTH = 64;
	public static final int MAX_TITLE_LENGTH = 32;
	public static final int MAX_BODY_LENGTH = 4096;

	private static final Permission ADMINISTRATOR_PERMISSION = Permissions.COMMANDS_GAMEMASTER;

	private UUID ownerUuid;
	private String ownerName = DEFAULT_OWNER_NAME;
	private String title = "";
	private String body = "";

	public VendorStandBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.VENDOR_STAND, pos, state);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);

		if (this.ownerUuid != null) {
			output.putString(KEY_OWNER_UUID, this.ownerUuid.toString());
		}

		output.putString(KEY_OWNER_NAME, this.ownerName);
		output.putString(KEY_TITLE, this.title);
		output.putString(KEY_BODY, this.body);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);

		this.ownerUuid = parseUuid(input.getStringOr(KEY_OWNER_UUID, ""));
		this.ownerName = normalizeOwnerName(input.getStringOr(KEY_OWNER_NAME, DEFAULT_OWNER_NAME));
		this.title = normalizeTitle(input.getStringOr(KEY_TITLE, ""));
		this.body = normalizeBody(input.getStringOr(KEY_BODY, ""));
	}

	@Override
	public net.minecraft.nbt.CompoundTag getUpdateTag(HolderLookup.Provider registryLookup) {
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
		this.level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_ALL);
	}

	public void initializeFromPlacedItem(Player placer, ItemStack stack) {
		VendorStandData storedData = stack.getOrDefault(ModDataComponents.VENDOR_STAND_DATA, VendorStandData.EMPTY);

		if (storedData.hasOwner()) {
			this.applyData(storedData);
			return;
		}

		this.ownerUuid = placer.getUUID();
		this.ownerName = normalizeOwnerName(placer.getName().getString());
		this.title = "";
		this.body = "";
		this.setChanged();
	}

	private void applyData(VendorStandData data) {
		this.ownerUuid = data.ownerUuidValue();
		this.ownerName = normalizeOwnerName(data.ownerName());
		this.title = normalizeTitle(data.title());
		this.body = normalizeBody(data.body());
		this.setChanged();
	}

	public ItemStack createPreservedItemStack() {
		ItemStack stack = new ItemStack(ModBlocks.VENDOR_STAND);
		stack.set(ModDataComponents.VENDOR_STAND_DATA, this.toData());
		return stack;
	}

	public VendorStandData toData() {
		return new VendorStandData(
				this.ownerUuid == null ? "" : this.ownerUuid.toString(),
				this.ownerName,
				this.title,
				this.body
		);
	}

	public void updateText(Player player, String newTitle, String newBody) {
		if (!this.canManage(player)) {
			return;
		}

		this.title = normalizeTitle(newTitle);
		this.body = normalizeBody(newBody);
		this.setChanged();
	}

	public boolean hasOwner() {
		return this.ownerUuid != null;
	}

	public boolean isOwner(UUID playerUuid) {
		return this.ownerUuid != null && this.ownerUuid.equals(playerUuid);
	}

	public boolean canManage(Player player) {
		if (!this.isOwner(player.getUUID())) {
			return false;
		}

		this.updateOwnerNameFromPlayerIfNeeded(player);
		return true;
	}

	public boolean canBreak(Player player) {
		if (this.isOwner(player.getUUID())) {
			this.updateOwnerNameFromPlayerIfNeeded(player);
			return true;
		}

		return player.permissions().hasPermission(ADMINISTRATOR_PERMISSION);
	}

	private void updateOwnerNameFromPlayerIfNeeded(Player player) {
		String currentPlayerName = normalizeOwnerName(player.getName().getString());

		if (currentPlayerName.equals(this.ownerName)) {
			return;
		}

		this.ownerName = currentPlayerName;
		this.setChanged();
	}

	public String getOwnerName() {
		return this.ownerName;
	}

	public String getTitleText() {
		return this.title;
	}

	public String getBodyText() {
		return this.body;
	}

	private static UUID parseUuid(String value) {
		if (value == null || value.isEmpty()) {
			return null;
		}

		try {
			return UUID.fromString(value);
		} catch (IllegalArgumentException exception) {
			return null;
		}
	}

	private static String normalizeOwnerName(String value) {
		String normalized = normalizeSingleLine(value, MAX_OWNER_NAME_LENGTH);
		return normalized.isBlank() ? DEFAULT_OWNER_NAME : normalized;
	}

	public static String normalizeTitle(String value) {
		return normalizeSingleLine(value, MAX_TITLE_LENGTH);
	}

	public static String normalizeBody(String value) {
		if (value == null || value.isEmpty()) {
			return "";
		}

		String normalized = value.replace("\r\n", "\n").replace('\r', '\n');
		return truncate(normalized, MAX_BODY_LENGTH);
	}

	private static String normalizeSingleLine(String value, int maxLength) {
		if (value == null || value.isEmpty()) {
			return "";
		}

		String normalized = value.replace('\r', ' ').replace('\n', ' ');
		return truncate(normalized, maxLength);
	}

	private static String truncate(String value, int maxLength) {
		if (value.length() <= maxLength) {
			return value;
		}

		return value.substring(0, maxLength);
	}
}
