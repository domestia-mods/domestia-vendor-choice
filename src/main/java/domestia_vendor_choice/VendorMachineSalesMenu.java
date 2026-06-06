package domestia_vendor_choice;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class VendorMachineSalesMenu extends AbstractContainerMenu {
	public static final int ID_CHECKOUT_BUTTON_BASE = 1000;
	private static final int ID_CHECKOUT_QUANTITY_MULTIPLIER = 100;

	private static final int SIZE_SLOT = 18;

	private static final int INDEX_STOCK_DISPLAY_START = 0;
	private static final int INDEX_PRICE_DISPLAY_START = INDEX_STOCK_DISPLAY_START + VendorMachineBlockEntity.STOCK_SLOT_COUNT;
	private static final int INDEX_VAULT_DISPLAY_START = INDEX_PRICE_DISPLAY_START + VendorMachineBlockEntity.PRICE_SLOT_COUNT;

	private static final int SIZE_DISPLAY_SLOT_COUNT =
			VendorMachineBlockEntity.STOCK_SLOT_COUNT
					+ VendorMachineBlockEntity.PRICE_SLOT_COUNT
					+ VendorMachineBlockEntity.VAULT_SLOT_COUNT;

	private static final int POS_HIDDEN_SLOT_X = -2000;
	private static final int POS_HIDDEN_SLOT_Y = -2000;

	private static final int INDEX_PAYMENT_SLOT = 0;
	private static final int POS_PAYMENT_SLOT_X = 98;
	private static final int POS_PAYMENT_SLOT_Y = 70;

	private static final int POS_PLAYER_INVENTORY_X = 8;
	private static final int POS_PLAYER_INVENTORY_Y = 139;
	private static final int POS_PLAYER_HOTBAR_Y = 197;

	private static final int LAYOUT_PLAYER_INVENTORY_COLUMNS = 9;
	private static final int LAYOUT_PLAYER_INVENTORY_ROWS = 3;
	private static final int INDEX_PLAYER_HOTBAR_START = 0;
	private static final int INDEX_PLAYER_MAIN_INVENTORY_START = 9;

	private static final int MENU_HIDDEN_DISPLAY_START = 0;
	private static final int MENU_HIDDEN_DISPLAY_END = MENU_HIDDEN_DISPLAY_START + SIZE_DISPLAY_SLOT_COUNT;

	private static final int MENU_PAYMENT_SLOT = MENU_HIDDEN_DISPLAY_END;
	private static final int MENU_PAYMENT_START = MENU_PAYMENT_SLOT;
	private static final int MENU_PAYMENT_END = MENU_PAYMENT_START + 1;

	private static final int MENU_PLAYER_MAIN_START = MENU_PAYMENT_END;
	private static final int MENU_PLAYER_MAIN_END = MENU_PLAYER_MAIN_START + LAYOUT_PLAYER_INVENTORY_COLUMNS * LAYOUT_PLAYER_INVENTORY_ROWS;

	private static final int MENU_PLAYER_HOTBAR_START = MENU_PLAYER_MAIN_END;
	private static final int MENU_PLAYER_HOTBAR_END = MENU_PLAYER_HOTBAR_START + LAYOUT_PLAYER_INVENTORY_COLUMNS;

	private static final int MENU_PLAYER_INVENTORY_START = MENU_PLAYER_MAIN_START;
	private static final int MENU_PLAYER_INVENTORY_END = MENU_PLAYER_HOTBAR_END;

	private final VendorMachineBlockEntity vendorMachineBlockEntity;
	private final SimpleContainer displayItems = new SimpleContainer(SIZE_DISPLAY_SLOT_COUNT);
	private final SimpleContainer paymentContainer = new SimpleContainer(1);

	public VendorMachineSalesMenu(int containerId, Inventory playerInventory) {
		this(containerId, playerInventory, null);
	}

	public VendorMachineSalesMenu(int containerId, Inventory playerInventory, VendorMachineBlockEntity vendorMachineBlockEntity) {
		super(ModMenus.VENDOR_MACHINE_SALES, containerId);

		this.vendorMachineBlockEntity = vendorMachineBlockEntity;

		if (this.vendorMachineBlockEntity != null) {
			this.vendorMachineBlockEntity.registerSalesMenuOpened();
		}

		this.syncDisplayItemsFromVendor();

		this.addHiddenDisplaySlots();
		this.addPaymentSlot();
		this.addPlayerInventorySlots(playerInventory);
	}

	public static int createCheckoutButtonId(int productIndex, int quantity) {
		return ID_CHECKOUT_BUTTON_BASE + productIndex * ID_CHECKOUT_QUANTITY_MULTIPLIER + quantity;
	}

	private static int getCheckoutProductIndex(int buttonId) {
		return (buttonId - ID_CHECKOUT_BUTTON_BASE) / ID_CHECKOUT_QUANTITY_MULTIPLIER;
	}

	private static int getCheckoutQuantity(int buttonId) {
		return (buttonId - ID_CHECKOUT_BUTTON_BASE) % ID_CHECKOUT_QUANTITY_MULTIPLIER;
	}

	@Override
	public void broadcastChanges() {
		this.syncDisplayItemsFromVendor();
		super.broadcastChanges();
	}

	private void syncDisplayItemsFromVendor() {
		if (this.vendorMachineBlockEntity == null) {
			return;
		}

		Container vendorInventory = this.vendorMachineBlockEntity.getInventory();

		this.syncStockAndPriceDisplayItems(vendorInventory);
		this.syncVaultDisplayItems(vendorInventory);
	}

	private void syncStockAndPriceDisplayItems(Container vendorInventory) {
		for (int index = 0; index < VendorMachineBlockEntity.STOCK_SLOT_COUNT; index++) {
			ItemStack stockStack = vendorInventory.getItem(VendorMachineBlockEntity.STOCK_SLOT_START + index);
			ItemStack priceStack = vendorInventory.getItem(VendorMachineBlockEntity.PRICE_SLOT_START + index);

			this.setDisplayItemIfChanged(INDEX_STOCK_DISPLAY_START + index, stockStack);
			this.setDisplayItemIfChanged(INDEX_PRICE_DISPLAY_START + index, priceStack);
		}
	}

	private void syncVaultDisplayItems(Container vendorInventory) {
		for (int index = 0; index < VendorMachineBlockEntity.VAULT_SLOT_COUNT; index++) {
			ItemStack vaultStack = vendorInventory.getItem(VendorMachineBlockEntity.VAULT_SLOT_START + index);

			this.setDisplayItemIfChanged(INDEX_VAULT_DISPLAY_START + index, vaultStack);
		}
	}

	private void setDisplayItemIfChanged(int displaySlot, ItemStack sourceStack) {
		ItemStack currentStack = this.displayItems.getItem(displaySlot);

		if (areStacksEqual(currentStack, sourceStack)) {
			return;
		}

		this.displayItems.setItem(displaySlot, sourceStack.copy());
	}

	private static boolean areStacksEqual(ItemStack firstStack, ItemStack secondStack) {
		if (firstStack.isEmpty() && secondStack.isEmpty()) {
			return true;
		}

		if (firstStack.isEmpty() || secondStack.isEmpty()) {
			return false;
		}

		return firstStack.getCount() == secondStack.getCount()
				&& ItemStack.isSameItemSameComponents(firstStack, secondStack);
	}

	private void addHiddenDisplaySlots() {
		for (int index = 0; index < SIZE_DISPLAY_SLOT_COUNT; index++) {
			this.addSlot(new LockedSlot(
					this.displayItems,
					index,
					POS_HIDDEN_SLOT_X,
					POS_HIDDEN_SLOT_Y
			));
		}
	}

	private void addPaymentSlot() {
		this.addSlot(new Slot(
				this.paymentContainer,
				INDEX_PAYMENT_SLOT,
				POS_PAYMENT_SLOT_X,
				POS_PAYMENT_SLOT_Y
		));
	}

	private void addPlayerInventorySlots(Inventory playerInventory) {
		this.addPlayerMainInventorySlots(playerInventory);
		this.addPlayerHotbarSlots(playerInventory);
	}

	private void addPlayerMainInventorySlots(Inventory playerInventory) {
		for (int row = 0; row < LAYOUT_PLAYER_INVENTORY_ROWS; row++) {
			for (int column = 0; column < LAYOUT_PLAYER_INVENTORY_COLUMNS; column++) {
				this.addSlot(new Slot(
						playerInventory,
						INDEX_PLAYER_MAIN_INVENTORY_START + column + row * LAYOUT_PLAYER_INVENTORY_COLUMNS,
						POS_PLAYER_INVENTORY_X + column * SIZE_SLOT,
						POS_PLAYER_INVENTORY_Y + row * SIZE_SLOT
				));
			}
		}
	}

	private void addPlayerHotbarSlots(Inventory playerInventory) {
		for (int column = 0; column < LAYOUT_PLAYER_INVENTORY_COLUMNS; column++) {
			this.addSlot(new Slot(
					playerInventory,
					INDEX_PLAYER_HOTBAR_START + column,
					POS_PLAYER_INVENTORY_X + column * SIZE_SLOT,
					POS_PLAYER_HOTBAR_Y
			));
		}
	}

	public ItemStack getStockDisplayStack(int index) {
		if (index < 0 || index >= VendorMachineBlockEntity.STOCK_SLOT_COUNT) {
			return ItemStack.EMPTY;
		}

		return this.displayItems.getItem(INDEX_STOCK_DISPLAY_START + index);
	}

	public ItemStack getPriceDisplayStack(int index) {
		if (index < 0 || index >= VendorMachineBlockEntity.PRICE_SLOT_COUNT) {
			return ItemStack.EMPTY;
		}

		return this.displayItems.getItem(INDEX_PRICE_DISPLAY_START + index);
	}

	public ItemStack getVaultDisplayStack(int index) {
		if (index < 0 || index >= VendorMachineBlockEntity.VAULT_SLOT_COUNT) {
			return ItemStack.EMPTY;
		}

		return this.displayItems.getItem(INDEX_VAULT_DISPLAY_START + index);
	}

	public ItemStack getPaymentStack() {
		return this.paymentContainer.getItem(INDEX_PAYMENT_SLOT);
	}

	public boolean canVaultAcceptPayment(ItemStack paymentStack) {
		return this.getVaultAcceptablePurchaseCount(paymentStack) > 0;
	}

	public int getVaultAcceptablePurchaseCount(ItemStack unitPaymentStack) {
		if (unitPaymentStack.isEmpty()) {
			return 0;
		}

		int unitPrice = unitPaymentStack.getCount();

		if (unitPrice <= 0) {
			return 0;
		}

		int acceptablePaymentItems = this.countAcceptablePaymentItemsInVault(unitPaymentStack);

		return acceptablePaymentItems / unitPrice;
	}

	private int countAcceptablePaymentItemsInVault(ItemStack unitPaymentStack) {
		int acceptablePaymentItems = 0;

		for (int index = 0; index < VendorMachineBlockEntity.VAULT_SLOT_COUNT; index++) {
			ItemStack vaultStack = this.getVaultDisplayStack(index);

			if (vaultStack.isEmpty()) {
				acceptablePaymentItems += unitPaymentStack.getMaxStackSize();
				continue;
			}

			if (!ItemStack.isSameItemSameComponents(vaultStack, unitPaymentStack)) {
				continue;
			}

			int freeSpace = vaultStack.getMaxStackSize() - vaultStack.getCount();

			if (freeSpace > 0) {
				acceptablePaymentItems += freeSpace;
			}
		}

		return acceptablePaymentItems;
	}

	@Override
	public boolean clickMenuButton(Player player, int buttonId) {
		if (buttonId < ID_CHECKOUT_BUTTON_BASE) {
			return false;
		}

		int productIndex = getCheckoutProductIndex(buttonId);
		int quantity = getCheckoutQuantity(buttonId);

		return this.checkout(player, productIndex, quantity);
	}

	private boolean checkout(Player player, int productIndex, int quantity) {
		if (!this.canAttemptCheckout(productIndex, quantity)) {
			return this.failCheckout(player);
		}

		Container vendorInventory = this.vendorMachineBlockEntity.getInventory();

		ItemStack stockStack = vendorInventory.getItem(VendorMachineBlockEntity.STOCK_SLOT_START + productIndex);
		ItemStack priceStack = vendorInventory.getItem(VendorMachineBlockEntity.PRICE_SLOT_START + productIndex);
		ItemStack paymentStack = this.paymentContainer.getItem(INDEX_PAYMENT_SLOT);

		if (!this.isCheckoutPaymentValid(stockStack, priceStack, paymentStack, quantity)) {
			return this.failCheckout(player);
		}

		int totalPaymentCount = priceStack.getCount() * quantity;

		ItemStack totalPaymentStack = priceStack.copy();
		totalPaymentStack.setCount(totalPaymentCount);

		if (!this.canVendorVaultAcceptPayment(totalPaymentStack)) {
			return this.failCheckout(player);
		}

		this.insertPaymentIntoVendorVault(totalPaymentStack);
		this.deliverPurchase(player, vendorInventory, stockStack, productIndex, quantity);
		this.returnChangeToPlayer(player, paymentStack, totalPaymentCount);
		this.clearPaymentSlot();

		this.vendorMachineBlockEntity.setChanged();
		this.vendorMachineBlockEntity.startTransactionPulse();

		this.syncDisplayItemsFromVendor();
		this.broadcastChanges();

		ModSounds.playMachineCheckoutSuccess(player);

		return true;
	}

	private boolean failCheckout(Player player) {
		ModSounds.playMachineError(player);
		return false;
	}

	private boolean canAttemptCheckout(int productIndex, int quantity) {
		if (this.vendorMachineBlockEntity == null) {
			return false;
		}

		if (productIndex < 0 || productIndex >= VendorMachineBlockEntity.STOCK_SLOT_COUNT) {
			return false;
		}

		return quantity > 0;
	}

	private boolean isCheckoutPaymentValid(ItemStack stockStack, ItemStack priceStack, ItemStack paymentStack, int quantity) {
		if (stockStack.isEmpty() || priceStack.isEmpty() || paymentStack.isEmpty()) {
			return false;
		}

		if (stockStack.getCount() < quantity) {
			return false;
		}

		int totalPaymentCount = priceStack.getCount() * quantity;

		if (!ItemStack.isSameItemSameComponents(paymentStack, priceStack)) {
			return false;
		}

		return paymentStack.getCount() >= totalPaymentCount;
	}

	private boolean canVendorVaultAcceptPayment(ItemStack paymentStack) {
		if (paymentStack.isEmpty() || this.vendorMachineBlockEntity == null) {
			return false;
		}

		Container vendorInventory = this.vendorMachineBlockEntity.getInventory();
		int remainingPayment = paymentStack.getCount();

		for (int index = 0; index < VendorMachineBlockEntity.VAULT_SLOT_COUNT; index++) {
			ItemStack vaultStack = vendorInventory.getItem(VendorMachineBlockEntity.VAULT_SLOT_START + index);

			if (vaultStack.isEmpty()) {
				remainingPayment -= paymentStack.getMaxStackSize();
			} else if (ItemStack.isSameItemSameComponents(vaultStack, paymentStack)) {
				remainingPayment -= vaultStack.getMaxStackSize() - vaultStack.getCount();
			}

			if (remainingPayment <= 0) {
				return true;
			}
		}

		return false;
	}

	private void insertPaymentIntoVendorVault(ItemStack paymentStack) {
		if (paymentStack.isEmpty() || this.vendorMachineBlockEntity == null) {
			return;
		}

		Container vendorInventory = this.vendorMachineBlockEntity.getInventory();
		ItemStack remainingPayment = paymentStack.copy();

		this.mergePaymentIntoExistingVaultStacks(vendorInventory, remainingPayment);
		this.insertPaymentIntoEmptyVaultSlots(vendorInventory, remainingPayment);
	}

	private void mergePaymentIntoExistingVaultStacks(Container vendorInventory, ItemStack remainingPayment) {
		for (int index = 0; index < VendorMachineBlockEntity.VAULT_SLOT_COUNT; index++) {
			if (remainingPayment.isEmpty()) {
				return;
			}

			ItemStack vaultStack = vendorInventory.getItem(VendorMachineBlockEntity.VAULT_SLOT_START + index);

			if (vaultStack.isEmpty()) {
				continue;
			}

			if (!ItemStack.isSameItemSameComponents(vaultStack, remainingPayment)) {
				continue;
			}

			int freeSpace = vaultStack.getMaxStackSize() - vaultStack.getCount();

			if (freeSpace <= 0) {
				continue;
			}

			int insertedCount = Math.min(freeSpace, remainingPayment.getCount());

			vaultStack.grow(insertedCount);
			remainingPayment.shrink(insertedCount);
		}
	}

	private void insertPaymentIntoEmptyVaultSlots(Container vendorInventory, ItemStack remainingPayment) {
		for (int index = 0; index < VendorMachineBlockEntity.VAULT_SLOT_COUNT; index++) {
			if (remainingPayment.isEmpty()) {
				return;
			}

			ItemStack vaultStack = vendorInventory.getItem(VendorMachineBlockEntity.VAULT_SLOT_START + index);

			if (!vaultStack.isEmpty()) {
				continue;
			}

			ItemStack insertedStack = remainingPayment.copy();
			insertedStack.setCount(Math.min(remainingPayment.getMaxStackSize(), remainingPayment.getCount()));

			vendorInventory.setItem(VendorMachineBlockEntity.VAULT_SLOT_START + index, insertedStack);
			remainingPayment.shrink(insertedStack.getCount());
		}
	}

	private void deliverPurchase(Player player, Container vendorInventory, ItemStack stockStack, int productIndex, int quantity) {
		ItemStack purchasedStack = stockStack.copy();
		purchasedStack.setCount(quantity);

		stockStack.shrink(quantity);

		if (stockStack.isEmpty()) {
			this.vendorMachineBlockEntity.clearStockAfterSale(productIndex);
		}

		this.giveToPlayer(player, purchasedStack);
	}

	private void returnChangeToPlayer(Player player, ItemStack paymentStack, int totalPaymentCount) {
		paymentStack.shrink(totalPaymentCount);

		if (!paymentStack.isEmpty()) {
			this.giveToPlayer(player, paymentStack.copy());
		}
	}

	private void clearPaymentSlot() {
		this.paymentContainer.setItem(INDEX_PAYMENT_SLOT, ItemStack.EMPTY);
	}

	private void giveToPlayer(Player player, ItemStack stack) {
		if (stack.isEmpty()) {
			return;
		}

		player.getInventory().placeItemBackInInventory(stack);
	}

	@Override
	public void removed(Player player) {
		super.removed(player);

		ItemStack paymentStack = this.paymentContainer.removeItemNoUpdate(INDEX_PAYMENT_SLOT);

		if (!paymentStack.isEmpty()) {
			this.giveToPlayer(player, paymentStack);
		}

		if (this.vendorMachineBlockEntity != null) {
			this.vendorMachineBlockEntity.registerSalesMenuClosed();
		}

		ModSounds.playMachineSalesClose(player);
	}

	public VendorMachineBlockEntity getVendorMachineBlockEntity() {
		return this.vendorMachineBlockEntity;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot sourceSlot = this.slots.get(index);

		if (!sourceSlot.hasItem()) {
			return ItemStack.EMPTY;
		}

		ItemStack sourceStack = sourceSlot.getItem();
		ItemStack originalStack = sourceStack.copy();

		if (index >= MENU_HIDDEN_DISPLAY_START && index < MENU_HIDDEN_DISPLAY_END) {
			return ItemStack.EMPTY;
		}

		if (index == MENU_PAYMENT_SLOT) {
			if (!this.moveItemStackTo(sourceStack, MENU_PLAYER_INVENTORY_START, MENU_PLAYER_INVENTORY_END, true)) {
				return ItemStack.EMPTY;
			}
		} else if (index >= MENU_PLAYER_INVENTORY_START && index < MENU_PLAYER_INVENTORY_END) {
			if (!this.moveItemStackTo(sourceStack, MENU_PAYMENT_START, MENU_PAYMENT_END, false)) {
				return ItemStack.EMPTY;
			}
		} else {
			return ItemStack.EMPTY;
		}

		if (sourceStack.isEmpty()) {
			sourceSlot.set(ItemStack.EMPTY);
		} else {
			sourceSlot.setChanged();
		}

		if (sourceStack.getCount() == originalStack.getCount()) {
			return ItemStack.EMPTY;
		}

		sourceSlot.onTake(player, sourceStack);

		return originalStack;
	}

	@Override
	public boolean stillValid(Player player) {
		if (this.vendorMachineBlockEntity == null) {
			return true;
		}

		return this.vendorMachineBlockEntity.stillValid(player);
	}

	private static class LockedSlot extends Slot {
		public LockedSlot(Container container, int slot, int x, int y) {
			super(container, slot, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return false;
		}

		@Override
		public boolean mayPickup(Player player) {
			return false;
		}
	}
}