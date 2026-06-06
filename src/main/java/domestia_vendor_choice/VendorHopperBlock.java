package domestia_vendor_choice;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class VendorHopperBlock extends Block implements EntityBlock {
	public static final MapCodec<VendorHopperBlock> CODEC = simpleCodec(VendorHopperBlock::new);
	public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;

	// Hopper-like shape. The block must not use a full-cube shape, otherwise it blocks adjacent containers visually and interactively.
	private static final VoxelShape SHAPE_BOWL = Block.box(0.0D, 10.0D, 0.0D, 16.0D, 16.0D, 16.0D);
	private static final VoxelShape SHAPE_FUNNEL = Block.box(4.0D, 4.0D, 4.0D, 12.0D, 10.0D, 12.0D);
	private static final VoxelShape SHAPE_SPOUT_DOWN = Block.box(6.0D, 0.0D, 6.0D, 10.0D, 4.0D, 10.0D);
	private static final VoxelShape SHAPE_SPOUT_NORTH = Block.box(6.0D, 4.0D, 0.0D, 10.0D, 8.0D, 4.0D);
	private static final VoxelShape SHAPE_SPOUT_SOUTH = Block.box(6.0D, 4.0D, 12.0D, 10.0D, 8.0D, 16.0D);
	private static final VoxelShape SHAPE_SPOUT_WEST = Block.box(0.0D, 4.0D, 6.0D, 4.0D, 8.0D, 10.0D);
	private static final VoxelShape SHAPE_SPOUT_EAST = Block.box(12.0D, 4.0D, 6.0D, 16.0D, 8.0D, 10.0D);

	private static final VoxelShape SHAPE_DOWN = Shapes.or(SHAPE_BOWL, SHAPE_FUNNEL, SHAPE_SPOUT_DOWN);
	private static final VoxelShape SHAPE_NORTH = Shapes.or(SHAPE_BOWL, SHAPE_FUNNEL, SHAPE_SPOUT_NORTH);
	private static final VoxelShape SHAPE_SOUTH = Shapes.or(SHAPE_BOWL, SHAPE_FUNNEL, SHAPE_SPOUT_SOUTH);
	private static final VoxelShape SHAPE_WEST = Shapes.or(SHAPE_BOWL, SHAPE_FUNNEL, SHAPE_SPOUT_WEST);
	private static final VoxelShape SHAPE_EAST = Shapes.or(SHAPE_BOWL, SHAPE_FUNNEL, SHAPE_SPOUT_EAST);

	// Translation keys.
	private static final String ID_SCREEN_VENDOR_HOPPER = "screen.domestia_vendor_choice.vendor_hopper";
	private static final String ID_MESSAGE_ACCESS_DENIED = "message.domestia_vendor_choice.access_denied";

	// Menu title and feedback messages.
	private static final Component TITLE_VENDOR_HOPPER = Component.translatable(ID_SCREEN_VENDOR_HOPPER);
	private static final Component MESSAGE_ACCESS_DENIED = Component.translatable(ID_MESSAGE_ACCESS_DENIED);

	public VendorHopperBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.DOWN));
	}

	@Override
	protected MapCodec<? extends Block> codec() {
		return CODEC;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction outputDirection = context.getClickedFace().getOpposite();

		if (outputDirection == Direction.UP) {
			outputDirection = Direction.DOWN;
		}

		return this.defaultBlockState().setValue(FACING, outputDirection);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new VendorHopperBlockEntity(pos, state);
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

		if (blockEntityType != ModBlockEntities.VENDOR_HOPPER) {
			return null;
		}

		return (tickerLevel, tickerPos, tickerState, blockEntity) -> {
			if (blockEntity instanceof VendorHopperBlockEntity vendorHopperBlockEntity) {
				vendorHopperBlockEntity.serverTick();
			}
		};
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return getShapeForFacing(state.getValue(FACING));
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return getShapeForFacing(state.getValue(FACING));
	}

	private static VoxelShape getShapeForFacing(Direction direction) {
		return switch (direction) {
			case NORTH -> SHAPE_NORTH;
			case SOUTH -> SHAPE_SOUTH;
			case WEST -> SHAPE_WEST;
			case EAST -> SHAPE_EAST;
			default -> SHAPE_DOWN;
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

		if (blockEntity instanceof VendorHopperBlockEntity vendorHopperBlockEntity) {
			vendorHopperBlockEntity.setOwnerAndDisplayName(player, stack);
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
		return this.handleVendorHopperInteraction(level, pos, player);
	}

	@Override
	protected InteractionResult useWithoutItem(
			BlockState state,
			Level level,
			BlockPos pos,
			Player player,
			BlockHitResult hitResult
	) {
		return this.handleVendorHopperInteraction(level, pos, player);
	}

	private InteractionResult handleVendorHopperInteraction(Level level, BlockPos pos, Player player) {
		if (!level.isClientSide()) {
			this.tryOpenVendorHopperMenu(level, pos, player);
		}

		return InteractionResult.SUCCESS;
	}

	private void tryOpenVendorHopperMenu(Level level, BlockPos pos, Player player) {
		BlockEntity blockEntity = level.getBlockEntity(pos);

		if (!(blockEntity instanceof VendorHopperBlockEntity vendorHopperBlockEntity)) {
			return;
		}

		if (!vendorHopperBlockEntity.canManage(player)) {
			player.sendSystemMessage(MESSAGE_ACCESS_DENIED);
			ModSounds.playMachineError(player);
			return;
		}

		this.openVendorHopperMenu(player, vendorHopperBlockEntity);
		ModSounds.playSafeOpen(player);
	}

	private void openVendorHopperMenu(Player player, VendorHopperBlockEntity vendorHopperBlockEntity) {
		MenuProvider menuProvider = new SimpleMenuProvider(
				(containerId, playerInventory, menuPlayer) -> new VendorHopperMenu(
						containerId,
						playerInventory,
						vendorHopperBlockEntity
				),
				TITLE_VENDOR_HOPPER
		);

		player.openMenu(menuProvider);
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
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}
}
