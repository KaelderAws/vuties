package com.vibe.vuties.event;

import com.vibe.vuties.Vuties;
import com.vibe.vuties.item.AceHelmetItem;
import com.vibe.vuties.item.RotorpackItem;
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
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Vuties.MODID)
public class CommonEvents {

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player) {
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

        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.getItem() instanceof RotorpackItem && RotorpackItem.isEngineEnabled(chest)) {
            // Cancel fall damage tracking on both server and client
            player.resetFallDistance();
        }

        // Ночное зрение от шлема аса (без партиклов и без значка в инвентаре/интерфейсе)
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (head.getItem() instanceof AceHelmetItem && AceHelmetItem.isNightVisionActive(head)) {
            MobEffectInstance current = player.getEffect(MobEffects.NIGHT_VISION);
            if (current == null || (!current.showIcon() && current.getDuration() < 200)) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, false, false, false));
            }
        } else {
            MobEffectInstance current = player.getEffect(MobEffects.NIGHT_VISION);
            if (current != null && !current.showIcon()) {
                player.removeEffect(MobEffects.NIGHT_VISION);
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
