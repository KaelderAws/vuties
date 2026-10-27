package com.vibe.vuties.logic;

import com.vibe.vuties.item.JetBootsItem;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SlideManager {

    public static final double SLIDE_CRUISE_SPEED = 0.725; // Базовая скорость (только Ctrl) ~14.5 блоков/с
    public static final double SLIDE_BOOST_SPEED = 0.98;   // Ускоренное скольжение (WASD + Ctrl) ~19.6 блоков/с

    private static final Map<UUID, Boolean> activeSliders = new ConcurrentHashMap<>();

    public static boolean isSliding(Player player) {
        if (player == null) return false;
        return activeSliders.getOrDefault(player.getUUID(), false);
    }

    public static void setSliding(Player player, boolean sliding) {
        if (player == null) return;
        if (sliding) {
            activeSliders.put(player.getUUID(), true);
        } else {
            activeSliders.remove(player.getUUID());
        }
    }

    public static boolean canSlide(Player player) {
        if (player == null || player.isSpectator()) return false;
        ItemStack feet = player.getItemBySlot(EquipmentSlot.FEET);
        if (!(feet.getItem() instanceof JetBootsItem)) return false;
        return JetBootsItem.hasSlideUpgrade(feet);
    }

    /**
     * Checks whether the player is currently on or directly above a surface (solid ground, water, or lava).
     */
    public static boolean isNearSurface(Player player) {
        if (player.onGround() || player.isInWater() || player.isInLava()) {
            return true;
        }

        Level level = player.level();
        Vec3 motion = player.getDeltaMovement();
        double checkDown = Math.max(0.85, Math.abs(motion.y) + 0.45);
        AABB footCheck = player.getBoundingBox().inflate(0.1, 0.0, 0.1).move(0, -checkDown, 0);

        if (!level.noCollision(player, footCheck)) {
            return true;
        }

        // Fluid check in the bounding area below feet
        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
        int minX = Mth.floor(footCheck.minX);
        int maxX = Mth.floor(footCheck.maxX);
        int minY = Mth.floor(footCheck.minY);
        int maxY = Mth.floor(footCheck.maxY);
        int minZ = Mth.floor(footCheck.minZ);
        int maxZ = Mth.floor(footCheck.maxZ);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    mpos.set(x, y, z);
                    FluidState fluid = level.getFluidState(mpos);
                    if (!fluid.isEmpty()) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Prevents sinking into water and lava while sliding like Frozone.
     * Intercepts falling landings before the player can dip beneath the liquid surface.
     */
    public static void applyLiquidSkimming(Player player) {
        Level level = player.level();
        Vec3 motion = player.getDeltaMovement();
        double py = player.getY();
        BlockPos pos = player.blockPosition();

        double highestFluidY = Double.NEGATIVE_INFINITY;

        // Check fluid in the column (1 block above, feet block, and 2 blocks below for high-speed falls)
        for (int dy = 1; dy >= -2; dy--) {
            BlockPos checkPos = pos.above(dy);
            FluidState fluid = level.getFluidState(checkPos);
            if (!fluid.isEmpty()) {
                double surfaceY = checkPos.getY() + fluid.getHeight(level, checkPos);
                if (surfaceY > highestFluidY) {
                    highestFluidY = surfaceY;
                }
            }
        }

        if (highestFluidY > Double.NEGATIVE_INFINITY) {
            double nextY = py + motion.y;

            // Если игрок приземляется на жидкость из прыжка/падения или уже на уровне поверхности:
            if (py <= highestFluidY + 0.25 || nextY <= highestFluidY) {
                double snapY = Math.max(py, highestFluidY);
                if (py < highestFluidY || nextY < highestFluidY) {
                    snapY = highestFluidY;
                }
                player.setPos(player.getX(), snapY, player.getZ());
                player.setDeltaMovement(motion.x, Math.max(motion.y, 0.0), motion.z);
                player.setOnGround(true);
                player.resetFallDistance();
                player.fallDistance = 0.0f;
            }
        }
    }

    public static boolean isOverWater(Level level, BlockPos pos) {
        return level.getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER) ||
               level.getFluidState(pos.below()).is(net.minecraft.tags.FluidTags.WATER);
    }

    public static boolean isOverLava(Level level, BlockPos pos) {
        return level.getFluidState(pos).is(net.minecraft.tags.FluidTags.LAVA) ||
               level.getFluidState(pos.below()).is(net.minecraft.tags.FluidTags.LAVA);
    }

    /**
     * Server tick for sliding player. Resets fall distance and clears fire.
     */
    public static void serverTick(Player player) {
        if (!isSliding(player)) return;

        if (!canSlide(player)) {
            setSliding(player, false);
            return;
        }

        // Always extinguish fire when sliding in jet boots
        player.clearFire();
        player.resetFallDistance();
        player.moveDist = 0.0f; // Prevent server-side walking/step sounds from triggering
    }
}
