package domestia_vendor_choice;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.UUID;

public class VendorHoloDisplayBlockEntity extends BlockEntity implements Container, WorldlyContainer {
	private static final String KEY_OWNER_UUID = "OwnerUuid";
	private static final String KEY_OWNER_NAME = "OwnerName";
	private static final String KEY_LINE_PREFIX = "Line";
	private static final String KEY_LINE_SIZE_PREFIX = "LineSize";
	private static final String KEY_BOARD_SIZE = "BoardSize";

	public static final String DEFAULT_OWNER_NAME_KEY = "display.domestia_vendor_choice.owner.fallback";
	public static final String DEFAULT_OWNER_NAME = "";

	public static final int DISPLAY_SLOT = 0;
	public static final int DYE_SLOT_START = 1;
	public static final int LINE_COUNT = 5;
	public static final int DYE_SLOT_COUNT = LINE_COUNT;
	public static final int HOLO_DISPLAY_SLOT_COUNT = 1 + DYE_SLOT_COUNT;

	public static final int FONT_SIZE_SMALL = 0;
	public static final int FONT_SIZE_NORMAL = 1;
	public static final int FONT_SIZE_LARGE = 2;
	public static final int FONT_SIZE_AUTO = 3;
	public static final int DEFAULT_FONT_SIZE = FONT_SIZE_NORMAL;

	public static final int BOARD_SIZE_MIN = 3;
	public static final int BOARD_SIZE_MAX = 15;
	public static final int BOARD_SIZE_STEP = 2;
	public static final int BOARD_SIZE_SMALL = BOARD_SIZE_MIN;
	public static final int BOARD_SIZE_MEDIUM = 5;
	public static final int BOARD_SIZE_LARGE = 7;
	public static final int DEFAULT_BOARD_SIZE = BOARD_SIZE_MEDIUM;

	public static final int MAX_OWNER_NAME_LENGTH = 64;
	public static final int MAX_LINE_LENGTH = 16;

	private static final int[] VANILLA_HOPPER_NO_SLOTS = new int[0];
	private static final double STILL_VALID_MAX_DISTANCE_SQUARED = 64.0D;

	private UUID ownerUuid;
	private String ownerName = DEFAULT_OWNER_NAME;
	private int boardSize = DEFAULT_BOARD_SIZE;
	private final String[] lines = new String[LINE_COUNT];
	private final int[] lineSizes = new int[LINE_COUNT];
	private final NonNullList<ItemStack> items = NonNullList.withSize(HOLO_DISPLAY_SLOT_COUNT, ItemStack.EMPTY);

