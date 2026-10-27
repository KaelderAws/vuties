package com.vibe.vuties.entity;

import com.vibe.vuties.block.WheelBlock;
import com.vibe.vuties.block.entity.WheelBlockEntity;
import com.vibe.vuties.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;

public class WheelRunnerEntity extends Entity {
    private BlockPos masterPos = BlockPos.ZERO;

    public WheelRunnerEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.blocksBuilding = false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    public void setMasterPos(BlockPos pos) {
        this.masterPos = pos;
    }

    public BlockPos getMasterPos() {
        return this.masterPos;
    }

    public void updateRunningState(boolean running, boolean sprinting) {
        if (!level().isClientSide && masterPos != null) {
            if (level().getBlockEntity(masterPos) instanceof WheelBlockEntity wheelBE) {
                wheelBE.setRunnerActive(running, sprinting);
            }
        }
    }

    @Override
    public boolean shouldRiderSit() {
        return false;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float partialTick) {
        return Vec3.ZERO;
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        if (!this.hasPassenger(passenger)) return;
        moveFunction.accept(passenger, this.getX(), this.getY(), this.getZ());
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        Direction facing = getDirection();
        Direction back = facing.getOpposite();
        Direction right = facing.getClockWise();
        Direction left = facing.getCounterClockWise();

        List<BlockPos> candidatePositions = new ArrayList<>();
        // Priority 1: Step off behind the wheel
        candidatePositions.add(masterPos.relative(back));
        // Priority 2: Sides
        candidatePositions.add(masterPos.relative(right));
        candidatePositions.add(masterPos.relative(left));
        // Priority 3: Diagonals behind
        candidatePositions.add(masterPos.relative(back).relative(right));
        candidatePositions.add(masterPos.relative(back).relative(left));
        // Priority 4: Front & front diagonals
        candidatePositions.add(masterPos.relative(facing));
        candidatePositions.add(masterPos.relative(facing).relative(right));
        candidatePositions.add(masterPos.relative(facing).relative(left));
        // Priority 5: 2 blocks away
        candidatePositions.add(masterPos.relative(back, 2));
        candidatePositions.add(masterPos.relative(right, 2));
        candidatePositions.add(masterPos.relative(left, 2));
        candidatePositions.add(masterPos.relative(facing, 2));

        for (BlockPos basePos : candidatePositions) {
            // Check current Y, 1 step up, and 1 step down
            for (int yOffset : new int[]{0, 1, -1}) {
                BlockPos feetPos = basePos.above(yOffset);
                BlockPos headPos = feetPos.above();
                BlockPos groundPos = feetPos.below();

                if (isPassable(feetPos) && isPassable(headPos) && hasGround(groundPos)) {
                    return new Vec3(feetPos.getX() + 0.5, feetPos.getY(), feetPos.getZ() + 0.5);
                }
            }
        }

        // Secondary check: any passable space even if ground is non-solid
        for (BlockPos basePos : candidatePositions) {
            BlockPos headPos = basePos.above();
            if (isPassable(basePos) && isPassable(headPos)) {
                return new Vec3(basePos.getX() + 0.5, basePos.getY(), basePos.getZ() + 0.5);
            }
        }

        // Fallback: behind the wheel
        BlockPos fallback = masterPos.relative(back);
        return new Vec3(fallback.getX() + 0.5, masterPos.getY(), fallback.getZ() + 0.5);
    }

    private boolean isPassable(BlockPos pos) {
        return level().getBlockState(pos).getCollisionShape(level(), pos).isEmpty();
    }

    private boolean hasGround(BlockPos pos) {
        return !level().getBlockState(pos).getCollisionShape(level(), pos).isEmpty();
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide) {
            if (getPassengers().isEmpty()) {
                discard();
                return;
            }

            if (!(level().getBlockEntity(masterPos) instanceof WheelBlockEntity wheelBE) ||
                !level().getBlockState(masterPos).is(ModBlocks.WHEEL.get())) {
                ejectPassengers();
                discard();
                return;
            }

            setPos(masterPos.getX() + 0.5, masterPos.getY() + 1.05, masterPos.getZ() + 0.5);
            Direction facing = wheelBE.getBlockState().getValue(WheelBlock.FACING);
            setYRot(facing.toYRot());
        }
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (!level().isClientSide) {
            if (level().getBlockEntity(masterPos) instanceof WheelBlockEntity wheelBE) {
                wheelBE.onRunnerDismounted();
            }
            discard();
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("MasterX")) {
            this.masterPos = new BlockPos(tag.getInt("MasterX"), tag.getInt("MasterY"), tag.getInt("MasterZ"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (masterPos != null) {
            tag.putInt("MasterX", masterPos.getX());
            tag.putInt("MasterY", masterPos.getY());
            tag.putInt("MasterZ", masterPos.getZ());
        }
    }
}
