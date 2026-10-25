package com.vibe.vuties.network;

import com.vibe.vuties.item.AceHelmetItem;
import com.vibe.vuties.item.RotorpackItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModNetwork {

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1.0");

        registrar.playToServer(
            RotorpackInputPayload.TYPE,
            RotorpackInputPayload.STREAM_CODEC,
            ModNetwork::handleInput
        );

        registrar.playToServer(
            RotorpackTogglePayload.TYPE,
            RotorpackTogglePayload.STREAM_CODEC,
            ModNetwork::handleToggle
        );

        registrar.playToServer(
            HelmetTogglePayload.TYPE,
            HelmetTogglePayload.STREAM_CODEC,
            ModNetwork::handleHelmetToggle
        );
    }

    public static void handleInput(RotorpackInputPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player == null) return;

            ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
            if (chest.getItem() instanceof RotorpackItem && RotorpackItem.isEngineEnabled(chest)) {
                // Keep fall distance at zero during active flight or hover
                player.resetFallDistance();

                // Синхронизация режима полёта на элитрах (крыльях) - требуется модуль крыльев
                if (RotorpackItem.hasWingsUpgrade(chest)) {
                    if (payload.sprinting() && !player.onGround()) {
                        if (!player.isFallFlying()) {
                            player.startFallFlying();
                        }
                    } else if (player.isFallFlying()) {
                        player.stopFallFlying();
                    }
                } else {
                    if (player.isFallFlying()) {
                        player.stopFallFlying();
                    }
                }
            }
        });
    }

    public static void handleToggle(RotorpackTogglePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player == null) return;

            ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
            if (chest.getItem() instanceof RotorpackItem) {
                if (payload.action() == 0) {
                    boolean next = !RotorpackItem.isEngineEnabled(chest);
                    RotorpackItem.setEngineEnabled(chest, next);
                    player.displayClientMessage(
                        Component.translatable(next ? "message.vuties.engine_on" : "message.vuties.engine_off"),
                        true
                    );
                } else if (payload.action() == 1) {
                    if (!RotorpackItem.hasHoverUpgrade(chest)) {
                        player.displayClientMessage(
                            Component.translatable("message.vuties.hover_unavailable"),
                            true
                        );
                    } else {
                        boolean next = !RotorpackItem.isHoverMode(chest);
                        RotorpackItem.setHoverMode(chest, next);
                        player.displayClientMessage(
                            Component.translatable(next ? "message.vuties.hover_on" : "message.vuties.hover_off"),
                            true
                        );
                    }
                }
            }
        });
    }

    public static void handleHelmetToggle(HelmetTogglePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player == null) return;

            ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
            if (head.getItem() instanceof AceHelmetItem) {
                boolean next = !AceHelmetItem.isHelmetOpen(head);
                AceHelmetItem.setHelmetOpen(head, next);
                player.displayClientMessage(
                    Component.translatable(next ? "message.vuties.helmet_open" : "message.vuties.helmet_closed"),
                    true
                );
            }
        });
    }
}
