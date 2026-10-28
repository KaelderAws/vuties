package com.vibe.vuties.block.entity;

import com.vibe.vuties.energy.EnergyCableNetwork;
import com.vibe.vuties.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

public class EnergyCableBlockEntity extends BlockEntity {

    @Nullable
    private EnergyCableNetwork network = null;
    private boolean isMaster = false;

    private final IEnergyStorage[] directionalStorages = new IEnergyStorage[6];
    private final IEnergyStorage defaultStorage = createStorage(null);

    public EnergyCableBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.ENERGY_CABLE.get(), pos, blockState);
        for (int i = 0; i < 6; i++) {
            directionalStorages[i] = createStorage(Direction.values()[i]);
        }
    }

    private IEnergyStorage createStorage(@Nullable Direction side) {
        return new IEnergyStorage() {
            @Override
            public int receiveEnergy(int maxReceive, boolean simulate) {
                if (level == null || level.isClientSide) return 0;
                if (network == null || !network.isValid()) {
                    network = EnergyCableNetwork.buildNetwork(level, worldPosition);
                }
                BlockPos fromPos = side != null ? worldPosition.relative(side) : worldPosition;
                return network.insertEnergy(level, maxReceive, simulate, fromPos);
            }

            @Override
            public int extractEnergy(int maxExtract, boolean simulate) {
                return 0;
            }

            @Override
            public int getEnergyStored() {
                return 0;
            }

            @Override
            public int getMaxEnergyStored() {
                return EnergyCableNetwork.MAX_TRANSFER_RATE;
            }

            @Override
            public boolean canExtract() {
                return false;
            }

            @Override
            public boolean canReceive() {
                return true;
            }
        };
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, EnergyCableBlockEntity be) {
        if (be.network == null || !be.network.isValid()) {
            be.network = EnergyCableNetwork.buildNetwork(level, pos);
        }

        if (be.isMaster && be.network != null && be.network.isValid()) {
            be.network.tick(level);
        }
    }

    public IEnergyStorage getEnergyStorage(@Nullable Direction side) {
        if (side == null) return defaultStorage;
        return directionalStorages[side.get3DDataValue()];
    }

    public void setNetwork(@Nullable EnergyCableNetwork network) {
        this.network = network;
    }

    public void clearNetwork() {
        this.network = null;
        this.isMaster = false;
    }

    public void invalidateNetwork() {
        if (this.network != null) {
            this.network.invalidate();
            this.network = null;
        }
        this.isMaster = false;
    }

    public void setMaster(boolean master) {
        this.isMaster = master;
    }

    public boolean isMaster() {
        return isMaster;
    }

    @Nullable
    public EnergyCableNetwork getNetwork() {
        return network;
    }
}
