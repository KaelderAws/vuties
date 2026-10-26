package com.vibe.vuties.event;

import com.vibe.vuties.Vuties;
import com.vibe.vuties.item.AceHelmetItem;
import com.vibe.vuties.item.RotorpackItem;
import com.vibe.vuties.logic.PhaseShiftManager;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;
import net.neoforged.neoforge.event.entity.living.LivingDrownEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Vuties.MODID)
public class CommonEvents {

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (PhaseShiftManager.isPhaseShiftActive(player)) {
                // Фазовый сдвиг обеспечивает полную неуязвимость
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (PhaseShiftManager.isPhaseShiftActive(player)) {
                event.setCanceled(true);
                return;
            }

            ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
            if (chest.getItem() instanceof RotorpackItem && RotorpackItem.isEngineEnabled(chest)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player == null) return;

        // Фазовый сдвиг поножей аса
        PhaseShiftManager.tickPlayer(player);
        if (PhaseShiftManager.isPhaseShiftActive(player)) {
            player.noPhysics = true;
            player.resetFallDistance();
        }

        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.getItem() instanceof RotorpackItem && RotorpackItem.isEngineEnabled(chest)) {
            // Cancel fall damage tracking on both server and client
            player.resetFallDistance();
        }

        // Ночное зрение от шлема аса (без партиклов и без значка в инвентаре/интерфейсе)
        // Обновляем на сервере с запасом длительности (>200 тиков), чтобы ванильный клиент никогда не включал мерцание
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!player.level().isClientSide()) {
            if (head.getItem() instanceof AceHelmetItem && AceHelmetItem.isNightVisionActive(head)) {
                MobEffectInstance current = player.getEffect(MobEffects.NIGHT_VISION);
                if (current == null || (!current.showIcon() && current.getDuration() <= 240)) {
                    player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 340, 0, false, false, false));
                }
            } else {
                MobEffectInstance current = player.getEffect(MobEffects.NIGHT_VISION);
                if (current != null && !current.showIcon()) {
                    player.removeEffect(MobEffects.NIGHT_VISION);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingBreathe(LivingBreatheEvent event) {
        if (event.getEntity() instanceof Player player) {
            ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
            if (head.getItem() instanceof AceHelmetItem && AceHelmetItem.isWaterBreathingActive(head)) {
                event.setCanBreathe(true);
                event.setRefillAirAmount(4);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDrown(LivingDrownEvent event) {
        if (event.getEntity() instanceof Player player) {
            ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
            if (head.getItem() instanceof AceHelmetItem && AceHelmetItem.isWaterBreathingActive(head)) {
                event.setCanceled(true);
            }
        }
    }
}
