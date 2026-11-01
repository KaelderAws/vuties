package com.vibe.vuties.client;

import com.vibe.vuties.Vuties;
import com.vibe.vuties.item.GauntletItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

@EventBusSubscriber(modid = Vuties.MODID, value = Dist.CLIENT)
public class ClientDrillHandler {

    public static final float MAX_DRILL_SPEED = 48.0f; // Maximum degrees per tick
    public static final float ACCELERATION = 3.6f;     // Speed gain per tick while digging/spinning
    public static final float DECELERATION = 1.4f;     // Speed loss per tick when stopped

    private static float drillSpeed = 0.0f;
    private static float prevDrillAngle = 0.0f;
    private static drillAngleHolder drillAngle = new drillAngleHolder();
    private static boolean currentlyDigging = false;

    private static float prevThrustProgress = 0.0f;
    private static float thrustProgress = 0.0f;

    private static class drillAngleHolder {
        float angle = 0.0f;
    }

    public static boolean isDigging() {
        return currentlyDigging;
    }

    public static float getInterpolatedThrust(float partialTick) {
        return Mth.lerp(partialTick, prevThrustProgress, thrustProgress);
    }

    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        ItemStack held = player.getMainHandItem();
        if (held.getItem() instanceof GauntletItem && GauntletItem.isDrillActive(held)) {
            if (event.isAttack()) {
                // Prevent arm swinging so gauntlet remains completely static
                event.setSwingHand(false);

                // When clicking in air (MISS), cancel event completely to skip missTime=10 and attack cooldown reset
                if (mc.hitResult == null || mc.hitResult.getType() == HitResult.Type.MISS) {
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || player.isSpectator()) return;

        // 1. Check Radial Menu opening on Alt key hold
        if (ModKeyMappings.GAUNTLET_MENU.isDown() && mc.screen == null) {
            ItemStack mainHand = player.getMainHandItem();
            ItemStack offHand = player.getOffhandItem();
            if (mainHand.getItem() instanceof GauntletItem) {
                mc.setScreen(new GauntletRadialScreen(InteractionHand.MAIN_HAND, mainHand));
            } else if (offHand.getItem() instanceof GauntletItem) {
                mc.setScreen(new GauntletRadialScreen(InteractionHand.OFF_HAND, offHand));
            }
        }

        // 2. Check if drill is held in main hand and is active
        ItemStack held = player.getMainHandItem();
        boolean hasActiveDrill = held.getItem() instanceof GauntletItem && GauntletItem.isDrillActive(held);

        currentlyDigging = false;
        if (hasActiveDrill && mc.level != null) {
            boolean attackDown = mc.options.keyAttack.isDown();
            boolean destroying = mc.gameMode != null && mc.gameMode.isDestroying();
            // Spin smoothly upon holding LMB in air or while mining blocks
            currentlyDigging = attackDown || destroying;
        }

        // 3. Thrust animation progress (interpolated extension of arm)
        prevThrustProgress = thrustProgress;
        if (hasActiveDrill && currentlyDigging) {
            thrustProgress = Math.min(1.0f, thrustProgress + 0.25f);
        } else {
            thrustProgress = Math.max(0.0f, thrustProgress - 0.20f);
        }

        // 4. Prevent arm swing while using the drill
        if (hasActiveDrill) {
            player.swinging = false;
            player.swingTime = 0;
            player.attackAnim = 0.0f;
            player.oAttackAnim = 0.0f;
        } else {
            thrustProgress = 0.0f;
            prevThrustProgress = 0.0f;
        }

        // 5. Accelerate or Decelerate rotation speed
        if (currentlyDigging) {
            drillSpeed = Math.min(MAX_DRILL_SPEED, drillSpeed + ACCELERATION);
        } else {
            drillSpeed = Math.max(0.0f, drillSpeed - DECELERATION);
        }

        // 6. Update angles
        prevDrillAngle = drillAngle.angle;
        drillAngle.angle += drillSpeed;
        if (drillAngle.angle > 360000.0f) {
            drillAngle.angle -= 360000.0f;
            prevDrillAngle -= 360000.0f;
        }
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player != null && event.getHand() == InteractionHand.MAIN_HAND) {
            ItemStack held = player.getMainHandItem();
            if (held.getItem() instanceof GauntletItem && GauntletItem.isDrillActive(held)) {
                // Suppress swing progress completely in first-person so the drill stays static
                player.swinging = false;
                player.swingTime = 0;
                player.attackAnim = 0.0f;
                player.oAttackAnim = 0.0f;
            }
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        ItemStack held = player.getMainHandItem();
        boolean hasActiveDrill = held.getItem() instanceof GauntletItem && GauntletItem.isDrillActive(held);
        if (hasActiveDrill && currentlyDigging) {
            PlayerModel<AbstractClientPlayer> model = event.getRenderer().getModel();
            boolean isRightMain = player.getMainArm() == HumanoidArm.RIGHT;
            if (isRightMain) {
                model.rightArm.skipDraw = true;
                model.rightSleeve.skipDraw = true;
            } else {
                model.leftArm.skipDraw = true;
                model.leftSleeve.skipDraw = true;
            }
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        PlayerModel<AbstractClientPlayer> model = event.getRenderer().getModel();
        model.rightArm.skipDraw = false;
        model.rightSleeve.skipDraw = false;
        model.leftArm.skipDraw = false;
        model.leftSleeve.skipDraw = false;
    }

    public static float getInterpolatedAngle(float partialTick) {
        return Mth.lerp(partialTick, prevDrillAngle, drillAngle.angle);
    }

    public static float getDrillSpeed() {
        return drillSpeed;
    }

    public static float getSpinRatio() {
        return drillSpeed / MAX_DRILL_SPEED;
    }
}
