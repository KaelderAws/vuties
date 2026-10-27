package com.vibe.vuties.block;

import com.mojang.serialization.MapCodec;
import com.vibe.vuties.block.entity.WheelStructureBlockEntity;
import com.vibe.vuties.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class WheelStructureBlock extends BaseEntityBlock {
    public static final MapCodec<WheelStructureBlock> CODEC = simpleCodec(WheelStructureBlock::new);
    public static final BooleanProperty CENTER = BooleanProperty.create("center");

    public WheelStructureBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(CENTER, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CENTER);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(CENTER)) {
            return Shapes.empty();
        }
        return Shapes.block();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WheelStructureBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof WheelStructureBlockEntity subBE && subBE.getMasterPos() != null) {
            return WheelBlock.interactWithWheel(level, subBE.getMasterPos(), player);
        }
        return InteractionResult.PASS;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            if (level.getBlockEntity(pos) instanceof WheelStructureBlockEntity subBE && subBE.getMasterPos() != null) {
                BlockPos masterPos = subBE.getMasterPos();
                if (level.getBlockState(masterPos).is(ModBlocks.WHEEL.get())) {
                    level.destroyBlock(masterPos, !player.isCreative());
                }
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            if (level.getBlockEntity(pos) instanceof WheelStructureBlockEntity subBE && subBE.getMasterPos() != null) {
                BlockPos masterPos = subBE.getMasterPos();
                if (level.getBlockState(masterPos).is(ModBlocks.WHEEL.get())) {
                    level.destroyBlock(masterPos, true);
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
