package domestia_vendor_choice;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
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
import org.jetbrains.annotations.Nullable;

public class VendorStandBlock extends Block implements EntityBlock {
	public static final MapCodec<VendorStandBlock> CODEC = simpleCodec(VendorStandBlock::new);
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final BooleanProperty WALL = BooleanProperty.create("wall");

	private static final VoxelShape FLOOR_SHAPE_NORTH = createFloorShapeNorth();
	private static final VoxelShape FLOOR_SHAPE_EAST = rotateNorthShape(FLOOR_SHAPE_NORTH, Direction.EAST);
	private static final VoxelShape FLOOR_SHAPE_SOUTH = rotateNorthShape(FLOOR_SHAPE_NORTH, Direction.SOUTH);
	private static final VoxelShape FLOOR_SHAPE_WEST = rotateNorthShape(FLOOR_SHAPE_NORTH, Direction.WEST);

	private static final VoxelShape WALL_SHAPE_NORTH = createWallShapeNorth();
	private static final VoxelShape WALL_SHAPE_EAST = rotateNorthShape(WALL_SHAPE_NORTH, Direction.EAST);
	private static final VoxelShape WALL_SHAPE_SOUTH = rotateNorthShape(WALL_SHAPE_NORTH, Direction.SOUTH);
	private static final VoxelShape WALL_SHAPE_WEST = rotateNorthShape(WALL_SHAPE_NORTH, Direction.WEST);

