package com.vibe.vuties.block.entity;

import com.vibe.vuties.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

public class WheelStructureBlockEntity extends BlockEntity {
    @Nullable
    private BlockPos masterPos = null;

    public WheelStructureBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.WHEEL_STRUCTURE.get(), pos, blockState);
    }

    public void setMasterPos(BlockPos pos) {
        this.masterPos = pos;
        setChanged();
    }

    @Nullable
    public BlockPos getMasterPos() {
        return masterPos;
    }

    @Nullable
    public IEnergyStorage getEnergyStorage(@Nullable Direction side) {
        if (level != null && masterPos != null) {
            if (level.getBlockEntity(masterPos) instanceof WheelBlockEntity masterBE) {
                return masterBE.getEnergyStorage(side);
            }
        }
        return null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (masterPos != null) {
            tag.putLong("Master", masterPos.asLong());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Master")) {
            this.masterPos = BlockPos.of(tag.getLong("Master"));
        }
    }
}