	public VendorHoloDisplayBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.VENDOR_HOLO_DISPLAY, pos, state);

		for (int index = 0; index < LINE_COUNT; index++) {
			this.lines[index] = "";
			this.lineSizes[index] = DEFAULT_FONT_SIZE;
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);

		this.saveOwner(output);
		this.saveSettings(output);
		ContainerHelper.saveAllItems(output, this.items);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);

		this.loadOwner(input);
		this.loadSettings(input);
		this.clearItemsWithoutUpdate();
		ContainerHelper.loadAllItems(input, this.items);
		this.normalizeSlotCounts();
	}

	private void saveOwner(ValueOutput output) {
		if (this.ownerUuid != null) {
			output.putString(KEY_OWNER_UUID, this.ownerUuid.toString());
		}

		output.putString(KEY_OWNER_NAME, this.ownerName);
	}

	private void loadOwner(ValueInput input) {
		this.ownerUuid = parseUuid(input.getStringOr(KEY_OWNER_UUID, ""));
		this.ownerName = normalizeOwnerName(input.getStringOr(KEY_OWNER_NAME, DEFAULT_OWNER_NAME));
	}

	private void saveSettings(ValueOutput output) {
		output.putString(KEY_BOARD_SIZE, Integer.toString(this.boardSize));

		for (int index = 0; index < LINE_COUNT; index++) {
			output.putString(KEY_LINE_PREFIX + index, this.lines[index]);
			output.putString(KEY_LINE_SIZE_PREFIX + index, Integer.toString(this.lineSizes[index]));
		}
	}

	private void loadSettings(ValueInput input) {
		this.boardSize = normalizeBoardSize(parseInt(input.getStringOr(KEY_BOARD_SIZE, Integer.toString(DEFAULT_BOARD_SIZE)), DEFAULT_BOARD_SIZE));

		for (int index = 0; index < LINE_COUNT; index++) {
			this.lines[index] = normalizeLine(input.getStringOr(KEY_LINE_PREFIX + index, ""));
			this.lineSizes[index] = normalizeFontSize(parseInt(input.getStringOr(KEY_LINE_SIZE_PREFIX + index, Integer.toString(DEFAULT_FONT_SIZE)), DEFAULT_FONT_SIZE));
		}
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
		this.level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_ALL);
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		this.clearContent();
	}

	@Override
	public int getContainerSize() {
		return HOLO_DISPLAY_SLOT_COUNT;
	}

	@Override
	public boolean isEmpty() {
		for (ItemStack itemStack : this.items) {
			if (!itemStack.isEmpty()) {
				return false;
			}
		}

		return true;
	}

	@Override
	public ItemStack getItem(int slot) {
		if (!isValidSlot(slot)) {
			return ItemStack.EMPTY;
		}

		return this.items.get(slot);
	}

	@Override
	public ItemStack removeItem(int slot, int amount) {
		ItemStack removedStack = ContainerHelper.removeItem(this.items, slot, amount);

		if (!removedStack.isEmpty()) {
			this.setChanged();
		}

		return removedStack;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		ItemStack removedStack = ContainerHelper.takeItem(this.items, slot);

		if (!removedStack.isEmpty()) {
			this.setChanged();
		}

		return removedStack;
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		if (!isValidSlot(slot)) {
			return;
		}

		ItemStack normalizedStack = stack;

		if (normalizedStack.isEmpty()) {
			this.items.set(slot, ItemStack.EMPTY);
			this.setChanged();
			return;
		}

		if (!this.canPlaceItem(slot, normalizedStack)) {
			return;
		}

		normalizedStack.setCount(1);
		this.items.set(slot, normalizedStack);
		this.setChanged();
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		if (!isValidSlot(slot) || stack.isEmpty()) {
			return false;
		}

		if (slot == DISPLAY_SLOT) {
			return true;
		}

		return isDyeStack(stack);
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return VANILLA_HOPPER_NO_SLOTS;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
		return false;
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return false;
	}

	@Override
	public boolean stillValid(Player player) {
		if (this.level == null) {
			return false;
		}

		if (this.level.getBlockEntity(this.worldPosition) != this) {
			return false;
		}

		return player.distanceToSqr(
				this.worldPosition.getX() + 0.5D,
				this.worldPosition.getY() + 0.5D,
				this.worldPosition.getZ() + 0.5D
		) <= STILL_VALID_MAX_DISTANCE_SQUARED;
	}

	@Override
	public void clearContent() {
		this.clearItemsWithoutUpdate();
		this.setChanged();
	}

	private void clearItemsWithoutUpdate() {
		for (int slot = 0; slot < this.items.size(); slot++) {
			this.items.set(slot, ItemStack.EMPTY);
		}
	}

	private void normalizeSlotCounts() {
		for (int slot = 0; slot < this.items.size(); slot++) {
			ItemStack stack = this.items.get(slot);

			if (!stack.isEmpty() && stack.getCount() > 1) {
				stack.setCount(1);
			}

			if (isDyeSlot(slot) && !stack.isEmpty() && !isDyeStack(stack)) {
				this.items.set(slot, ItemStack.EMPTY);
			}
		}
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
		return normalizeOwnerName(player.getName().getString());
	}

	public boolean hasOwner() {
		return VendorAccess.hasOwner(this.ownerUuid);
	}

	public boolean isOwner(UUID playerUuid) {
		return VendorAccess.isOwner(this.ownerUuid, playerUuid);
	}

	public boolean canManage(Player player) {
		if (!VendorAccess.canManage(this.ownerUuid, player)) {
			return false;
		}

		this.updateOwnerNameFromPlayerIfNeeded(player);
		return true;
	}

	public boolean canBreak(Player player) {
		if (VendorAccess.canManage(this.ownerUuid, player)) {
			this.updateOwnerNameFromPlayerIfNeeded(player);
			return true;
		}

		return VendorAccess.isAdministrator(player);
	}

	public String getLine(int index) {
		if (!isValidLine(index)) {
			return "";
		}

		return this.lines[index];
	}

	public int getLineSize(int index) {
		if (!isValidLine(index)) {
			return DEFAULT_FONT_SIZE;
		}

		return this.lineSizes[index];
	}

	public int getBoardSize() {
		return this.boardSize;
	}

	public ItemStack getDisplayStack() {
		return this.items.get(DISPLAY_SLOT).copy();
	}

	public int getLineColor(int index) {
		if (!isValidLine(index)) {
			return dyeColorFromStack(ItemStack.EMPTY);
		}

		return dyeColorFromStack(this.items.get(DYE_SLOT_START + index));
	}

	public void updateSettings(Player player, String[] newLines, int[] newLineSizes, int newBoardSize) {
		if (!this.canManage(player)) {
			return;
		}

		for (int index = 0; index < LINE_COUNT; index++) {
			this.lines[index] = normalizeLine(index < newLines.length ? newLines[index] : "");
			this.lineSizes[index] = normalizeFontSize(index < newLineSizes.length ? newLineSizes[index] : DEFAULT_FONT_SIZE);
		}

		this.boardSize = normalizeBoardSize(newBoardSize);
		this.setChanged();
	}

	public String getPackedLines() {
		StringBuilder builder = new StringBuilder();

		for (int index = 0; index < LINE_COUNT; index++) {
			if (index > 0) {
				builder.append('\n');
			}

			builder.append(this.lines[index]);
		}

		return builder.toString();
	}

	public int getPackedLineSizes() {
		int packed = 0;

		for (int index = 0; index < LINE_COUNT; index++) {
			packed |= (normalizeFontSize(this.lineSizes[index]) & 3) << (index * 2);
		}

		return packed;
	}

	public static String[] unpackLines(String packedLines) {
		String[] result = new String[LINE_COUNT];
		String[] parts = packedLines == null ? new String[0] : packedLines.split("\n", -1);

		for (int index = 0; index < LINE_COUNT; index++) {
			result[index] = normalizeLine(index < parts.length ? parts[index] : "");
		}

		return result;
	}

	public static int[] unpackLineSizes(int packedLineSizes) {
		int[] result = new int[LINE_COUNT];

		for (int index = 0; index < LINE_COUNT; index++) {
			result[index] = normalizeFontSize((packedLineSizes >> (index * 2)) & 3);
		}

		return result;
	}

	public static int normalizeFontSize(int value) {
		return switch (value) {
			case FONT_SIZE_SMALL -> FONT_SIZE_SMALL;
			case FONT_SIZE_LARGE -> FONT_SIZE_LARGE;
			case FONT_SIZE_AUTO -> FONT_SIZE_AUTO;
			default -> FONT_SIZE_NORMAL;
		};
	}

	public static int normalizeBoardSize(int value) {
		int clamped = Math.max(BOARD_SIZE_MIN, Math.min(BOARD_SIZE_MAX, value));
		int index = Math.round((clamped - BOARD_SIZE_MIN) / (float) BOARD_SIZE_STEP);
		return BOARD_SIZE_MIN + index * BOARD_SIZE_STEP;
	}

	public static String normalizeLine(String value) {
		return normalizeSingleLine(value, MAX_LINE_LENGTH);
	}

	private static String normalizeOwnerName(String value) {
		String normalized = normalizeSingleLine(value, MAX_OWNER_NAME_LENGTH);
		return normalized.isBlank() ? DEFAULT_OWNER_NAME : normalized;
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

	private static int parseInt(String value, int fallback) {
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException exception) {
			return fallback;
		}
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

	public static boolean isDyeStack(ItemStack stack) {
		return stack.is(Items.WHITE_DYE)
				|| stack.is(Items.LIGHT_GRAY_DYE)
				|| stack.is(Items.GRAY_DYE)
				|| stack.is(Items.BLACK_DYE)
				|| stack.is(Items.BROWN_DYE)
				|| stack.is(Items.RED_DYE)
				|| stack.is(Items.ORANGE_DYE)
				|| stack.is(Items.YELLOW_DYE)
				|| stack.is(Items.LIME_DYE)
				|| stack.is(Items.GREEN_DYE)
				|| stack.is(Items.CYAN_DYE)
				|| stack.is(Items.LIGHT_BLUE_DYE)
				|| stack.is(Items.BLUE_DYE)
				|| stack.is(Items.PURPLE_DYE)
				|| stack.is(Items.MAGENTA_DYE)
				|| stack.is(Items.PINK_DYE);
	}

	public static int dyeColorFromStack(ItemStack stack) {
		if (stack.is(Items.BLACK_DYE)) {
			return 0xFF111111;
		}
		if (stack.is(Items.BLUE_DYE)) {
			return 0xFF5555FF;
		}
		if (stack.is(Items.BROWN_DYE)) {
			return 0xFFAA6633;
		}
		if (stack.is(Items.CYAN_DYE)) {
			return 0xFF00FFFF;
		}
		if (stack.is(Items.GRAY_DYE)) {
			return 0xFFAAAAAA;
		}
		if (stack.is(Items.GREEN_DYE)) {
			return 0xFF55FF55;
		}
		if (stack.is(Items.LIGHT_BLUE_DYE)) {
			return 0xFF55CCFF;
		}
		if (stack.is(Items.LIGHT_GRAY_DYE)) {
			return 0xFFDDDDDD;
		}
		if (stack.is(Items.LIME_DYE)) {
			return 0xFF99FF33;
		}
		if (stack.is(Items.MAGENTA_DYE)) {
			return 0xFFFF55FF;
		}
		if (stack.is(Items.ORANGE_DYE)) {
			return 0xFFFFAA00;
		}
		if (stack.is(Items.PINK_DYE)) {
			return 0xFFFF99CC;
		}
		if (stack.is(Items.PURPLE_DYE)) {
			return 0xFFAA55FF;
		}
		if (stack.is(Items.RED_DYE)) {
			return 0xFFFF5555;
		}
		if (stack.is(Items.YELLOW_DYE)) {
			return 0xFFFFFF55;
		}

		return 0xFFFFFFFF;
	}

	private static boolean isValidLine(int index) {
		return index >= 0 && index < LINE_COUNT;
	}

	private static boolean isValidSlot(int slot) {
		return slot >= 0 && slot < HOLO_DISPLAY_SLOT_COUNT;
	}

	private static boolean isDyeSlot(int slot) {
		return slot >= DYE_SLOT_START && slot < DYE_SLOT_START + DYE_SLOT_COUNT;
	}
}
