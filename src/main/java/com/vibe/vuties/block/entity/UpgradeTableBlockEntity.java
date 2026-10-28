package com.vibe.vuties.block.entity;

import com.vibe.vuties.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

public class UpgradeTableBlockEntity extends BlockEntity {

    public static final int MAX_ENERGY = 50000;
    public static final int MAX_RECEIVE = 1000;
    public static final int ENERGY_PER_UPGRADE = 1000;
    public static final int INSTALL_COOLDOWN_TICKS = 40; // 2 seconds

    private final EnergyStorage energyStorage = new EnergyStorage(MAX_ENERGY, MAX_RECEIVE, 0) {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = super.receiveEnergy(maxReceive, simulate);
            if (received > 0 && !simulate) {
                setChanged();
            }
            return received;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int extracted = super.extractEnergy(maxExtract, simulate);
            if (extracted > 0 && !simulate) {
                setChanged();
            }
            return extracted;
        }
    };

    private ItemStack gearItem = ItemStack.EMPTY;
    private int installCooldown = 0;
    private int maxCooldown = INSTALL_COOLDOWN_TICKS;

    private final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energyStorage.getEnergyStored() & 0xFFFF;
                case 1 -> (energyStorage.getEnergyStored() >> 16) & 0xFFFF;
                case 2 -> energyStorage.getMaxEnergyStored() & 0xFFFF;
                case 3 -> (energyStorage.getMaxEnergyStored() >> 16) & 0xFFFF;
                case 4 -> installCooldown;
                case 5 -> maxCooldown;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 4 -> installCooldown = value;
                case 5 -> maxCooldown = value;
            }
        }

        @Override
        public int getCount() {
            return 6;
        }
    };

    public UpgradeTableBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.UPGRADE_TABLE.get(), pos, blockState);
    }

    public ContainerData getContainerData() {
        return containerData;
    }

    public EnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    public IEnergyStorage getEnergyStorage(@Nullable Direction side) {
        return energyStorage;
    }

    public int getEnergyStored() {
        return energyStorage.getEnergyStored();
    }

    public int getMaxEnergyStored() {
        return energyStorage.getMaxEnergyStored();
    }

    public boolean consumeEnergyForUpgrade() {
        if (energyStorage.getEnergyStored() >= ENERGY_PER_UPGRADE) {
            energyStorage.extractEnergy(ENERGY_PER_UPGRADE, false);
            setChanged();
            return true;
        }
        return false;
    }

    public int getInstallCooldown() {
        return installCooldown;
    }

    public void startInstallationCooldown() {
        this.installCooldown = INSTALL_COOLDOWN_TICKS;
        this.maxCooldown = INSTALL_COOLDOWN_TICKS;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.playSound(null, worldPosition, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.6f, 1.4f);
        }
    }

    public ItemStack getGearItem() {
        return gearItem;
    }

    public void setGearItem(ItemStack gearItem) {
        this.gearItem = gearItem;
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, UpgradeTableBlockEntity be) {
        if (be.installCooldown > 0) {
            be.installCooldown--;
            be.setChanged();
            if (be.installCooldown == 0) {
                level.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.7f, 1.0f);
            }
        }

        // Pull energy from adjacent energy-capable blocks (Energy Cell, etc.)
        if (level.getGameTime() % 5 == 0 && be.energyStorage.getEnergyStored() < be.energyStorage.getMaxEnergyStored()) {
            for (Direction dir : Direction.values()) {
                IEnergyStorage neighbor = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos.relative(dir), dir.getOpposite());
                if (neighbor != null && neighbor.canExtract()) {
                    int needed = be.energyStorage.getMaxEnergyStored() - be.energyStorage.getEnergyStored();
                    int extracted = neighbor.extractEnergy(Math.min(needed, MAX_RECEIVE), false);
                    if (extracted > 0) {
                        be.energyStorage.receiveEnergy(extracted, false);
                        be.setChanged();
                        break;
                    }
                }
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Energy", energyStorage.serializeNBT(registries));
        tag.putInt("InstallCooldown", installCooldown);
        tag.putInt("MaxCooldown", maxCooldown);
        if (!gearItem.isEmpty()) {
            tag.put("Gear", gearItem.save(registries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Energy")) {
            energyStorage.deserializeNBT(registries, tag.get("Energy"));
        }
        this.installCooldown = tag.getInt("InstallCooldown");
        this.maxCooldown = tag.contains("MaxCooldown") ? tag.getInt("MaxCooldown") : INSTALL_COOLDOWN_TICKS;
        if (tag.contains("Gear")) {
            this.gearItem = ItemStack.parse(registries, tag.getCompound("Gear")).orElse(ItemStack.EMPTY);
        } else {
            this.gearItem = ItemStack.EMPTY;
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("EnergyStored", energyStorage.getEnergyStored());
        tag.putInt("InstallCooldown", installCooldown);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            if (tag.contains("EnergyStored")) {
                int target = tag.getInt("EnergyStored");
                int diff = target - energyStorage.getEnergyStored();
                if (diff > 0) {
                    energyStorage.receiveEnergy(diff, false);
                } else if (diff < 0) {
                    energyStorage.extractEnergy(-diff, false);
                }
            }
            if (tag.contains("InstallCooldown")) {
                this.installCooldown = tag.getInt("InstallCooldown");
            }
        }
    }
}
