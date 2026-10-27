package com.vibe.vuties.block.entity;

import com.vibe.vuties.block.WheelBlock;
import com.vibe.vuties.entity.WheelRunnerEntity;
import com.vibe.vuties.init.ModBlockEntities;
import com.vibe.vuties.init.ModBlocks;
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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class WheelBlockEntity extends BlockEntity implements GeoBlockEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public static class WheelEnergyStorage implements IEnergyStorage {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return 0;
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
            return 0;
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return false;
        }
    }

    private final WheelEnergyStorage energyStorage = new WheelEnergyStorage();

    private boolean isRunning = false;
    private boolean isSprinting = false;
    private int runTicks = 0;

    // Client interpolation fields
    public float rotationAngle = 0.0f;
    public float prevRotationAngle = 0.0f;
    public float rotationSpeed = 0.0f;

    @Nullable
    private WheelRunnerEntity runnerEntity = null;

    public WheelBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.WHEEL.get(), pos, blockState);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    public WheelEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    public IEnergyStorage getEnergyStorage(@Nullable Direction side) {
        return energyStorage;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public boolean isSprinting() {
        return isSprinting;
    }

    public void setRunnerEntity(@Nullable WheelRunnerEntity entity) {
        this.runnerEntity = entity;
    }

    public void setRunnerActive(boolean running, boolean sprinting) {
        boolean stateChanged = (this.isRunning != running || this.isSprinting != sprinting);
        this.isRunning = running;
        this.isSprinting = sprinting;
        if (stateChanged && level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    public void onRunnerDismounted() {
        this.isRunning = false;
        this.isSprinting = false;
        this.runnerEntity = null;
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    public void ejectRunner() {
        if (runnerEntity != null && runnerEntity.isAlive()) {
            runnerEntity.ejectPassengers();
            runnerEntity.discard();
        }
        this.runnerEntity = null;
        this.isRunning = false;
        this.isSprinting = false;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, WheelBlockEntity be) {
        if (be.runnerEntity != null && (!be.runnerEntity.isAlive() || be.runnerEntity.getPassengers().isEmpty())) {
            be.onRunnerDismounted();
        }

        if (be.isRunning) {
            int feGen = be.isSprinting ? 40 : 20;
            be.pushEnergyToNeighbors(level, pos, state, feGen);
            be.runTicks++;

            if (be.runnerEntity != null && be.runnerEntity.getFirstPassenger() instanceof Player player) {
                float exhaustion = be.isSprinting ? 0.05F : 0.015F;
                player.causeFoodExhaustion(exhaustion);
                if (player.getFoodData().getFoodLevel() <= 6 && be.isSprinting) {
                    be.isSprinting = false;
                }
            }

            if (be.runTicks % 10 == 0) {
                level.sendBlockUpdated(pos, state, state, 2);
            }
        } else {
            be.runTicks = 0;
        }
    }

    private void pushEnergyToNeighbors(Level level, BlockPos pos, BlockState state, int amount) {
        int remaining = amount;
        BlockPos[] wheelBlocks = new BlockPos[]{pos, pos.above()};
        for (BlockPos partPos : wheelBlocks) {
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = partPos.relative(dir);
                if (neighborPos.equals(pos) || neighborPos.equals(pos.above())) continue;

                IEnergyStorage receiver = level.getCapability(Capabilities.EnergyStorage.BLOCK, neighborPos, dir.getOpposite());
                if (receiver != null && receiver.canReceive()) {
                    int accepted = receiver.receiveEnergy(remaining, false);
                    remaining -= accepted;
                    if (remaining <= 0) return;
                }
            }
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, WheelBlockEntity be) {
        be.prevRotationAngle = be.rotationAngle;

        float targetSpeed;
        if (be.isRunning) {
            targetSpeed = be.isSprinting ? 18.0f : 8.0f;
        } else {
            targetSpeed = 0.0f;
        }

        be.rotationSpeed = Mth.approach(be.rotationSpeed, targetSpeed, 0.8f);
        be.rotationAngle = (be.rotationAngle + be.rotationSpeed) % 360.0f;

        if (be.rotationSpeed > 1.0f && level.random.nextInt(6) == 0) {
            level.playLocalSound(
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                SoundEvents.WOOD_STEP,
                SoundSource.BLOCKS,
                0.3f,
                0.85f + (be.rotationSpeed / 30.0f),
                false
            );
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Running", isRunning);
        tag.putBoolean("Sprinting", isSprinting);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.isRunning = tag.getBoolean("Running");
        this.isSprinting = tag.getBoolean("Sprinting");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putBoolean("Running", this.isRunning);
        tag.putBoolean("Sprinting", this.isSprinting);
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
            this.isRunning = tag.getBoolean("Running");
            this.isSprinting = tag.getBoolean("Sprinting");
        }
    }
}
