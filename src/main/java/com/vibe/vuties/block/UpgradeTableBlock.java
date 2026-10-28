package com.vibe.vuties.block;

import com.mojang.serialization.MapCodec;
import com.vibe.vuties.block.entity.UpgradeTableBlockEntity;
import com.vibe.vuties.init.ModBlockEntities;
import com.vibe.vuties.menu.UpgradeTableMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class UpgradeTableBlock extends BaseEntityBlock {
    public static final MapCodec<UpgradeTableBlock> CODEC = simpleCodec(UpgradeTableBlock::new);
    private static final Component CONTAINER_TITLE = Component.translatable("container.vuties.upgrade_table");

    public UpgradeTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new UpgradeTableBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType, ModBlockEntities.UPGRADE_TABLE.get(),
            level.isClientSide ? null : UpgradeTableBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            MenuProvider provider = state.getMenuProvider(level, pos);
            if (provider != null) {
                player.openMenu(provider);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        UpgradeTableBlockEntity tableBE = (be instanceof UpgradeTableBlockEntity tbe) ? tbe : null;
        ContainerData data = (tableBE != null) ? tableBE.getContainerData() : new SimpleContainerData(6);
        return new SimpleMenuProvider((containerId, playerInventory, player) ->
            new UpgradeTableMenu(containerId, playerInventory, ContainerLevelAccess.create(level, pos), data, tableBE),
            CONTAINER_TITLE);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof UpgradeTableBlockEntity tableBE) {
                if (!tableBE.getGearItem().isEmpty()) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), tableBE.getGearItem());
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("tooltip.vuties.upgrade_table.desc").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("tooltip.vuties.upgrade_table.capacity").withStyle(ChatFormatting.AQUA));
        tooltipComponents.add(Component.translatable("tooltip.vuties.upgrade_table.input").withStyle(ChatFormatting.GREEN));
        tooltipComponents.add(Component.translatable("tooltip.vuties.upgrade_table.cost").withStyle(ChatFormatting.YELLOW));
    }
}
