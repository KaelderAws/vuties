package com.vibe.vuties.client;

import com.vibe.vuties.Vuties;
import com.vibe.vuties.block.entity.WheelBlockEntity;
import com.vibe.vuties.entity.WheelRunnerEntity;
import com.vibe.vuties.network.WheelRunPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CalculatePlayerTurnEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = Vuties.MODID, value = Dist.CLIENT)
public class ClientWheelHandler {

    private static boolean wasRunning = false;
    private static boolean wasSprinting = false;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        if (player.getVehicle() instanceof WheelRunnerEntity runner) {
            // Lock camera to forward direction ONLY in first person
            if (mc.options.getCameraType().isFirstPerson()) {
                Direction facing = runner.getDirection();
                float targetYaw = facing.toYRot();
                player.setYRot(targetYaw);
                player.setYHeadRot(targetYaw);
                player.setYBodyRot(targetYaw);
                player.yRotO = targetYaw;
                player.setXRot(0.0F);
                player.xRotO = 0.0F;
            }

            boolean forward = mc.options.keyUp.isDown();
            boolean sprint = mc.options.keySprint.isDown() || player.isSprinting();

            if (forward) {
                player.walkAnimation.update(sprint ? 1.0F : 0.6F, 0.4F);
            } else {
                player.walkAnimation.update(0.0F, 1.0F);
                player.walkAnimation.setSpeed(0.0F);
            }

            if (player.level().getBlockEntity(runner.getMasterPos()) instanceof WheelBlockEntity wheelBE) {
                wheelBE.setRunnerActive(forward, sprint);
            }

            if (forward != wasRunning || sprint != wasSprinting || player.tickCount % 10 == 0) {
                wasRunning = forward;
                wasSprinting = sprint;
                PacketDistributor.sendToServer(new WheelRunPayload(forward, sprint));
            }
        } else if (wasRunning || wasSprinting) {
            wasRunning = false;
            wasSprinting = false;
            player.walkAnimation.update(0.0F, 1.0F);
            player.walkAnimation.setSpeed(0.0F);
        }
    }

    private static float savedYRot;
    private static float savedYRotO;
    private static float savedYBodyRot;
    private static float savedYBodyRotO;
    private static float savedYHeadRot;
    private static float savedYHeadRotO;
    private static float savedXRot;
    private static float savedXRotO;
    private static boolean modifiedRotations = false;

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (player.getVehicle() instanceof WheelRunnerEntity runner) {
            // Stand rather than sit
            event.getRenderer().getModel().riding = false;

            Minecraft mc = Minecraft.getInstance();
            if (player == mc.player && !mc.options.keyUp.isDown()) {
                player.walkAnimation.update(0.0F, 1.0F);
                player.walkAnimation.setSpeed(0.0F);
            }

            // Lock model facing forward in the wheel even in 3rd person camera
            Direction facing = runner.getDirection();
            float facingYaw = facing.toYRot();

            savedYRot = player.getYRot();
            savedYRotO = player.yRotO;
            savedYBodyRot = player.yBodyRot;
            savedYBodyRotO = player.yBodyRotO;
            savedYHeadRot = player.yHeadRot;
            savedYHeadRotO = player.yHeadRotO;
            savedXRot = player.getXRot();
            savedXRotO = player.xRotO;
            modifiedRotations = true;

            player.setYRot(facingYaw);
            player.yRotO = facingYaw;
            player.yBodyRot = facingYaw;
            player.yBodyRotO = facingYaw;
            player.setYHeadRot(facingYaw);
            player.yHeadRotO = facingYaw;
            player.setXRot(0.0F);
            player.xRotO = 0.0F;
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();
        if (modifiedRotations && player.getVehicle() instanceof WheelRunnerEntity) {
            player.setYRot(savedYRot);
            player.yRotO = savedYRotO;
            player.yBodyRot = savedYBodyRot;
            player.yBodyRotO = savedYBodyRotO;
            player.setYHeadRot(savedYHeadRot);
            player.yHeadRotO = savedYHeadRotO;
            player.setXRot(savedXRot);
            player.xRotO = savedXRotO;
            modifiedRotations = false;
        }
    }

    @SubscribeEvent
    public static void onCalculatePlayerTurn(CalculatePlayerTurnEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getVehicle() instanceof WheelRunnerEntity) {
            if (mc.options.getCameraType().isFirstPerson()) {
                event.setMouseSensitivity(0.0D);
            }
        }
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getVehicle() instanceof WheelRunnerEntity runner) {
            if (mc.options.getCameraType().isFirstPerson()) {
                float targetYaw = runner.getDirection().toYRot();
                event.setYaw(targetYaw);
                event.setPitch(0.0F);
                event.setRoll(0.0F);
            }
        }
    }
}
