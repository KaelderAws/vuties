package com.vibe.vuties.block.entity;

import com.vibe.vuties.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

public class CreativeEnergyCellBlockEntity extends BlockEntity implements IEnergyStorage {

    public CreativeEnergyCellBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.CREATIVE_ENERGY_CELL.get(), pos, blockState);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CreativeEnergyCellBlockEntity be) {
        // Automatically push energy to adjacent receivers every tick (capped to 100,000 FE/t to prevent integer overflow)
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            if (!level.isLoaded(neighborPos)) continue;
            IEnergyStorage neighbor = level.getCapability(Capabilities.EnergyStorage.BLOCK, neighborPos, dir.getOpposite());
            if (neighbor != null && neighbor.canReceive()) {
                neighbor.receiveEnergy(100000, false);
            }
        }
    }

    public IEnergyStorage getEnergyStorage(@Nullable Direction side) {
        return this;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        return maxReceive;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return maxExtract;
    }

    @Override
    public int getEnergyStored() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int getMaxEnergyStored() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean canExtract() {
        return true;
    }

    @Override
    public boolean canReceive() {
        return true;
    }
}
