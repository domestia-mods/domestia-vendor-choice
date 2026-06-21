package domestia_vendor_choice;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class VendorHoloDisplayBlock extends HorizontalDirectionalBlock implements EntityBlock {
	public static final MapCodec<VendorHoloDisplayBlock> CODEC = simpleCodec(VendorHoloDisplayBlock::new);
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final BooleanProperty CEILING = BooleanProperty.create("ceiling");

	public static final int LIGHT_EMISSION = 12;

	private static final VoxelShape SHAPE_BASE_NORTH = Shapes.or(
			Block.box(0.0D, 0.0D, 1.0D, 15.0D, 2.0D, 15.0D),
			Block.box(6.0D, 0.0D, 1.0D, 10.0D, 2.0D, 15.0D),
			Block.box(1.0D, 0.0D, 9.0D, 15.0D, 2.0D, 13.0D),
			Block.box(7.0D, 1.0D, 7.0D, 9.0D, 12.0D, 9.0D),
			Block.box(4.0D, 8.0D, 4.0D, 12.0D, 12.0D, 12.0D)
	);
	private static final VoxelShape SHAPE_BASE_EAST = Shapes.or(
			Block.box(1.0D, 0.0D, 1.0D, 15.0D, 2.0D, 16.0D),
			Block.box(1.0D, 0.0D, 6.0D, 15.0D, 2.0D, 10.0D),
			Block.box(3.0D, 0.0D, 1.0D, 7.0D, 2.0D, 15.0D),
			Block.box(7.0D, 1.0D, 7.0D, 9.0D, 12.0D, 9.0D),
			Block.box(4.0D, 8.0D, 4.0D, 12.0D, 12.0D, 12.0D)
	);
	private static final VoxelShape SHAPE_BASE_SOUTH = Shapes.or(
			Block.box(1.0D, 0.0D, 1.0D, 16.0D, 2.0D, 15.0D),
			Block.box(6.0D, 0.0D, 1.0D, 10.0D, 2.0D, 15.0D),
			Block.box(1.0D, 0.0D, 3.0D, 15.0D, 2.0D, 7.0D),
			Block.box(7.0D, 1.0D, 7.0D, 9.0D, 12.0D, 9.0D),
			Block.box(4.0D, 8.0D, 4.0D, 12.0D, 12.0D, 12.0D)
	);
	private static final VoxelShape SHAPE_BASE_WEST = Shapes.or(
			Block.box(1.0D, 0.0D, 0.0D, 15.0D, 2.0D, 15.0D),
			Block.box(1.0D, 0.0D, 6.0D, 15.0D, 2.0D, 10.0D),
			Block.box(9.0D, 0.0D, 1.0D, 13.0D, 2.0D, 15.0D),
			Block.box(7.0D, 1.0D, 7.0D, 9.0D, 12.0D, 9.0D),
			Block.box(4.0D, 8.0D, 4.0D, 12.0D, 12.0D, 12.0D)
	);

	private static final VoxelShape SHAPE_CEILING_NORTH = flipShapeY(SHAPE_BASE_NORTH);
	private static final VoxelShape SHAPE_CEILING_EAST = flipShapeY(SHAPE_BASE_EAST);
	private static final VoxelShape SHAPE_CEILING_SOUTH = flipShapeY(SHAPE_BASE_SOUTH);
	private static final VoxelShape SHAPE_CEILING_WEST = flipShapeY(SHAPE_BASE_WEST);

	private static final String ID_SCREEN_VENDOR_HOLO_DISPLAY = "screen.domestia_vendor_choice.vendor_holo_display_control";
	private static final String ID_MESSAGE_ACCESS_DENIED = "message.domestia_vendor_choice.access_denied";

	private static final Component TITLE_VENDOR_HOLO_DISPLAY = Component.translatable(ID_SCREEN_VENDOR_HOLO_DISPLAY);
	private static final Component MESSAGE_ACCESS_DENIED = Component.translatable(ID_MESSAGE_ACCESS_DENIED);

	public VendorHoloDisplayBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any()
				.setValue(FACING, Direction.NORTH)
				.setValue(CEILING, false));
	}

	@Override
	protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
		return CODEC;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction clickedFace = context.getClickedFace();
		if (clickedFace != Direction.UP && clickedFace != Direction.DOWN) {
			return null;
		}

		return this.defaultBlockState()
				.setValue(FACING, context.getHorizontalDirection().getOpposite())
				.setValue(CEILING, clickedFace == Direction.DOWN);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new VendorHoloDisplayBlockEntity(pos, state);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return getShapeForFacing(state.getValue(FACING), state.getValue(CEILING));
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return getShapeForFacing(state.getValue(FACING), state.getValue(CEILING));
	}

	private static VoxelShape getShapeForFacing(Direction facing, boolean ceiling) {
		if (ceiling) {
			return switch (facing) {
				case EAST -> SHAPE_CEILING_EAST;
				case SOUTH -> SHAPE_CEILING_SOUTH;
				case WEST -> SHAPE_CEILING_WEST;
				default -> SHAPE_CEILING_NORTH;
			};
		}

		return switch (facing) {
			case EAST -> SHAPE_BASE_EAST;
			case SOUTH -> SHAPE_BASE_SOUTH;
			case WEST -> SHAPE_BASE_WEST;
			default -> SHAPE_BASE_NORTH;
		};
	}

	private static VoxelShape flipShapeY(VoxelShape shape) {
		VoxelShape[] flippedShape = {Shapes.empty()};
		shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> flippedShape[0] = Shapes.or(
				flippedShape[0],
				Block.box(
						minX * 16.0D,
						(1.0D - maxY) * 16.0D,
						minZ * 16.0D,
						maxX * 16.0D,
						(1.0D - minY) * 16.0D,
						maxZ * 16.0D
				)
		));
		return flippedShape[0];
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

		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (blockEntity instanceof VendorHoloDisplayBlockEntity vendorHoloDisplayBlockEntity) {
			vendorHoloDisplayBlockEntity.setOwner(player);
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
		return this.handleVendorHoloDisplayInteraction(level, pos, player);
	}

	@Override
	protected InteractionResult useWithoutItem(
			BlockState state,
			Level level,
			BlockPos pos,
			Player player,
			BlockHitResult hitResult
	) {
		return this.handleVendorHoloDisplayInteraction(level, pos, player);
	}

	private InteractionResult handleVendorHoloDisplayInteraction(Level level, BlockPos pos, Player player) {
		if (!level.isClientSide()) {
			this.tryOpenVendorHoloDisplayMenu(level, pos, player);
		}

		return InteractionResult.SUCCESS;
	}

	private void tryOpenVendorHoloDisplayMenu(Level level, BlockPos pos, Player player) {
		BlockEntity blockEntity = level.getBlockEntity(pos);

		if (!(blockEntity instanceof VendorHoloDisplayBlockEntity vendorHoloDisplayBlockEntity)) {
			return;
		}

		if (!vendorHoloDisplayBlockEntity.canManage(player)) {
			player.sendSystemMessage(MESSAGE_ACCESS_DENIED);
			ModSounds.playMachineError(player);
			return;
		}

		this.openVendorHoloDisplayMenu(player, vendorHoloDisplayBlockEntity);
		ModSounds.playSafeOpen(player);
	}

	private void openVendorHoloDisplayMenu(Player player, VendorHoloDisplayBlockEntity vendorHoloDisplayBlockEntity) {
		MenuProvider menuProvider = new SimpleMenuProvider(
				(containerId, playerInventory, menuPlayer) -> new VendorHoloDisplayMenu(
						containerId,
						playerInventory,
						vendorHoloDisplayBlockEntity
				),
				TITLE_VENDOR_HOLO_DISPLAY
		);

		player.openMenu(menuProvider);

		if (player instanceof ServerPlayer serverPlayer) {
			ModNetworking.openVendorHoloDisplay(serverPlayer, vendorHoloDisplayBlockEntity);
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
		builder.add(FACING, CEILING);
	}
}
