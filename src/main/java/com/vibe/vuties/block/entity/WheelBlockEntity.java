package com.vibe.vuties.block.entity;

import com.vibe.vuties.block.WheelBlock;
import com.vibe.vuties.entity.WheelRunnerEntity;
import com.vibe.vuties.init.ModBlockEntities;
import com.vibe.vuties.init.ModBlocks;
import com.vibe.vuties.init.ModEntityTypes;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
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
    private int villagerCooldown = 0;

    // Client interpolation fields
    public float rotationAngle = 0.0f;
    public float prevRotationAngle = 0.0f;
    public float rotationSpeed = 0.0f;
    public float riderLimbPos = 0.0f;
    public float prevRiderLimbPos = 0.0f;

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

    public boolean hasOccupant() {
        return runnerEntity != null && runnerEntity.isAlive() && !runnerEntity.getPassengers().isEmpty();
    }

    @Nullable
    public Entity getOccupant() {
        return (runnerEntity != null && runnerEntity.isAlive()) ? runnerEntity.getFirstPassenger() : null;
    }

    @Nullable
    public WheelRunnerEntity getRunnerEntity() {
        return runnerEntity;
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

    public void ejectRunnerWithCooldown(int cooldownTicks) {
        this.villagerCooldown = cooldownTicks;
        WheelRunnerEntity runner = this.runnerEntity;
        this.runnerEntity = null;
        this.isRunning = false;
        this.isSprinting = false;

        if (runner != null && runner.isAlive()) {
            Entity passenger = runner.getFirstPassenger();
            runner.ejectPassengers();
            runner.discard();
            if (passenger instanceof LivingEntity living) {
                living.walkAnimation.setSpeed(0.0F);
                if (level != null && !this.isRemoved() && level.getBlockState(worldPosition).is(ModBlocks.WHEEL.get())) {
                    Direction facing = getBlockState().getValue(WheelBlock.FACING);
                    Direction back = facing.getOpposite();
                    living.push(back.getStepX() * 0.25, 0.1, back.getStepZ() * 0.25);
                }
            }
        }

        if (level != null && !level.isClientSide && !this.isRemoved() && level.getBlockState(worldPosition).is(ModBlocks.WHEEL.get())) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    public void ejectRunner() {
        ejectRunnerWithCooldown(60);
    }

    public void seatVillager(Villager villager) {
        if (level == null || level.isClientSide) return;

        Direction facing = getBlockState().getValue(WheelBlock.FACING);

        WheelRunnerEntity runner = new WheelRunnerEntity(ModEntityTypes.WHEEL_RUNNER.get(), level);
        runner.setMasterPos(worldPosition);
        runner.setPos(worldPosition.getX() + 0.5, worldPosition.getY() + 1.05, worldPosition.getZ() + 0.5);
        runner.setYRot(facing.toYRot());
        runner.setXRot(0.0F);

        villager.getNavigation().stop();
        villager.setDeltaMovement(Vec3.ZERO);
        villager.setYRot(facing.toYRot());
        villager.setXRot(0.0F);
        villager.yRotO = facing.toYRot();
        villager.xRotO = 0.0F;
        villager.setYHeadRot(facing.toYRot());
        villager.setYBodyRot(facing.toYRot());

        level.addFreshEntity(runner);
        villager.startRiding(runner, true);
        setRunnerEntity(runner);
        setRunnerActive(true, false);
    }

    private void checkForNearbyVillager(Level level, BlockPos pos) {
        AABB wheelBox = new AABB(pos.getX(), pos.getY(), pos.getZ(),
                                 pos.getX() + 1.0, pos.getY() + 2.0, pos.getZ() + 1.0);
        AABB captureBox = wheelBox.inflate(0.35, 0.1, 0.35);

        List<Villager> villagers = level.getEntitiesOfClass(Villager.class, captureBox,
            v -> v.isAlive() && !v.isPassenger() && !v.isSleeping());

        if (!villagers.isEmpty()) {
            Villager closest = null;
            double minDist = Double.MAX_VALUE;
            double centerX = pos.getX() + 0.5;
            double centerZ = pos.getZ() + 0.5;
            for (Villager v : villagers) {
                double dist = v.distanceToSqr(centerX, pos.getY() + 1.0, centerZ);
                if (dist < minDist) {
                    minDist = dist;
                    closest = v;
                }
            }
            if (closest != null) {
                seatVillager(closest);
            }
        }
    }

    private static final Map<EntityType<?>, Float> HOSTILE_DISTANCES = Map.ofEntries(
        Map.entry(EntityType.DROWNED, 8.0F),
        Map.entry(EntityType.EVOKER, 12.0F),
        Map.entry(EntityType.HUSK, 8.0F),
        Map.entry(EntityType.ILLUSIONER, 12.0F),
        Map.entry(EntityType.PILLAGER, 15.0F),
        Map.entry(EntityType.RAVAGER, 12.0F),
        Map.entry(EntityType.VEX, 8.0F),
        Map.entry(EntityType.VINDICATOR, 10.0F),
        Map.entry(EntityType.ZOGLIN, 10.0F),
        Map.entry(EntityType.ZOMBIE, 8.0F),
        Map.entry(EntityType.ZOMBIE_VILLAGER, 8.0F)
    );

    public static boolean isVillagerScared(Level level, Villager villager, BlockPos pos) {
        if (villager.getBrain().hasMemoryValue(MemoryModuleType.NEAREST_HOSTILE)
            || villager.getBrain().hasMemoryValue(MemoryModuleType.HURT_BY)
            || villager.getBrain().isActive(Activity.PANIC)) {
            return true;
        }

        AABB searchBox = new AABB(pos).inflate(15.0D);
        List<LivingEntity> enemies = level.getEntitiesOfClass(LivingEntity.class, searchBox,
            e -> e.isAlive() && !e.isSpectator());

        for (LivingEntity enemy : enemies) {
            Float maxDist = HOSTILE_DISTANCES.get(enemy.getType());
            if (maxDist == null) {
                if (enemy instanceof Zombie) {
                    maxDist = 8.0F;
                } else if (enemy instanceof Raider) {
                    maxDist = 12.0F;
                } else if (enemy instanceof Zoglin) {
                    maxDist = 10.0F;
                } else if (enemy instanceof Vex) {
                    maxDist = 8.0F;
                }
            }

            if (maxDist != null) {
                double distSqr = enemy.distanceToSqr(villager);
                if (distSqr <= maxDist * maxDist) {
                    if (distSqr <= 16.0 || villager.hasLineOfSight(enemy) || enemy.hasLineOfSight(villager)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, WheelBlockEntity be) {
        if (be.villagerCooldown > 0) {
            be.villagerCooldown--;
        }

        if (be.runnerEntity != null && (!be.runnerEntity.isAlive() || be.runnerEntity.getPassengers().isEmpty())) {
            be.onRunnerDismounted();
        }

        if (!be.hasOccupant() && be.villagerCooldown <= 0) {
            be.checkForNearbyVillager(level, pos);
        }

        if (be.hasOccupant()) {
            Entity occupant = be.getOccupant();
            if (occupant instanceof Villager villager) {
                boolean scared = isVillagerScared(level, villager, pos);
                be.setRunnerActive(true, scared);
                int feGen = scared ? 30 : 15;
                be.pushEnergyToNeighbors(level, pos, state, feGen);
                be.runTicks++;

                if (be.runTicks % 10 == 0) {
                    level.sendBlockUpdated(pos, state, state, 2);
                }
            } else if (occupant instanceof Player player) {
                if (be.isRunning) {
                    int feGen = be.isSprinting ? 40 : 20;
                    be.pushEnergyToNeighbors(level, pos, state, feGen);
                    be.runTicks++;

                    float exhaustion = be.isSprinting ? 0.05F : 0.015F;
                    player.causeFoodExhaustion(exhaustion);
                    if (player.getFoodData().getFoodLevel() <= 6 && be.isSprinting) {
                        be.isSprinting = false;
                    }

                    if (be.runTicks % 10 == 0) {
                        level.sendBlockUpdated(pos, state, state, 2);
                    }
                } else {
                    be.runTicks = 0;
                }
            }
        } else {
            be.runTicks = 0;
            if (be.isRunning) {
                be.isRunning = false;
                be.isSprinting = false;
                level.sendBlockUpdated(pos, state, state, 2);
            }
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
        be.prevRiderLimbPos = be.riderLimbPos;

        float targetSpeed;
        if (be.isRunning) {
            targetSpeed = be.isSprinting ? 18.0f : 8.0f;
        } else {
            targetSpeed = 0.0f;
        }

        be.rotationSpeed = Mth.approach(be.rotationSpeed, targetSpeed, 0.8f);
        be.rotationAngle = (be.rotationAngle + be.rotationSpeed) % 360.0f;

        if (be.rotationSpeed > 0.01f) {
            be.riderLimbPos += be.rotationSpeed * 0.055f;
        }

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
        tag.putInt("VillagerCooldown", villagerCooldown);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.isRunning = tag.getBoolean("Running");
        this.isSprinting = tag.getBoolean("Sprinting");
        this.villagerCooldown = tag.getInt("VillagerCooldown");
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
