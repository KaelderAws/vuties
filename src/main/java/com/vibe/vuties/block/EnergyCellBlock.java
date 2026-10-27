package com.vibe.vuties.block;

import com.mojang.serialization.MapCodec;
import com.vibe.vuties.block.entity.EnergyCellBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class EnergyCellBlock extends BaseEntityBlock {
    public static final MapCodec<EnergyCellBlock> CODEC = simpleCodec(EnergyCellBlock::new);

    public EnergyCellBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyCellBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            if (level.getBlockEntity(pos) instanceof EnergyCellBlockEntity cell) {
                int stored = cell.getEnergyStored();
                int max = cell.getMaxEnergyStored();
                double pct = max > 0 ? ((double) stored / max * 100.0) : 0.0;
                player.displayClientMessage(
                    Component.translatable("message.vuties.energy_cell_status",
                        String.format("%,d", stored),
                        String.format("%,d", max),
                        String.format("%.1f", pct)
                    ),
                    true
                );
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof EnergyCellBlockEntity cell) {
            int stored = cell.getEnergyStored();
            int max = cell.getMaxEnergyStored();
            return max > 0 ? (int) Math.floor((double) stored / max * 15.0) : 0;
        }
        return 0;
    }
}
