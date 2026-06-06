package domestia_vendor_choice;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

public class VendorMachineBlock extends HorizontalDirectionalBlock implements EntityBlock {
	public static final MapCodec<VendorMachineBlock> CODEC = simpleCodec(VendorMachineBlock::new);
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

	// Transaction pulse output.
	public static final int POWER_TRANSACTION_SIGNAL_STRENGTH = 15;

	// Translation keys.
	private static final String ID_SCREEN_SALES = "screen.domestia_vendor_choice.vendor_machine_sales";
	private static final String ID_SCREEN_CONTROL = "screen.domestia_vendor_choice.vendor_machine_control";
	private static final String ID_MESSAGE_ACCESS_DENIED = "message.domestia_vendor_choice.access_denied";

	// Menu titles and feedback messages.
	private static final Component TITLE_SALES = Component.translatable(ID_SCREEN_SALES);
	private static final Component TITLE_CONTROL = Component.translatable(ID_SCREEN_CONTROL);
	private static final Component MESSAGE_ACCESS_DENIED = Component.translatable(ID_MESSAGE_ACCESS_DENIED);

	public VendorMachineBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
		return CODEC;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new VendorMachineBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
			Level level,
			BlockState state,
			BlockEntityType<T> blockEntityType
	) {
		if (level.isClientSide()) {
			return null;
		}

		if (blockEntityType != ModBlockEntities.VENDOR_MACHINE) {
			return null;
		}

		return (tickerLevel, tickerPos, tickerState, blockEntity) -> {
			if (blockEntity instanceof VendorMachineBlockEntity vendorMachineBlockEntity) {
				vendorMachineBlockEntity.serverTick();
			}
		};
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);

		if (level.isClientSide()) {
			return;
		}

		if (!(placer instanceof Player player)) {
			return;
		}

		this.assignOwnerAndDisplayName(level, pos, player, stack);
	}

	private void assignOwnerAndDisplayName(Level level, BlockPos pos, Player player, ItemStack stack) {
		BlockEntity blockEntity = level.getBlockEntity(pos);

		if (blockEntity instanceof VendorMachineBlockEntity vendorMachineBlockEntity) {
			vendorMachineBlockEntity.setOwnerAndDisplayName(player, stack);
		}
	}

	@Override
	protected InteractionResult useItemOn(
			ItemStack stack,
			BlockState state,
			Level level,
			BlockPos pos,
			Player player,
			InteractionHand hand,
			BlockHitResult hitResult
	) {
		return this.handleVendorInteraction(state, level, pos, player, hitResult);
	}

	@Override
	protected InteractionResult useWithoutItem(
			BlockState state,
			Level level,
			BlockPos pos,
			Player player,
			BlockHitResult hitResult
	) {
		return this.handleVendorInteraction(state, level, pos, player, hitResult);
	}

	private InteractionResult handleVendorInteraction(
			BlockState state,
			Level level,
			BlockPos pos,
			Player player,
			BlockHitResult hitResult
	) {
		Direction clickedSide = hitResult.getDirection();
		Direction frontSide = state.getValue(FACING);
		Direction backSide = frontSide.getOpposite();

		if (clickedSide == frontSide) {
			if (!level.isClientSide()) {
				this.tryOpenSalesMenu(level, pos, player);
			}

			return InteractionResult.SUCCESS;
		}

		if (clickedSide == backSide) {
			if (!level.isClientSide()) {
				this.tryOpenControlMenu(level, pos, player);
			}

			return InteractionResult.SUCCESS;
		}

		return InteractionResult.PASS;
	}

	private void tryOpenSalesMenu(Level level, BlockPos pos, Player player) {
		BlockEntity blockEntity = level.getBlockEntity(pos);

		if (blockEntity instanceof VendorMachineBlockEntity vendorMachineBlockEntity) {
			this.openSalesMenu(player, vendorMachineBlockEntity);
			ModSounds.playMachineSalesOpen(player);
		}
	}

	private void tryOpenControlMenu(Level level, BlockPos pos, Player player) {
		BlockEntity blockEntity = level.getBlockEntity(pos);

		if (!(blockEntity instanceof VendorMachineBlockEntity vendorMachineBlockEntity)) {
			return;
		}

		if (!vendorMachineBlockEntity.canManage(player)) {
			player.sendSystemMessage(MESSAGE_ACCESS_DENIED);
			ModSounds.playMachineError(player);
			return;
		}

		this.openControlMenu(player, vendorMachineBlockEntity);
		ModSounds.playMachineControlOpen(player);
	}

	private void openSalesMenu(Player player, VendorMachineBlockEntity vendorMachineBlockEntity) {
		MenuProvider menuProvider = new SimpleMenuProvider(
				(containerId, playerInventory, menuPlayer) -> new VendorMachineSalesMenu(
						containerId,
						playerInventory,
						vendorMachineBlockEntity
				),
				TITLE_SALES
		);

		player.openMenu(menuProvider);
	}

	private void openControlMenu(Player player, VendorMachineBlockEntity vendorMachineBlockEntity) {
		MenuProvider menuProvider = new SimpleMenuProvider(
				(containerId, playerInventory, menuPlayer) -> new VendorMachineControlMenu(
						containerId,
						playerInventory,
						vendorMachineBlockEntity
				),
				TITLE_CONTROL
		);

		player.openMenu(menuProvider);
	}

	// Redstone output is emitted only during a successful checkout transaction pulse.
	// Front opens Sales UI. Back opens Control UI. All other faces are utility output faces.
	@Override
	protected boolean isSignalSource(BlockState state) {
		return true;
	}

	@Override
	protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return this.getTransactionSignal(state, level, pos, direction);
	}

	@Override
	protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return this.getTransactionSignal(state, level, pos, direction);
	}

	private int getTransactionSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		if (!isTransactionOutputFace(state, direction)) {
			return 0;
		}

		BlockEntity blockEntity = level.getBlockEntity(pos);

		if (!(blockEntity instanceof VendorMachineBlockEntity vendorMachineBlockEntity)) {
			return 0;
		}

		if (!vendorMachineBlockEntity.isTransactionPowered()) {
			return 0;
		}

		return POWER_TRANSACTION_SIGNAL_STRENGTH;
	}

	public static boolean isTransactionOutputFace(BlockState state, Direction direction) {
		Direction frontSide = state.getValue(FACING);
		Direction backSide = frontSide.getOpposite();

		return direction != frontSide && direction != backSide;
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		BlockEntity blockEntity = level.getBlockEntity(pos);

		if (blockEntity instanceof VendorMachineBlockEntity vendorMachineBlockEntity) {
			vendorMachineBlockEntity.handleTransactionPulseScheduledTick(level);
		}
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
		builder.add(FACING);
	}
}