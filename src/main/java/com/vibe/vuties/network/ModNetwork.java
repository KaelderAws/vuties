package com.vibe.vuties.network;

import com.vibe.vuties.entity.WheelRunnerEntity;
import com.vibe.vuties.item.AceHelmetItem;
import com.vibe.vuties.item.ExoLegsItem;
import com.vibe.vuties.item.GauntletItem;
import com.vibe.vuties.logic.PhaseShiftManager;
import com.vibe.vuties.logic.SlideManager;
import com.vibe.vuties.item.RotorpackItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
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

        registrar.playToServer(
            NightVisionTogglePayload.TYPE,
            NightVisionTogglePayload.STREAM_CODEC,
            ModNetwork::handleNightVisionToggle
        );

        registrar.playToServer(
            PhaseShiftPayload.TYPE,
            PhaseShiftPayload.STREAM_CODEC,
            ModNetwork::handlePhaseShift
        );

        registrar.playToServer(
            SlidePayload.TYPE,
            SlidePayload.STREAM_CODEC,
            ModNetwork::handleSlide
        );

        registrar.playToServer(
            WheelRunPayload.TYPE,
            WheelRunPayload.STREAM_CODEC,
            ModNetwork::handleWheelRun
        );

        registrar.playToServer(
            GauntletModePayload.TYPE,
            GauntletModePayload.STREAM_CODEC,
            ModNetwork::handleGauntletMode
        );

        registrar.playToServer(
            DrillMinePayload.TYPE,
            DrillMinePayload.STREAM_CODEC,
            ModNetwork::handleDrillMine
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
                    if (RotorpackItem.hasHoverUpgrade(chest)) {
                        boolean next = !RotorpackItem.isHoverMode(chest);
                        RotorpackItem.setHoverMode(chest, next);
                        player.displayClientMessage(
                            Component.translatable(next ? "message.vuties.hover_on" : "message.vuties.hover_off"),
                            true
                        );
                    } else {
                        player.displayClientMessage(
                            Component.translatable("message.vuties.hover_unavailable"),
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

    public static void handleNightVisionToggle(NightVisionTogglePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player == null) return;

            ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
            if (head.getItem() instanceof AceHelmetItem) {
                if (!AceHelmetItem.hasNightVisionUpgrade(head)) {
                    player.displayClientMessage(
                        Component.translatable("message.vuties.night_vision_unavailable"),
                        true
                    );
                } else if (AceHelmetItem.isHelmetOpen(head)) {
                    player.displayClientMessage(
                        Component.translatable("message.vuties.night_vision_mask_open"),
                        true
                    );
                } else {
                    boolean next = !AceHelmetItem.isNightVisionEnabled(head);
                    AceHelmetItem.setNightVisionEnabled(head, next);
                    player.displayClientMessage(
                        Component.translatable(next ? "message.vuties.night_vision_on" : "message.vuties.night_vision_off"),
                        true
                    );
                }
            }
        });
    }

    public static void handlePhaseShift(PhaseShiftPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player == null) return;

            ItemStack legs = player.getItemBySlot(EquipmentSlot.LEGS);
            if (legs.getItem() instanceof ExoLegsItem) {
                if (payload.action() == 0) {
                    if (PhaseShiftManager.canPhaseShift(player)) {
                        PhaseShiftManager.executePhaseShift(player, new Vec3(payload.vx(), payload.vy(), payload.vz()));
                    }
                } else if (payload.action() == 1) {
                    if (PhaseShiftManager.isPhaseShiftActive(player)) {
                        PhaseShiftManager.steerPhaseShift(player, new Vec3(payload.vx(), payload.vy(), payload.vz()));
                    }
                } else if (payload.action() == 2) {
                    PhaseShiftManager.endPhaseShift(player);
                }
            }
        });
    }

    public static void handleSlide(SlidePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player == null) return;

            SlideManager.setSliding(player, payload.sliding());
            if (payload.sliding()) {
                player.clearFire();
                player.resetFallDistance();
            }
        });
    }

    public static void handleWheelRun(WheelRunPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player == null) return;
            if (player.getVehicle() instanceof WheelRunnerEntity runner) {
                runner.updateRunningState(payload.running(), payload.sprinting());
            }
        });
    }

    public static void handleGauntletMode(GauntletModePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player == null) return;

            ItemStack stack = payload.handSlot() == 0 ? player.getMainHandItem() : player.getOffhandItem();
            if (stack.getItem() instanceof GauntletItem) {
                if (payload.mode() == GauntletItem.MODE_DRILL && !GauntletItem.hasDrillUpgrade(stack)) {
                    return;
                }
                GauntletItem.setMode(stack, payload.mode());
            }
        });
    }

    public static void handleDrillMine(DrillMinePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player == null) return;

            ItemStack main = player.getMainHandItem();
            ItemStack off = player.getOffhandItem();
            boolean hasDrill = (main.getItem() instanceof GauntletItem && GauntletItem.isDrillActive(main))
                    || (off.getItem() instanceof GauntletItem && GauntletItem.isDrillActive(off));

            if (!hasDrill) return;

            BlockPos pos = payload.pos();
            Level level = player.level();
            BlockState state = level.getBlockState(pos);
            if (state.getDestroySpeed(level, pos) >= 0 && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 36.0) {
                level.destroyBlock(pos, true, player);
            }
        });
    }
}
