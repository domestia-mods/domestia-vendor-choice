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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import java.util.UUID;

public class VendorHopperBlockEntity extends BlockEntity implements Container, WorldlyContainer {
	// Persistent NBT keys.
	private static final String KEY_OWNER_UUID = "OwnerUuid";
	private static final String KEY_OWNER_NAME = "OwnerName";
	private static final String KEY_DISPLAY_NAME = "DisplayName";

	// Default label. Used when the block item was not renamed on an anvil.
	public static final String DEFAULT_DISPLAY_NAME = "Hopper";

	// Fallback text for legacy blocks that have OwnerUuid but no stored owner name.
	public static final String DEFAULT_OWNER_NAME = "Owner";

	// Hopper inventory layout.
	// Buffer slots are the working hopper inventory. Template slots are owner-managed filters.
	public static final int BUFFER_SLOT_START = 0;
	public static final int BUFFER_SLOT_COUNT = 5;

	public static final int TEMPLATE_SLOT_START = BUFFER_SLOT_START + BUFFER_SLOT_COUNT;
	public static final int TEMPLATE_SLOT_COUNT = 5;

	public static final int HOPPER_SLOT_COUNT = BUFFER_SLOT_COUNT + TEMPLATE_SLOT_COUNT;

	// Vanilla hopper exposure.
	// Vanilla hoppers may feed items into Vendor Hopper buffer slots, but they must never siphon private storage.
	private static final int[] VANILLA_HOPPER_INSERT_SLOTS = createSlotRange(BUFFER_SLOT_START, BUFFER_SLOT_COUNT);

	// Interaction constants.
	private static final double STILL_VALID_MAX_DISTANCE_SQUARED = 64.0;

	// Transfer constants. Vanilla hopper-like cadence: one item per cycle.
	// The local cooldown is decremented on the ticks between transfer attempts.
	// Setting the value to 7 produces an effective 8-tick cadence: transfer tick + 7 cooldown ticks.
	private static final int TICKS_TRANSFER_INTERVAL = 7;
	private static final int COUNT_TRANSFER_ITEMS_PER_CYCLE = 1;

	// Dropped item pickup area.
	// The scan covers the hopper body plus the input space above it.
	// A half-block horizontal margin catches fast item entities that are pushed onto a neighboring block edge,
	// while keeping the behavior local to the hopper instead of turning it into a wide-area vacuum collector.
	private static final double DROPPED_ITEM_PICKUP_HORIZONTAL_PADDING = 0.5D;

	private UUID ownerUuid;
	private String ownerName = DEFAULT_OWNER_NAME;
	private String displayName = DEFAULT_DISPLAY_NAME;

	private int transferCooldownTicks = 0;

	private final NonNullList<ItemStack> items = NonNullList.withSize(HOPPER_SLOT_COUNT, ItemStack.EMPTY);

