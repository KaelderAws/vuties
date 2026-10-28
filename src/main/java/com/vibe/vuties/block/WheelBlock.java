package com.vibe.vuties.block;

import com.mojang.serialization.MapCodec;
import com.vibe.vuties.block.entity.WheelBlockEntity;
import com.vibe.vuties.entity.WheelRunnerEntity;
import com.vibe.vuties.init.ModBlocks;
import com.vibe.vuties.init.ModEntityTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class WheelBlock extends BaseEntityBlock {
    public static final MapCodec<WheelBlock> CODEC = simpleCodec(WheelBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

    public WheelBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(FACING, Direction.NORTH)
            .setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (pos.getY() < level.getMaxBuildHeight() - 1 && level.getBlockState(pos.above()).canBeReplaced(context)) {
            return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection())
                .setValue(HALF, DoubleBlockHalf.LOWER);
        }
        return null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), 3);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
        DoubleBlockHalf half = state.getValue(HALF);
        if (facing.getAxis() != Direction.Axis.Y || half == DoubleBlockHalf.LOWER != (facing == Direction.UP) || facingState.is(this) && facingState.getValue(HALF) != half) {
            return half == DoubleBlockHalf.LOWER && facing == Direction.DOWN && !state.canSurvive(level, currentPos) ? Blocks.AIR.defaultBlockState() : super.updateShape(state, facing, facingState, level, currentPos, facingPos);
        } else {
            return Blocks.AIR.defaultBlockState();
        }
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getValue(HALF) == DoubleBlockHalf.LOWER) {
            return true;
        } else {
            BlockState below = level.getBlockState(pos.below());
            return below.is(this) && below.getValue(HALF) == DoubleBlockHalf.LOWER;
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            DoubleBlockHalf half = state.getValue(HALF);
            BlockPos otherPos = (half == DoubleBlockHalf.LOWER) ? pos.above() : pos.below();
            BlockState otherState = level.getBlockState(otherPos);
            if (otherState.is(this) && otherState.getValue(HALF) != half) {
                if (player.isCreative()) {
                    level.setBlock(otherPos, Blocks.AIR.defaultBlockState(), 35);
                    level.levelEvent(player, 2001, otherPos, Block.getId(otherState));
                } else {
                    level.destroyBlock(otherPos, false);
                }
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            DoubleBlockHalf half = state.getValue(HALF);
            BlockPos masterPos = (half == DoubleBlockHalf.LOWER) ? pos : pos.below();
            if (level.getBlockEntity(masterPos) instanceof WheelBlockEntity wheelBE) {
                wheelBE.ejectRunnerWithCooldown(60);
            }

            BlockPos otherPos = (half == DoubleBlockHalf.LOWER) ? pos.above() : pos.below();
            BlockState otherState = level.getBlockState(otherPos);
            if (otherState.is(this) && otherState.getValue(HALF) != half) {
                level.setBlock(otherPos, Blocks.AIR.defaultBlockState(), 35);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new WheelBlockEntity(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (state.getValue(HALF) != DoubleBlockHalf.LOWER) {
            return null;
        }
        return (lvl, p, st, be) -> {
            if (be instanceof WheelBlockEntity wheelBE) {
                if (lvl.isClientSide) {
                    WheelBlockEntity.clientTick(lvl, p, st, wheelBE);
                } else {
                    WheelBlockEntity.serverTick(lvl, p, st, wheelBE);
                }
            }
        };
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (player.isShiftKeyDown() || player.isCrouching()) {
            BlockPos masterPos = (state.getValue(HALF) == DoubleBlockHalf.LOWER) ? pos : pos.below();
            if (level.getBlockEntity(masterPos) instanceof WheelBlockEntity wheelBE && wheelBE.hasOccupant()) {
                if (!level.isClientSide) {
                    wheelBE.ejectRunnerWithCooldown(60);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        BlockPos masterPos = (state.getValue(HALF) == DoubleBlockHalf.LOWER) ? pos : pos.below();
        return interactWithWheel(level, masterPos, player);
    }

    public static InteractionResult interactWithWheel(Level level, BlockPos masterPos, Player player) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(masterPos);
        if (!(be instanceof WheelBlockEntity wheelBE)) {
            return InteractionResult.PASS;
        }

        // If player is crouching (Shift+ПКМ): eject occupant
        if (player.isShiftKeyDown() || player.isCrouching()) {
            if (wheelBE.hasOccupant()) {
                wheelBE.ejectRunnerWithCooldown(60);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }

        // If player is already riding this wheel, dismount them
        if (player.getVehicle() instanceof WheelRunnerEntity runner && masterPos.equals(runner.getMasterPos())) {
            player.stopRiding();
            return InteractionResult.SUCCESS;
        }

        // If wheel is already occupied by someone else, cannot enter
        if (wheelBE.hasOccupant()) {
            return InteractionResult.SUCCESS;
        }

        BlockState masterState = level.getBlockState(masterPos);
        if (!masterState.is(ModBlocks.WHEEL.get())) {
            return InteractionResult.PASS;
        }

        Direction facing = masterState.getValue(FACING);

        WheelRunnerEntity runner = new WheelRunnerEntity(ModEntityTypes.WHEEL_RUNNER.get(), level);
        runner.setMasterPos(masterPos);
        runner.setPos(masterPos.getX() + 0.5, masterPos.getY() + 1.05, masterPos.getZ() + 0.5);
        runner.setYRot(facing.toYRot());
        runner.setXRot(0.0F);

        // Pre-orient player look direction
        player.setYRot(facing.toYRot());
        player.setXRot(0.0F);
        player.yRotO = facing.toYRot();
        player.xRotO = 0.0F;
        player.setYHeadRot(facing.toYRot());
        player.setYBodyRot(facing.toYRot());

        level.addFreshEntity(runner);

        player.startRiding(runner, true);
        wheelBE.setRunnerEntity(runner);
        player.displayClientMessage(Component.translatable("message.vuties.wheel_enter"), true);

        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, java.util.List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("tooltip.vuties.wheel.desc").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("tooltip.vuties.wheel.player_gen").withStyle(ChatFormatting.AQUA));
        tooltipComponents.add(Component.translatable("tooltip.vuties.wheel.villager_gen").withStyle(ChatFormatting.GREEN));
    }
}
