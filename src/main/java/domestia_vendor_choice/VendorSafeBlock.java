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
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

public class VendorSafeBlock extends HorizontalDirectionalBlock implements EntityBlock {
	public static final MapCodec<VendorSafeBlock> CODEC = simpleCodec(VendorSafeBlock::new);
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

	// Translation keys.
	private static final String ID_SCREEN_VENDOR_SAFE = "screen.domestia_vendor_choice.vendor_safe";
	private static final String ID_MESSAGE_ACCESS_DENIED = "message.domestia_vendor_choice.access_denied";

	// Menu title and feedback messages.
	private static final Component TITLE_VENDOR_SAFE = Component.translatable(ID_SCREEN_VENDOR_SAFE);
	private static final Component MESSAGE_ACCESS_DENIED = Component.translatable(ID_MESSAGE_ACCESS_DENIED);

	public VendorSafeBlock(BlockBehaviour.Properties properties) {
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
		return new VendorSafeBlockEntity(pos, state);
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

		if (blockEntity instanceof VendorSafeBlockEntity vendorSafeBlockEntity) {
			vendorSafeBlockEntity.setOwnerAndDisplayName(player, stack);
		}
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
		return this.handleVendorSafeInteraction(level, pos, player);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		return this.handleVendorSafeInteraction(level, pos, player);
	}

	private InteractionResult handleVendorSafeInteraction(Level level, BlockPos pos, Player player) {
		if (!level.isClientSide()) {
			this.tryOpenVendorSafeMenu(level, pos, player);
		}

		return InteractionResult.SUCCESS;
	}

	private void tryOpenVendorSafeMenu(Level level, BlockPos pos, Player player) {
		BlockEntity blockEntity = level.getBlockEntity(pos);

		if (!(blockEntity instanceof VendorSafeBlockEntity vendorSafeBlockEntity)) {
			return;
		}

		if (!vendorSafeBlockEntity.canManage(player)) {
			player.sendSystemMessage(MESSAGE_ACCESS_DENIED);
			ModSounds.playMachineError(player);
			return;
		}

		this.openVendorSafeMenu(player, vendorSafeBlockEntity);
		ModSounds.playSafeOpen(player);
	}

	private void openVendorSafeMenu(Player player, VendorSafeBlockEntity vendorSafeBlockEntity) {
		MenuProvider menuProvider = new SimpleMenuProvider(
				(containerId, playerInventory, menuPlayer) -> new VendorSafeMenu(containerId, playerInventory, vendorSafeBlockEntity),
				TITLE_VENDOR_SAFE
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
	protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
		builder.add(FACING);
	}
}