	public VendorHopperBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.VENDOR_HOPPER, pos, state);
	}

	private static int[] createSlotRange(int firstSlot, int slotCount) {
		int[] slots = new int[slotCount];

		for (int index = 0; index < slotCount; index++) {
			slots[index] = firstSlot + index;
		}

		return slots;
	}

	public void serverTick() {
		if (this.level == null || this.level.isClientSide()) {
			return;
		}

		if (!this.hasOwner()) {
			return;
		}

		// Dropped items can be short-lived in the hopper input area when other blocks eject them with velocity.
		// Scan for them every tick, independently from the scheduled one-item transfer cooldown.
		this.pickUpDroppedItemsFromPickupArea();

		if (this.transferCooldownTicks > 0) {
			this.transferCooldownTicks--;
			return;
		}

		this.transferCooldownTicks = TICKS_TRANSFER_INTERVAL;

		if (this.transferOneCycle()) {
			this.setChanged();
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);

		this.saveOwner(output);
		this.saveDisplayName(output);
		ContainerHelper.saveAllItems(output, this.items);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);

		this.loadOwner(input);
		this.loadDisplayName(input);

		this.clearItemsWithoutUpdate();
		ContainerHelper.loadAllItems(input, this.items);
		this.normalizeTemplateSlotCounts();

		this.transferCooldownTicks = 0;
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

	private void saveDisplayName(ValueOutput output) {
		output.putString(KEY_DISPLAY_NAME, this.displayName);
	}

	private void loadDisplayName(ValueInput input) {
		this.displayName = normalizeStoredText(
				input.getStringOr(KEY_DISPLAY_NAME, DEFAULT_DISPLAY_NAME),
				DEFAULT_DISPLAY_NAME
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

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		// Privacy rule:
		// Do not call super here. The default BlockEntity behavior drops container contents.
		// Vendor Hopper must drop contents only through ModEvents owner-break logic.
		this.clearContent();
	}

	private boolean transferOneCycle() {
		// Push first to free internal space, then try one input action during the same cycle.
		// This preserves the one-item-per-operation rule while avoiding the old alternating
		// push-or-pull behavior that made through-routing noticeably slower than vanilla hoppers.
		// Dropped item pickup is handled every tick before the transfer cooldown check.
		boolean transferredAnyItem = false;

		if (this.pushOneItemToOutput()) {
			transferredAnyItem = true;
		}

		if (this.pullOneItemFromAbove()) {
			transferredAnyItem = true;
		}
		else if (!this.hasSourceContainerBlockAbove() && this.pullOneItemFromContainerEntityAbove()) {
			transferredAnyItem = true;
		}

		return transferredAnyItem;
	}

	private boolean pushOneItemToOutput() {
		Direction outputDirection = this.getOutputDirection();

		if (outputDirection == Direction.UP) {
			return false;
		}

		BlockPos targetPos = this.worldPosition.relative(outputDirection);
		BlockEntity targetBlockEntity = null;
		Container targetContainer = null;

		// Vanilla hoppers are allowed to feed Vendor Hopper, but Vendor Hopper must not feed vanilla hoppers.
		if (!this.isVanillaHopperBlockAt(targetPos)) {
			BlockEntity candidateBlockEntity = this.level.getBlockEntity(targetPos);

			if (candidateBlockEntity instanceof Container candidateContainer) {
				targetBlockEntity = candidateBlockEntity;
				targetContainer = candidateContainer;
			}
		}

		Direction targetSide = outputDirection.getOpposite();

		for (int sourceSlot = BUFFER_SLOT_START; sourceSlot < BUFFER_SLOT_START + BUFFER_SLOT_COUNT; sourceSlot++) {
			ItemStack sourceStack = this.items.get(sourceSlot);

			if (sourceStack.isEmpty()) {
				continue;
			}

			if (this.tryDivertOneItemDownwardByTemplate(sourceStack)) {
				if (sourceStack.isEmpty()) {
					this.items.set(sourceSlot, ItemStack.EMPTY);
				}

				return true;
			}

			if (targetBlockEntity == null || targetContainer == null) {
				continue;
			}

			ItemStack transferStack = sourceStack.copy();
			transferStack.setCount(COUNT_TRANSFER_ITEMS_PER_CYCLE);

			ItemStack remainingStack = this.insertIntoTargetContainer(targetBlockEntity, targetContainer, transferStack, targetSide);

			if (!remainingStack.isEmpty()) {
				continue;
			}

			sourceStack.shrink(COUNT_TRANSFER_ITEMS_PER_CYCLE);

			if (sourceStack.isEmpty()) {
				this.items.set(sourceSlot, ItemStack.EMPTY);
			}

			return true;
		}

		return false;
	}

	private boolean pullOneItemFromAbove() {
		if (!this.hasAnyFreeInternalSpace()) {
			return false;
		}

		BlockPos sourcePos = this.worldPosition.above();
		BlockEntity sourceBlockEntity = this.level.getBlockEntity(sourcePos);

		if (!(sourceBlockEntity instanceof Container sourceContainer)) {
			return false;
		}

		// Vendor Hopper stacks are output-driven. The upper hopper must decide whether an item
		// is diverted downward by an explicit lower template before its normal facing output.
		if (sourceBlockEntity instanceof VendorHopperBlockEntity) {
			return false;
		}

		ItemStack extractedStack = this.extractFromSourceContainer(
				sourceBlockEntity,
				sourceContainer,
				Direction.DOWN,
				COUNT_TRANSFER_ITEMS_PER_CYCLE
		);

		if (extractedStack.isEmpty()) {
			return false;
		}

		ItemStack remainingStack = this.insertIntoInternalInventory(extractedStack);

		if (!remainingStack.isEmpty()) {
			// This should not normally happen because insertion is pre-checked, but keep the transfer lossless.
			this.insertIntoTargetContainer(sourceBlockEntity, sourceContainer, remainingStack, Direction.DOWN);
			return false;
		}

		return true;
	}

	private boolean hasSourceContainerBlockAbove() {
		BlockEntity sourceBlockEntity = this.level.getBlockEntity(this.worldPosition.above());
		return sourceBlockEntity instanceof Container;
	}

	private boolean pullOneItemFromContainerEntityAbove() {
		if (!this.hasAnyFreeInternalSpace()) {
			return false;
		}

		for (Entity entity : this.level.getEntitiesOfClass(Entity.class, this.getPickupArea(), entity -> entity instanceof Container)) {
			if (!(entity instanceof Container sourceContainer)) {
				continue;
			}

			ItemStack extractedStack = this.extractFromGenericContainer(sourceContainer, COUNT_TRANSFER_ITEMS_PER_CYCLE);

			if (extractedStack.isEmpty()) {
				continue;
			}

			ItemStack remainingStack = this.insertIntoInternalInventory(extractedStack);

			if (!remainingStack.isEmpty()) {
				this.insertIntoGenericContainer(sourceContainer, remainingStack);
				return false;
			}

			return true;
		}

		return false;
	}

	private boolean pickUpDroppedItemsFromPickupArea() {
		if (!this.hasAnyFreeInternalSpace()) {
			return false;
		}

		boolean pickedUpAnyItem = false;

		for (ItemEntity itemEntity : this.level.getEntitiesOfClass(ItemEntity.class, this.getPickupArea(), itemEntity -> !itemEntity.getItem().isEmpty())) {
			if (this.pickUpDroppedItem(itemEntity)) {
				pickedUpAnyItem = true;

				if (!this.hasAnyFreeInternalSpace()) {
					break;
				}
			}
		}

		return pickedUpAnyItem;
	}

	public boolean tryPickUpCollidedItem(ItemEntity itemEntity) {
		if (this.level == null || this.level.isClientSide()) {
			return false;
		}

		if (!this.hasOwner()) {
			return false;
		}

		// Collision pickup must not be blocked by the scheduled transfer cooldown.
		// Fast item entities can pass through the hopper body and settle outside the normal scan area
		// before the next 8-tick transfer cycle. Keep the cooldown for scheduled transfers,
		// but allow the collision path to catch items at the moment they touch the hopper.
		return this.pickUpDroppedItem(itemEntity);
	}

	private boolean pickUpDroppedItem(ItemEntity itemEntity) {
		if (!this.hasAnyFreeInternalSpace()) {
			return false;
		}

		ItemStack itemStack = itemEntity.getItem();

		if (itemStack.isEmpty()) {
			return false;
		}

		ItemStack remainingStack = this.insertIntoInternalInventory(itemStack);

		if (remainingStack.getCount() == itemStack.getCount()) {
			return false;
		}

		if (remainingStack.isEmpty()) {
			itemEntity.discard();
		} else {
			itemEntity.setItem(remainingStack);
		}

		return true;
	}

	private AABB getPickupArea() {
		return new AABB(this.worldPosition)
				.expandTowards(0.0D, 1.0D, 0.0D)
				.inflate(DROPPED_ITEM_PICKUP_HORIZONTAL_PADDING, 0.0D, DROPPED_ITEM_PICKUP_HORIZONTAL_PADDING);
	}

	private boolean tryDivertOneItemDownwardByTemplate(ItemStack sourceStack) {
		if (this.level == null || sourceStack.isEmpty()) {
			return false;
		}

		BlockEntity lowerBlockEntity = this.level.getBlockEntity(this.worldPosition.below());

		if (!(lowerBlockEntity instanceof VendorHopperBlockEntity lowerVendorHopper)) {
			return false;
		}

		if (!this.canAccessTargetContainer(lowerVendorHopper)) {
			return false;
		}

		ItemStack transferStack = sourceStack.copy();
		transferStack.setCount(COUNT_TRANSFER_ITEMS_PER_CYCLE);

		ItemStack remainingStack = lowerVendorHopper.insertForDownwardTemplateDiversion(transferStack);

		if (!remainingStack.isEmpty()) {
			return false;
		}

		sourceStack.shrink(COUNT_TRANSFER_ITEMS_PER_CYCLE);
		return true;
	}

	private ItemStack insertIntoTargetContainer(
			BlockEntity targetBlockEntity,
			Container targetContainer,
			ItemStack sourceStack,
			Direction targetSide
	) {
		if (sourceStack.isEmpty()) {
			return ItemStack.EMPTY;
		}

		if (!this.canAccessTargetContainer(targetBlockEntity)) {
			return sourceStack;
		}

		if (targetBlockEntity instanceof VendorHopperBlockEntity vendorHopperBlockEntity) {
			return vendorHopperBlockEntity.insertForSecureTransfer(sourceStack);
		}

		if (targetBlockEntity instanceof VendorMachineBlockEntity vendorMachineBlockEntity) {
			return vendorMachineBlockEntity.insertStockForSecureTransfer(sourceStack);
		}

		ItemStack remainingStack = sourceStack.copy();

		if (targetContainer instanceof WorldlyContainer worldlyContainer && !this.isVendorContainer(targetBlockEntity)) {
			int[] slots = worldlyContainer.getSlotsForFace(targetSide);

			for (int slot : slots) {
				if (remainingStack.isEmpty()) {
					break;
				}

				if (!worldlyContainer.canPlaceItemThroughFace(slot, remainingStack, targetSide)) {
					continue;
				}

				remainingStack = this.insertIntoContainerSlot(targetContainer, slot, remainingStack);
			}
		} else {
			for (int slot = 0; slot < targetContainer.getContainerSize(); slot++) {
				if (remainingStack.isEmpty()) {
					break;
				}

				remainingStack = this.insertIntoContainerSlot(targetContainer, slot, remainingStack);
			}
		}

		if (remainingStack.getCount() != sourceStack.getCount()) {
			targetContainer.setChanged();
		}

		return remainingStack;
	}

	private ItemStack insertIntoContainerSlot(Container container, int slot, ItemStack remainingStack) {
		ItemStack targetStack = container.getItem(slot);

		if (targetStack.isEmpty()) {
			ItemStack insertedStack = remainingStack.copy();
			insertedStack.setCount(Math.min(remainingStack.getMaxStackSize(), remainingStack.getCount()));

			container.setItem(slot, insertedStack);
			remainingStack.shrink(insertedStack.getCount());

			return remainingStack;
		}

		if (!ItemStack.isSameItemSameComponents(targetStack, remainingStack)) {
			return remainingStack;
		}

		int freeSpace = Math.min(targetStack.getMaxStackSize(), container.getMaxStackSize()) - targetStack.getCount();

		if (freeSpace <= 0) {
			return remainingStack;
		}

		int insertedCount = Math.min(freeSpace, remainingStack.getCount());

		targetStack.grow(insertedCount);
		remainingStack.shrink(insertedCount);

		return remainingStack;
	}

	private ItemStack insertIntoGenericContainer(Container container, ItemStack sourceStack) {
		ItemStack remainingStack = sourceStack.copy();

		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			if (remainingStack.isEmpty()) {
				break;
			}

			remainingStack = this.insertIntoContainerSlot(container, slot, remainingStack);
		}

		if (remainingStack.getCount() != sourceStack.getCount()) {
			container.setChanged();
		}

		return remainingStack;
	}

	private ItemStack extractFromGenericContainer(Container container, int maxCount) {
		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			ItemStack extractedStack = this.extractFromContainerSlot(container, null, slot, Direction.DOWN, maxCount);

			if (!extractedStack.isEmpty()) {
				return extractedStack;
			}
		}

		return ItemStack.EMPTY;
	}

	private ItemStack extractFromSourceContainer(
			BlockEntity sourceBlockEntity,
			Container sourceContainer,
			Direction sourceSide,
			int maxCount
	) {
		if (!this.canAccessSourceContainer(sourceBlockEntity)) {
			return ItemStack.EMPTY;
		}

		if (sourceBlockEntity instanceof VendorHopperBlockEntity vendorHopperBlockEntity) {
			return vendorHopperBlockEntity.extractBufferForSecureTransfer(maxCount);
		}

		if (sourceBlockEntity instanceof VendorMachineBlockEntity vendorMachineBlockEntity) {
			return vendorMachineBlockEntity.extractVaultForSecureTransfer(maxCount);
		}

		if (sourceContainer instanceof WorldlyContainer worldlyContainer && !this.isVendorContainer(sourceBlockEntity)) {
			int[] slots = worldlyContainer.getSlotsForFace(sourceSide);

			for (int slot : slots) {
				ItemStack extractedStack = this.extractFromContainerSlot(sourceContainer, worldlyContainer, slot, sourceSide, maxCount);

				if (!extractedStack.isEmpty()) {
					return extractedStack;
				}
			}
		} else {
			for (int slot = 0; slot < sourceContainer.getContainerSize(); slot++) {
				ItemStack extractedStack = this.extractFromContainerSlot(sourceContainer, null, slot, sourceSide, maxCount);

				if (!extractedStack.isEmpty()) {
					return extractedStack;
				}
			}
		}

		return ItemStack.EMPTY;
	}

	private ItemStack extractFromContainerSlot(
			Container container,
			WorldlyContainer worldlyContainer,
			int slot,
			Direction sourceSide,
			int maxCount
	) {
		ItemStack sourceStack = container.getItem(slot);

		if (sourceStack.isEmpty()) {
			return ItemStack.EMPTY;
		}

		if (worldlyContainer != null && !worldlyContainer.canTakeItemThroughFace(slot, sourceStack, sourceSide)) {
			return ItemStack.EMPTY;
		}

		ItemStack extractedStack = sourceStack.copy();
		extractedStack.setCount(Math.min(maxCount, sourceStack.getCount()));

		sourceStack.shrink(extractedStack.getCount());

		if (sourceStack.isEmpty()) {
			container.setItem(slot, ItemStack.EMPTY);
		}

		container.setChanged();

		return extractedStack;
	}

	public ItemStack insertForSecureTransfer(ItemStack sourceStack) {
		return this.insertIntoInternalInventory(sourceStack);
	}

	private ItemStack insertForDownwardTemplateDiversion(ItemStack sourceStack) {
		ItemStack remainingStack = sourceStack.copy();

		// Downward sorting diversion accepts only explicit Template matches.
		// Empty Template lanes are intentionally ignored here, unlike general hopper input.
		this.insertIntoMatchingTemplateBuffers(remainingStack);

		if (remainingStack.getCount() != sourceStack.getCount()) {
			this.setChanged();
		}

		return remainingStack;
	}

	public ItemStack extractBufferForSecureTransfer(int maxCount) {
		for (int slot = BUFFER_SLOT_START; slot < BUFFER_SLOT_START + BUFFER_SLOT_COUNT; slot++) {
			ItemStack extractedStack = this.extractFromContainerSlot(this, null, slot, Direction.DOWN, maxCount);

			if (!extractedStack.isEmpty()) {
				return extractedStack;
			}
		}

		return ItemStack.EMPTY;
	}

	private ItemStack insertIntoInternalInventory(ItemStack sourceStack) {
		ItemStack remainingStack = sourceStack.copy();

		this.insertIntoMatchingTemplateBuffers(remainingStack);
		this.insertIntoUnfilteredBuffers(remainingStack);

		if (remainingStack.getCount() != sourceStack.getCount()) {
			this.setChanged();
		}

		return remainingStack;
	}

	private void insertIntoMatchingTemplateBuffers(ItemStack remainingStack) {
		for (int templateIndex = 0; templateIndex < TEMPLATE_SLOT_COUNT; templateIndex++) {
			if (remainingStack.isEmpty()) {
				return;
			}

			int templateSlot = TEMPLATE_SLOT_START + templateIndex;
			ItemStack templateStack = this.items.get(templateSlot);

			if (templateStack.isEmpty()) {
				continue;
			}

			if (!ItemStack.isSameItemSameComponents(templateStack, remainingStack)) {
				continue;
			}

			int bufferSlot = BUFFER_SLOT_START + templateIndex;
			this.insertIntoBufferSlot(bufferSlot, remainingStack);
		}
	}

	private void insertIntoUnfilteredBuffers(ItemStack remainingStack) {
		for (int templateIndex = 0; templateIndex < TEMPLATE_SLOT_COUNT; templateIndex++) {
			if (remainingStack.isEmpty()) {
				return;
			}

			int templateSlot = TEMPLATE_SLOT_START + templateIndex;
			ItemStack templateStack = this.items.get(templateSlot);

			if (!templateStack.isEmpty()) {
				continue;
			}

			int bufferSlot = BUFFER_SLOT_START + templateIndex;
			this.insertIntoBufferSlot(bufferSlot, remainingStack);
		}
	}

	private void insertIntoBufferSlot(int bufferSlot, ItemStack remainingStack) {
		if (!this.canInsertIntoBufferSlot(bufferSlot, remainingStack)) {
			return;
		}

		ItemStack bufferStack = this.items.get(bufferSlot);

		if (bufferStack.isEmpty()) {
			ItemStack insertedStack = remainingStack.copy();
			insertedStack.setCount(Math.min(remainingStack.getMaxStackSize(), remainingStack.getCount()));

			this.items.set(bufferSlot, insertedStack);
			remainingStack.shrink(insertedStack.getCount());
			return;
		}

		if (!ItemStack.isSameItemSameComponents(bufferStack, remainingStack)) {
			return;
		}

		int freeSpace = Math.min(bufferStack.getMaxStackSize(), this.getMaxStackSize()) - bufferStack.getCount();

		if (freeSpace <= 0) {
			return;
		}

		int insertedCount = Math.min(freeSpace, remainingStack.getCount());

		bufferStack.grow(insertedCount);
		remainingStack.shrink(insertedCount);
	}

	private boolean canInsertIntoBufferSlot(int bufferSlot, ItemStack stack) {
		if (!this.isBufferSlot(bufferSlot) || stack.isEmpty()) {
			return false;
		}

		int templateSlot = TEMPLATE_SLOT_START + (bufferSlot - BUFFER_SLOT_START);
		ItemStack templateStack = this.items.get(templateSlot);

		if (!templateStack.isEmpty() && !ItemStack.isSameItemSameComponents(templateStack, stack)) {
			return false;
		}

		ItemStack bufferStack = this.items.get(bufferSlot);

		if (bufferStack.isEmpty()) {
			return true;
		}

		if (!ItemStack.isSameItemSameComponents(bufferStack, stack)) {
			return false;
		}

		return bufferStack.getCount() < Math.min(bufferStack.getMaxStackSize(), this.getMaxStackSize());
	}

	private boolean hasAnyFreeInternalSpace() {
		for (int slot = BUFFER_SLOT_START; slot < BUFFER_SLOT_START + BUFFER_SLOT_COUNT; slot++) {
			ItemStack stack = this.items.get(slot);

			if (stack.isEmpty()) {
				return true;
			}

			if (stack.getCount() < Math.min(stack.getMaxStackSize(), this.getMaxStackSize())) {
				return true;
			}
		}

		return false;
	}

	private boolean canAccessSourceContainer(BlockEntity sourceBlockEntity) {
		return this.canAccessNeighborContainer(sourceBlockEntity);
	}

	private boolean canAccessTargetContainer(BlockEntity targetBlockEntity) {
		return this.canAccessNeighborContainer(targetBlockEntity);
	}

	private boolean canAccessNeighborContainer(BlockEntity blockEntity) {
		if (this.isVendorContainer(blockEntity)) {
			return this.isSameOwnerVendorContainer(blockEntity);
		}

		return true;
	}

	private boolean isVendorContainer(BlockEntity blockEntity) {
		return blockEntity instanceof VendorMachineBlockEntity
				|| blockEntity instanceof VendorSafeBlockEntity
				|| blockEntity instanceof VendorHopperBlockEntity;
	}

	private boolean isSameOwnerVendorContainer(BlockEntity blockEntity) {
		if (this.ownerUuid == null) {
			return false;
		}

		if (blockEntity instanceof VendorMachineBlockEntity vendorMachineBlockEntity) {
			return vendorMachineBlockEntity.isOwner(this.ownerUuid);
		}

		if (blockEntity instanceof VendorSafeBlockEntity vendorSafeBlockEntity) {
			return vendorSafeBlockEntity.isOwner(this.ownerUuid);
		}

		if (blockEntity instanceof VendorHopperBlockEntity vendorHopperBlockEntity) {
			return vendorHopperBlockEntity.isOwner(this.ownerUuid);
		}

		return false;
	}

	private boolean isVanillaHopperBlockAt(BlockPos pos) {
		return this.level != null && this.level.getBlockState(pos).is(Blocks.HOPPER);
	}

	private Direction getOutputDirection() {
		BlockState state = this.getBlockState();

		if (state.hasProperty(VendorHopperBlock.FACING)) {
			return state.getValue(VendorHopperBlock.FACING);
		}

		return Direction.DOWN;
	}

	@Override
	public int getContainerSize() {
		return HOPPER_SLOT_COUNT;
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
		if (this.isTemplateSlot(slot) && stack.getCount() > 1) {
			stack.setCount(1);
		} else if (stack.getCount() > this.getMaxStackSize()) {
			stack.setCount(this.getMaxStackSize());
		}

		this.items.set(slot, stack);
		this.setChanged();
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		if (this.isTemplateSlot(slot)) {
			return true;
		}

		return this.canInsertIntoBufferSlot(slot, stack);
	}

	// Vanilla hoppers may insert into Vendor Hopper as one-way feeder input.
	// They must never extract from Vendor Hopper, so private logistics cannot be siphoned.
	@Override
	public int[] getSlotsForFace(Direction side) {
		return VANILLA_HOPPER_INSERT_SLOTS;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
		return this.canInsertIntoBufferSlot(slot, stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return false;
	}

	private boolean isBufferSlot(int slot) {
		return slot >= BUFFER_SLOT_START && slot < BUFFER_SLOT_START + BUFFER_SLOT_COUNT;
	}

	private boolean isTemplateSlot(int slot) {
		return slot >= TEMPLATE_SLOT_START && slot < TEMPLATE_SLOT_START + TEMPLATE_SLOT_COUNT;
	}

	private void normalizeTemplateSlotCounts() {
		for (int slot = TEMPLATE_SLOT_START; slot < TEMPLATE_SLOT_START + TEMPLATE_SLOT_COUNT; slot++) {
			ItemStack stack = this.items.get(slot);

			if (!stack.isEmpty() && stack.getCount() > 1) {
				stack.setCount(1);
			}
		}
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
				this.worldPosition.getX() + 0.5,
				this.worldPosition.getY() + 0.5,
				this.worldPosition.getZ() + 0.5
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

	public UUID getOwnerUuid() {
		return this.ownerUuid;
	}

	public String getOwnerName() {
		return this.ownerName;
	}

	public String getDisplayName() {
		return this.displayName;
	}

	public void setOwnerUuid(UUID ownerUuid) {
		this.ownerUuid = ownerUuid;
		this.setChanged();
	}

	public void setOwner(Player player) {
		this.ownerUuid = player.getUUID();
		this.ownerName = getPlayerName(player);
		this.setChanged();
	}

	public void setOwnerAndDisplayName(Player player, ItemStack placedStack) {
		this.ownerUuid = player.getUUID();
		this.ownerName = getPlayerName(player);
		this.displayName = extractCustomDisplayName(placedStack);
		this.setChanged();
	}

	private void updateOwnerNameFromPlayerIfNeeded(Player player) {
		if (!this.isOwner(player.getUUID())) {
			return;
		}

		if (!DEFAULT_OWNER_NAME.equals(this.ownerName)) {
			return;
		}

		this.ownerName = getPlayerName(player);
		this.setChanged();
	}

	private static String getPlayerName(Player player) {
		return normalizeStoredText(player.getName().getString(), DEFAULT_OWNER_NAME);
	}

	private static String extractCustomDisplayName(ItemStack stack) {
		Component customName = stack.get(DataComponents.CUSTOM_NAME);

		if (customName == null) {
			return DEFAULT_DISPLAY_NAME;
		}

		return normalizeStoredText(customName.getString(), DEFAULT_DISPLAY_NAME);
	}

	public boolean hasOwner() {
		return VendorAccess.hasOwner(this.ownerUuid);
	}

	public boolean isOwner(UUID playerUuid) {
		return VendorAccess.isOwner(this.ownerUuid, playerUuid);
	}

	// Management means opening the private hopper container.
	// Only the owner can access the contents.
	public boolean canManage(Player player) {
		if (!VendorAccess.canManage(this.ownerUuid, player)) {
			return false;
		}

		this.updateOwnerNameFromPlayerIfNeeded(player);
		return true;
	}

	// Breaking is administrative recovery.
	// The owner can break the hopper, and administrators can destroy it if needed.
	public boolean canBreak(Player player) {
		if (VendorAccess.canManage(this.ownerUuid, player)) {
			this.updateOwnerNameFromPlayerIfNeeded(player);
			return true;
		}

		return VendorAccess.isAdministrator(player);
	}
}