	public VendorStandBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(
				this.stateDefinition.any()
						.setValue(FACING, Direction.NORTH)
						.setValue(WALL, false)
		);
	}

	@Override
	protected MapCodec<? extends Block> codec() {
		return CODEC;
	}

	@Override
	@Nullable
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction clickedFace = context.getClickedFace();

		if (clickedFace == Direction.DOWN) {
			return null;
		}

		if (clickedFace == Direction.UP) {
			return this.defaultBlockState()
					.setValue(WALL, false)
					.setValue(FACING, context.getHorizontalDirection().getOpposite());
		}

		return this.defaultBlockState()
				.setValue(WALL, true)
				.setValue(FACING, clickedFace);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new VendorStandBlockEntity(pos, state);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);

		if (level.isClientSide() || !(placer instanceof Player player)) {
			return;
		}

		BlockEntity blockEntity = level.getBlockEntity(pos);

		if (blockEntity instanceof VendorStandBlockEntity vendorStandBlockEntity) {
			vendorStandBlockEntity.initializeFromPlacedItem(player, stack);
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
		return this.handleInteraction(state, level, pos, player, hitResult);
	}

	@Override
	protected InteractionResult useWithoutItem(
			BlockState state,
			Level level,
			BlockPos pos,
			Player player,
			BlockHitResult hitResult
	) {
		return this.handleInteraction(state, level, pos, player, hitResult);
	}

	private InteractionResult handleInteraction(
			BlockState state,
			Level level,
			BlockPos pos,
			Player player,
			BlockHitResult hitResult
	) {
		if (!isInteractiveSurface(state, hitResult.getDirection())) {
			return InteractionResult.PASS;
		}

		if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
			BlockEntity blockEntity = level.getBlockEntity(pos);

			if (blockEntity instanceof VendorStandBlockEntity vendorStandBlockEntity) {
				ModNetworking.openVendorStand(serverPlayer, vendorStandBlockEntity);
			}
		}

		return InteractionResult.SUCCESS;
	}

	private static boolean isInteractiveSurface(BlockState state, Direction clickedFace) {
		Direction facing = state.getValue(FACING);

		if (state.getValue(WALL)) {
			return clickedFace == facing;
		}

		return clickedFace == Direction.UP || clickedFace == facing;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return getShapeForState(state);
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return getShapeForState(state);
	}

	private static VoxelShape getShapeForState(BlockState state) {
		Direction facing = state.getValue(FACING);
		boolean wall = state.getValue(WALL);

		return switch (facing) {
			case EAST -> wall ? WALL_SHAPE_EAST : FLOOR_SHAPE_EAST;
			case SOUTH -> wall ? WALL_SHAPE_SOUTH : FLOOR_SHAPE_SOUTH;
			case WEST -> wall ? WALL_SHAPE_WEST : FLOOR_SHAPE_WEST;
			default -> wall ? WALL_SHAPE_NORTH : FLOOR_SHAPE_NORTH;
		};
	}

	private static VoxelShape createFloorShapeNorth() {
		VoxelShape shape = Shapes.or(
				Block.box(6.0D, 0.0D, 6.0D, 10.0D, 2.0D, 10.0D),
				Block.box(7.0D, 2.0D, 7.0D, 9.0D, 12.0D, 9.0D)
		);

		// The reading surface is subdivided into one-model-unit strips so the interaction
		// follows the rendered 17.5-degree slope. The surface shape is shifted one model
		// unit below the visible panel so the selection outline stays visually hidden
		// under the rendered model instead of drawing a striped stair-step overlay.
		shape = Shapes.or(shape, createRotatedXShape(
				2.0D, 8.5D, 1.0D,
				14.0D, 9.5D, 15.0D,
				-17.5D, 9.0D, 1.0D, 14
		));

		shape = Shapes.or(shape, createRotatedXShape(
				1.0D, 9.87545D, 0.94221D,
				15.0D, 10.87545D, 1.94221D,
				-17.5D, 9.87545D, -1.05779D, 1
		));
		shape = Shapes.or(shape, createRotatedXShape(
				2.0D, 9.57474D, -0.0115D,
				14.0D, 10.57474D, 0.9885D,
				-17.5D, 9.57474D, -2.0115D, 1
		));
		shape = Shapes.or(shape, createRotatedXShape(
				2.0D, 10.82916D, 0.64151D,
				14.0D, 11.82916D, 1.64151D,
				-17.5D, 10.82916D, -1.35849D, 1
		));
		shape = Shapes.or(shape, createRotatedXShape(
				2.0D, 10.17615D, 1.89593D,
				14.0D, 11.17615D, 2.89593D,
				-17.5D, 10.17615D, -0.10407D, 1
		));
		shape = Shapes.or(shape, createRotatedXShape(
				2.0D, 13.48392D, 12.38682D,
				14.0D, 14.48392D, 13.38682D,
				-17.5D, 13.48392D, 10.38682D, 1
		));
		shape = Shapes.or(shape, createRotatedXShape(
				2.0D, 14.73834D, 13.03983D,
				14.0D, 15.73834D, 14.03983D,
				-17.5D, 14.73834D, 11.03983D, 1
		));
		shape = Shapes.or(shape, createRotatedXShape(
				1.0D, 13.78462D, 13.34053D,
				15.0D, 14.78462D, 14.34053D,
				-17.5D, 13.78462D, 11.34053D, 1
		));
		shape = Shapes.or(shape, createRotatedXShape(
				2.0D, 14.08533D, 14.29425D,
				14.0D, 15.08533D, 15.29425D,
				-17.5D, 14.08533D, 12.29425D, 1
		));

		return shape;
	}

	private static VoxelShape createWallShapeNorth() {
		VoxelShape shape = Shapes.empty();

		shape = Shapes.or(shape, Block.box(2.0D, 2.0D, 15.0D, 14.0D, 16.0D, 16.0D));
		shape = Shapes.or(shape, Block.box(1.0D, 2.0D, 14.0D, 15.0D, 3.0D, 15.0D));
		shape = Shapes.or(shape, Block.box(2.0D, 1.0D, 14.0D, 14.0D, 2.0D, 15.0D));
		shape = Shapes.or(shape, Block.box(2.0D, 2.0D, 13.0D, 14.0D, 3.0D, 14.0D));
		shape = Shapes.or(shape, Block.box(2.0D, 3.0D, 14.0D, 14.0D, 4.0D, 15.0D));
		shape = Shapes.or(shape, Block.box(2.0D, 14.0D, 14.0D, 14.0D, 15.0D, 15.0D));
		shape = Shapes.or(shape, Block.box(2.0D, 15.0D, 13.0D, 14.0D, 16.0D, 14.0D));
		shape = Shapes.or(shape, Block.box(1.0D, 15.0D, 14.0D, 15.0D, 16.0D, 15.0D));
		shape = Shapes.or(shape, Block.box(2.0D, 16.0D, 14.0D, 14.0D, 17.0D, 15.0D));

		return shape;
	}

	private static VoxelShape createRotatedXShape(
			double minX,
			double minY,
			double minZ,
			double maxX,
			double maxY,
			double maxZ,
			double angleDegrees,
			double originY,
			double originZ,
			int zSlices
	) {
		double angle = Math.toRadians(angleDegrees);
		double cosine = Math.cos(angle);
		double sine = Math.sin(angle);
		double sliceDepth = (maxZ - minZ) / zSlices;
		VoxelShape shape = Shapes.empty();

		for (int slice = 0; slice < zSlices; slice++) {
			double sliceMinZ = minZ + sliceDepth * slice;
			double sliceMaxZ = slice == zSlices - 1 ? maxZ : sliceMinZ + sliceDepth;
			double rotatedMinY = Double.POSITIVE_INFINITY;
			double rotatedMinZ = Double.POSITIVE_INFINITY;
			double rotatedMaxY = Double.NEGATIVE_INFINITY;
			double rotatedMaxZ = Double.NEGATIVE_INFINITY;

			for (double y : new double[] { minY, maxY }) {
				for (double z : new double[] { sliceMinZ, sliceMaxZ }) {
					double relativeY = y - originY;
					double relativeZ = z - originZ;
					double rotatedY = originY + relativeY * cosine - relativeZ * sine;
					double rotatedZ = originZ + relativeY * sine + relativeZ * cosine;

					rotatedMinY = Math.min(rotatedMinY, rotatedY);
					rotatedMinZ = Math.min(rotatedMinZ, rotatedZ);
					rotatedMaxY = Math.max(rotatedMaxY, rotatedY);
					rotatedMaxZ = Math.max(rotatedMaxZ, rotatedZ);
				}
			}

			shape = Shapes.or(shape, Block.box(
					minX, rotatedMinY, rotatedMinZ,
					maxX, rotatedMaxY, rotatedMaxZ
			));
		}

		return shape;
	}

	private static VoxelShape rotateNorthShape(VoxelShape source, Direction facing) {
		if (facing == Direction.NORTH) {
			return source;
		}

		VoxelShape[] rotated = { Shapes.empty() };

		source.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
			VoxelShape box = switch (facing) {
				case EAST -> Shapes.box(1.0D - maxZ, minY, minX, 1.0D - minZ, maxY, maxX);
				case SOUTH -> Shapes.box(1.0D - maxX, minY, 1.0D - maxZ, 1.0D - minX, maxY, 1.0D - minZ);
				case WEST -> Shapes.box(minZ, minY, 1.0D - maxX, maxZ, maxY, 1.0D - minX);
				default -> Shapes.box(minX, minY, minZ, maxX, maxY, maxZ);
			};
			rotated[0] = Shapes.or(rotated[0], box);
		});

		return rotated[0];
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
		builder.add(FACING, WALL);
	}
}
