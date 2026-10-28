package com.vibe.vuties.block;

import com.mojang.serialization.MapCodec;
import com.vibe.vuties.block.entity.EnergyCableBlockEntity;
import com.vibe.vuties.init.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EnergyCableBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<EnergyCableBlock> CODEC = simpleCodec(EnergyCableBlock::new);

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;
    public static final BooleanProperty CONDUCTING = BooleanProperty.create("conducting");
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private static final VoxelShape CORE_SHAPE = Block.box(6.0, 6.0, 6.0, 10.0, 10.0, 10.0);
    private static final VoxelShape SHAPE_DOWN = Block.box(6.0, 0.0, 6.0, 10.0, 6.0, 10.0);
    private static final VoxelShape SHAPE_UP = Block.box(6.0, 10.0, 6.0, 10.0, 16.0, 10.0);
    private static final VoxelShape SHAPE_NORTH = Block.box(6.0, 6.0, 0.0, 10.0, 10.0, 6.0);
    private static final VoxelShape SHAPE_SOUTH = Block.box(6.0, 6.0, 10.0, 10.0, 10.0, 16.0);
    private static final VoxelShape SHAPE_WEST = Block.box(0.0, 6.0, 6.0, 6.0, 10.0, 10.0);
    private static final VoxelShape SHAPE_EAST = Block.box(10.0, 6.0, 6.0, 16.0, 10.0, 10.0);

    private static final VoxelShape[] SHAPES = new VoxelShape[64];

    static {
        for (int i = 0; i < 64; i++) {
            VoxelShape shape = CORE_SHAPE;
            if ((i & 1) != 0) shape = Shapes.or(shape, SHAPE_DOWN);
            if ((i & 2) != 0) shape = Shapes.or(shape, SHAPE_UP);
            if ((i & 4) != 0) shape = Shapes.or(shape, SHAPE_NORTH);
            if ((i & 8) != 0) shape = Shapes.or(shape, SHAPE_SOUTH);
            if ((i & 16) != 0) shape = Shapes.or(shape, SHAPE_WEST);
            if ((i & 32) != 0) shape = Shapes.or(shape, SHAPE_EAST);
            SHAPES[i] = shape;
        }
    }

    public EnergyCableBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(DOWN, false)
            .setValue(UP, false)
            .setValue(NORTH, false)
            .setValue(SOUTH, false)
            .setValue(WEST, false)
            .setValue(EAST, false)
            .setValue(CONDUCTING, false)
            .setValue(WATERLOGGED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DOWN, UP, NORTH, SOUTH, WEST, EAST, CONDUCTING, WATERLOGGED);
    }

    private static int getShapeIndex(BlockState state) {
        int index = 0;
        if (state.getValue(DOWN)) index |= 1;
        if (state.getValue(UP)) index |= 2;
        if (state.getValue(NORTH)) index |= 4;
        if (state.getValue(SOUTH)) index |= 8;
        if (state.getValue(WEST)) index |= 16;
        if (state.getValue(EAST)) index |= 32;
        return index;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[getShapeIndex(state)];
    }

    public static BooleanProperty getPropertyForDirection(Direction dir) {
        return switch (dir) {
            case DOWN -> DOWN;
            case UP -> UP;
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
        };
    }

    public static boolean isConnected(BlockState state, Direction dir) {
        return state.getValue(getPropertyForDirection(dir));
    }

    public static boolean canConnectTo(LevelAccessor level, BlockPos cablePos, Direction dir) {
        BlockPos neighborPos = cablePos.relative(dir);
        if (level instanceof Level l && !l.isLoaded(neighborPos)) return false;

        BlockState neighborState = level.getBlockState(neighborPos);
        if (neighborState.isAir()) return false;
        if (neighborState.getBlock() instanceof EnergyCableBlock) return true;

        if (level instanceof Level l) {
            IEnergyStorage storage = l.getCapability(Capabilities.EnergyStorage.BLOCK, neighborPos, dir.getOpposite());
            if (storage != null && (storage.canReceive() || storage.canExtract())) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        FluidState fluid = level.getFluidState(pos);

        BlockState state = this.defaultBlockState().setValue(WATERLOGGED, fluid.getType() == Fluids.WATER);
        for (Direction dir : Direction.values()) {
            state = state.setValue(getPropertyForDirection(dir), canConnectTo(level, pos, dir));
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }

        boolean connects = canConnectTo(level, pos, direction);
        boolean wasConnected = state.getValue(getPropertyForDirection(direction));

        if (connects != wasConnected) {
            if (level instanceof Level l && !l.isClientSide) {
                if (level.getBlockEntity(pos) instanceof EnergyCableBlockEntity be) {
                    be.invalidateNetwork();
                }
            }
            return state.setValue(getPropertyForDirection(direction), connects);
        }
        return state;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof EnergyCableBlockEntity be) {
                be.invalidateNetwork();
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyCableBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.ENERGY_CABLE.get(), EnergyCableBlockEntity::serverTick);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            if (level.getBlockEntity(pos) instanceof EnergyCableBlockEntity be) {
                int cableCount = be.getNetwork() != null ? be.getNetwork().getMemberCount() : 1;
                player.displayClientMessage(
                    Component.translatable("message.vuties.energy_cable_status",
                        String.format("%,d", 10000),
                        cableCount
                    ),
                    true
                );
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("tooltip.vuties.energy_cable.desc").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("tooltip.vuties.energy_cable.rate").withStyle(ChatFormatting.GREEN));
        tooltipComponents.add(Component.translatable("tooltip.vuties.energy_cable.feature").withStyle(ChatFormatting.DARK_GRAY));
    }
}
