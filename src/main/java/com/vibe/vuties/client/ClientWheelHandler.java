package com.vibe.vuties.client;

import com.vibe.vuties.Vuties;
import com.vibe.vuties.block.WheelBlock;
import com.vibe.vuties.block.entity.WheelBlockEntity;
import com.vibe.vuties.entity.WheelRunnerEntity;
import com.vibe.vuties.init.ModBlocks;
import com.vibe.vuties.network.WheelRunPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.WalkAnimationState;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CalculatePlayerTurnEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.lang.reflect.Field;

@EventBusSubscriber(modid = Vuties.MODID, value = Dist.CLIENT)
public class ClientWheelHandler {

    private static boolean wasRunning = false;
    private static boolean wasSprinting = false;

    private static Field walkPositionField = null;
    private static Field walkSpeedOldField = null;
    private static boolean walkFieldsInit = false;

    private static void initWalkFields() {
        if (walkFieldsInit) return;
        walkFieldsInit = true;
        try {
            walkPositionField = WalkAnimationState.class.getDeclaredField("position");
            walkPositionField.setAccessible(true);
        } catch (Exception e) {
            Field[] fields = WalkAnimationState.class.getDeclaredFields();
            for (Field f : fields) {
                if (f.getType() == float.class) {
                    f.setAccessible(true);
                    if (walkPositionField == null && f.getName().toLowerCase().contains("pos")) {
                        walkPositionField = f;
                    }
                }
            }
        }
        try {
            walkSpeedOldField = WalkAnimationState.class.getDeclaredField("speedOld");
            walkSpeedOldField.setAccessible(true);
        } catch (Exception e) {
            Field[] fields = WalkAnimationState.class.getDeclaredFields();
            for (Field f : fields) {
                if (f.getType() == float.class) {
                    f.setAccessible(true);
                    if (walkSpeedOldField == null && f.getName().toLowerCase().contains("old")) {
                        walkSpeedOldField = f;
                    }
                }
            }
        }
    }

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
    public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
        LivingEntity entity = event.getEntity();
        if (entity.getVehicle() instanceof WheelRunnerEntity runner) {
            // Stand rather than sit
            event.getRenderer().getModel().riding = false;

            BlockPos masterPos = runner.getMasterPos();
            if (masterPos != null && entity.level().getBlockEntity(masterPos) instanceof WheelBlockEntity wheelBE) {
                // Smooth leg animation synchronized with wheel rotation (eliminates jitter)
                initWalkFields();
                if (walkPositionField != null && walkSpeedOldField != null) {
                    try {
                        float limbSpeed = wheelBE.rotationSpeed * 0.055f;
                        walkPositionField.setFloat(entity.walkAnimation, wheelBE.riderLimbPos);
                        entity.walkAnimation.setSpeed(limbSpeed);
                        walkSpeedOldField.setFloat(entity.walkAnimation, limbSpeed);
                    } catch (Exception ignored) {}
                } else {
                    entity.walkAnimation.setSpeed(wheelBE.rotationSpeed * 0.055f);
                }
            }

            if (!(entity instanceof Player)) {
                Direction facing = runner.getDirection();
                if (masterPos != null && entity.level().getBlockState(masterPos).is(ModBlocks.WHEEL.get())) {
                    facing = entity.level().getBlockState(masterPos).getValue(WheelBlock.FACING);
                }
                float facingYaw = facing.toYRot();
                entity.setYRot(facingYaw);
                entity.yRotO = facingYaw;
                entity.yBodyRot = facingYaw;
                entity.yBodyRotO = facingYaw;
                entity.setYHeadRot(facingYaw);
                entity.yHeadRotO = facingYaw;
            }
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (player.getVehicle() instanceof WheelRunnerEntity runner) {
            // Stand rather than sit
            event.getRenderer().getModel().riding = false;

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
