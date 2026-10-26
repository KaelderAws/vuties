package com.vibe.vuties.logic;

import com.vibe.vuties.init.ModItems;
import com.vibe.vuties.item.ExoLegsItem;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PhaseShiftManager {

    public static final int PHASE_DURATION_TICKS = 24;
    public static final int COOLDOWN_TICKS = 35;
    public static final double DASH_SPEED = 1.25;

    public static final Map<UUID, Integer> activePhases = new ConcurrentHashMap<>();

    public static boolean isPhaseShiftActive(Player player) {
        if (player == null) return false;
        return activePhases.getOrDefault(player.getUUID(), 0) > 0;
    }

    public static int getRemainingPhaseTicks(Player player) {
        if (player == null) return 0;
        return activePhases.getOrDefault(player.getUUID(), 0);
    }

    public static boolean canPhaseShift(Player player) {
        if (player == null || player.isSpectator()) return false;
        ItemStack legs = player.getItemBySlot(EquipmentSlot.LEGS);
        if (!(legs.getItem() instanceof ExoLegsItem)) return false;
        // Фазовый сдвиг доступен ТОЛЬКО при установленном фазовом модуле
        if (!ExoLegsItem.hasPhaseUpgrade(legs)) return false;
        return !player.getCooldowns().isOnCooldown(ModItems.EXO_LEGS.get());
    }

    public static void executePhaseShift(Player player, Vec3 movement) {
        if (!canPhaseShift(player)) return;

        // Физическое перемещение и фазирование сквозь объекты
        player.setDeltaMovement(movement);
        player.noPhysics = true;
        player.hurtMarked = true;
        player.resetFallDistance();

        // Запуск таймера и перезарядки
        player.getCooldowns().addCooldown(ModItems.EXO_LEGS.get(), COOLDOWN_TICKS);
        activePhases.put(player.getUUID(), PHASE_DURATION_TICKS);

        // Звуки квантового фазового сдвига
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.7F);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2F, 1.7F);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 0.9F, 1.5F);
    }

    public static void steerPhaseShift(Player player, Vec3 movement) {
        if (player == null) return;
        player.setDeltaMovement(movement);
        player.noPhysics = true;
        player.hurtMarked = true;
        player.resetFallDistance();
    }

    public static void endPhaseShift(Player player) {
        if (player == null) return;
        activePhases.remove(player.getUUID());
        ensureSafeExit(player);
    }

    public static void tickPlayer(Player player) {
        if (player == null) return;
        UUID uuid = player.getUUID();
        Integer remaining = activePhases.get(uuid);

        if (remaining != null && remaining > 0) {
            int next = remaining - 1;
            if (next <= 0) {
                activePhases.remove(uuid);
                ensureSafeExit(player);
            } else {
                activePhases.put(uuid, next);
                player.noPhysics = true;
                player.resetFallDistance();

                // Отражение любых снарядов в радиусе 2.5 блоков
                AABB bounds = player.getBoundingBox().inflate(2.5);
                for (Projectile p : player.level().getEntitiesOfClass(Projectile.class, bounds, proj -> proj.getOwner() != player)) {
                    Vec3 away = p.position().subtract(player.position()).normalize().scale(1.5);
                    p.setDeltaMovement(away.x, 0.35, away.z);
                    p.hurtMarked = true;
                }
            }
        }
    }

    public static void ensureSafeExit(Player player) {
        player.noPhysics = false;
        Level level = player.level();
        AABB box = player.getBoundingBox();

        // Проверка: если игрок закончил рывок внутри твердого блока, мягко перемещаем в ближайший воздух
        if (!level.noCollision(player, box)) {
            BlockPos base = player.blockPosition();
            BlockPos best = null;
            double bestDist = Double.MAX_VALUE;

            for (int dx = -2; dx <= 2; dx++) {
                for (int dy = -1; dy <= 3; dy++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        BlockPos p = base.offset(dx, dy, dz);
                        AABB candidate = box.move(p.getX() + 0.5 - player.getX(), p.getY() - player.getY(), p.getZ() + 0.5 - player.getZ());
                        if (level.noCollision(player, candidate)) {
                            double dist = p.distSqr(base);
                            if (dist < bestDist) {
                                bestDist = dist;
                                best = p;
                            }
                        }
                    }
                }
            }

            if (best != null) {
                player.teleportTo(best.getX() + 0.5, best.getY(), best.getZ() + 0.5);
            }
        }
    }
}
