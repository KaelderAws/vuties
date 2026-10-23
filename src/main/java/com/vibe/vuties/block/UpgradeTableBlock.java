package com.vibe.vuties.block;

import com.mojang.serialization.MapCodec;
import com.vibe.vuties.menu.UpgradeTableMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class UpgradeTableBlock extends Block {
    public static final MapCodec<UpgradeTableBlock> CODEC = simpleCodec(UpgradeTableBlock::new);
    private static final Component CONTAINER_TITLE = Component.translatable("container.vuties.upgrade_table");

    public UpgradeTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            player.openMenu(state.getMenuProvider(level, pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new SimpleMenuProvider((containerId, playerInventory, player) ->
            new UpgradeTableMenu(containerId, playerInventory, ContainerLevelAccess.create(level, pos)),
            CONTAINER_TITLE);
    }
}
