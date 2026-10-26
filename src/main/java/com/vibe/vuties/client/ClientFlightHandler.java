package com.vibe.vuties.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.vibe.vuties.Vuties;
import com.vibe.vuties.item.AceHelmetItem;
import com.vibe.vuties.item.ExoLegsItem;
import com.vibe.vuties.logic.PhaseShiftManager;
import com.vibe.vuties.network.PhaseShiftPayload;
import com.vibe.vuties.item.RotorpackItem;
import com.vibe.vuties.network.HelmetTogglePayload;
import com.vibe.vuties.network.NightVisionTogglePayload;
import net.minecraft.network.chat.Component;
import com.vibe.vuties.network.RotorpackInputPayload;
import com.vibe.vuties.network.RotorpackTogglePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.PlayerModel;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = Vuties.MODID, value = Dist.CLIENT)
public class ClientFlightHandler {

    // Плавная интерполяция наклона корпуса игрока (в режиме вертолёта)
    private static float currentPitchTilt = 0.0f;
    private static float targetPitchTilt = 0.0f;
    private static float currentRollTilt = 0.0f;
    private static float targetRollTilt = 0.0f;

    // Плавная анимация раскрытия/складывания крыльев
    private static float prevWingProgress = 0.0f;
    private static float currentWingProgress = 0.0f;

    // Инерция и анимация замедления роторов
    private static float rotorSpeed = 0.0f;
    private static float prevRotorAngle = 0.0f;
    private static float rotorAngle = 0.0f;

    // Защита от дребезга при нажатии Ctrl
    private static int elytraCooldown = 0;

    public static float getInterpolatedWingProgress(float partialTick) {
        return Mth.lerp(partialTick, prevWingProgress, currentWingProgress);
    }

    public static float getInterpolatedRotorAngle(float partialTick) {
        return Mth.lerp(partialTick, prevRotorAngle, rotorAngle);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || player.isSpectator()) return;

        // Переключение забрала / маски шлема
        while (ModKeyMappings.TOGGLE_HELMET.consumeClick()) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
            if (head.getItem() instanceof AceHelmetItem) {
                AceHelmetItem.setHelmetOpen(head, !AceHelmetItem.isHelmetOpen(head));
                PacketDistributor.sendToServer(new HelmetTogglePayload());
            }
        }

        // Переключение ночного зрения шлема
        while (ModKeyMappings.TOGGLE_NIGHT_VISION.consumeClick()) {
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
                    PacketDistributor.sendToServer(new NightVisionTogglePayload());
                }
            }
        }

        // Управление и обработка Фазового сдвига (Phase Shift / Phantom Dash)
        PhaseShiftManager.tickPlayer(player);

        if (PhaseShiftManager.isPhaseShiftActive(player)) {
            // УПРАВЛЕНИЕ НАПРАВЛЕНИЕМ В ПРОЦЕССЕ РЫВКА В РЕАЛЬНОМ ВРЕМЕНИ
            Vec3 steerDir = calculatePhaseSteerDirection(player);
            Vec3 motion = steerDir.scale(PhaseShiftManager.DASH_SPEED);

            player.setDeltaMovement(motion);
            // Прямое перемещение позиции сквозь любые блоки и стены
            player.setPos(player.getX() + motion.x, player.getY() + motion.y, player.getZ() + motion.z);
            player.noPhysics = true;
            player.resetFallDistance();

            // Отправляем пакет синхронизации направления на сервер
            PacketDistributor.sendToServer(new PhaseShiftPayload(1, motion.x, motion.y, motion.z));

            // Записываем узел 3D светового шлейфа Трона (без партиклов)
            PhaseShiftClientRenderer.recordRibbonNode(player);
        } else {
            // Активация рывка по клавише [R]
            while (ModKeyMappings.PHASE_SHIFT.consumeClick()) {
                ItemStack legs = player.getItemBySlot(EquipmentSlot.LEGS);
                if (legs.getItem() instanceof ExoLegsItem && PhaseShiftManager.canPhaseShift(player)) {
                    Vec3 steerDir = calculatePhaseSteerDirection(player);
                    Vec3 motion = steerDir.scale(PhaseShiftManager.DASH_SPEED);

                    PhaseShiftManager.executePhaseShift(player, motion);
                    player.setPos(player.getX() + motion.x, player.getY() + motion.y, player.getZ() + motion.z);
                    PacketDistributor.sendToServer(new PhaseShiftPayload(0, motion.x, motion.y, motion.z));
                    PhaseShiftClientRenderer.recordRibbonNode(player);
                }
            }
        }

        // Если на игроке НЕ надет наш ранец - немедленно выходим! Ванильные элитры не трогаем.
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!(chest.getItem() instanceof RotorpackItem)) {
            targetPitchTilt = 0.0f;
            currentPitchTilt = 0.0f;
            targetRollTilt = 0.0f;
            currentRollTilt = 0.0f;
            prevWingProgress = 0.0f;
            currentWingProgress = 0.0f;
            rotorSpeed = 0.0f;
            return;
        }

        if (elytraCooldown > 0) {
            elytraCooldown--;
        }

        // Переключение тумблеров (двигатель / зависание)
        while (ModKeyMappings.TOGGLE_ENGINE.consumeClick()) {
            RotorpackItem.setEngineEnabled(chest, !RotorpackItem.isEngineEnabled(chest));
            PacketDistributor.sendToServer(new RotorpackTogglePayload(0));
        }
        while (ModKeyMappings.TOGGLE_HOVER.consumeClick()) {
            if (RotorpackItem.hasHoverUpgrade(chest)) {
                RotorpackItem.setHoverMode(chest, !RotorpackItem.isHoverMode(chest));
            }
            PacketDistributor.sendToServer(new RotorpackTogglePayload(1));
        }

        boolean engine = RotorpackItem.isEngineEnabled(chest);
        boolean hover = RotorpackItem.hasHoverUpgrade(chest) && RotorpackItem.isHoverMode(chest);

        boolean jump = mc.options.keyJump.isDown();
        boolean sneak = mc.options.keyShift.isDown();
        boolean forward = mc.options.keyUp.isDown();
        boolean backward = mc.options.keyDown.isDown();
        boolean left = mc.options.keyLeft.isDown();
        boolean right = mc.options.keyRight.isDown();
        boolean sprint = mc.options.keySprint.isDown() || player.isSprinting();

        // -------------------------------------------------------------
        // ОБНОВЛЕНИЕ АНИМАЦИЙ (КРЫЛЬЯ И ИНЕРЦИЯ РОТОРОВ)
        // -------------------------------------------------------------
        prevWingProgress = currentWingProgress;
        prevRotorAngle = rotorAngle;

        boolean wingsDeploy = RotorpackItem.hasWingsUpgrade(chest) && engine && (player.isFallFlying() || sprint) && !player.onGround();
        float targetWing = wingsDeploy ? 1.0f : 0.0f;
        currentWingProgress = Mth.approach(currentWingProgress, targetWing, 0.09f);

        // Инерция роторов: плавный разгон и естественное затухание при отключении
        float targetSpeed;
        if (!engine) {
            targetSpeed = 0.0f;
        } else if (player.isFallFlying()) {
            targetSpeed = 1.30f;
        } else if (!player.onGround()) {
            targetSpeed = 0.85f;
        } else {
            targetSpeed = 0.30f;
        }

        float accelRate = (targetSpeed > rotorSpeed) ? 0.12f : 0.035f;
        rotorSpeed = Mth.approach(rotorSpeed, targetSpeed, accelRate);
        rotorAngle += rotorSpeed;

        if (!engine) {
            targetPitchTilt = 0.0f;
            currentPitchTilt = 0.0f;
            targetRollTilt = 0.0f;
            currentRollTilt = 0.0f;
            if (player.isFallFlying()) {
                player.stopFallFlying();
            }
            return;
        }

        // -------------------------------------------------------------
        // РЕЖИМ 1: КРЫЛЬЯ / ЭЛИТРЫ ПРИ ЗАЖАТИИ CTRL (ПОСТОЯННАЯ ТЯГА) - ТРЕБУЕТСЯ МОДУЛЬ КРЫЛЬЕВ
        // -------------------------------------------------------------
        if (RotorpackItem.hasWingsUpgrade(chest)) {
            if (sprint && !player.onGround() && !player.isFallFlying() && elytraCooldown == 0) {
                elytraCooldown = 6;
                player.startFallFlying();
                player.connection.send(new ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
            }

            if (player.isFallFlying()) {
                if (!sprint || player.onGround()) {
                    if (elytraCooldown == 0) {
                        elytraCooldown = 6;
                        player.stopFallFlying();
                        PacketDistributor.sendToServer(new RotorpackInputPayload(jump, sneak, forward, false));
                        return;
                    }
                }

                // Полноценное 3D-управление элитрами с постоянной тягой (25 м/с)
                Vec3 look = player.getLookAngle();
                double cruiseSpeed = 1.25;
                player.setDeltaMovement(look.scale(cruiseSpeed));
                player.resetFallDistance();

                // Тонкие инверсионные следы с законцовок крыльев (раз в 4 тика)
                if (player.tickCount % 4 == 0) {
                    spawnWingtipTrails(player);
                }

                PacketDistributor.sendToServer(new RotorpackInputPayload(jump, sneak, true, true));
                return;
            }
        } else {
            // Без модуля крыльев - немедленно выключаем элитры, если игра попыталась их включить
            if (player.isFallFlying()) {
                player.stopFallFlying();
            }
        }

        // -------------------------------------------------------------
        // РЕЖИМ 2: ВЕРТИКАЛЬНЫЙ ВЗЛЁТ / ЗАВИСАНИЕ (VTOL / HELICOPTER)
        // -------------------------------------------------------------
        Vec3 motion = player.getDeltaMovement();
        double mx = motion.x;
        double my = motion.y;
        double mz = motion.z;

        boolean activeFlying = false;

        float yawRad = (float) Math.toRadians(player.getYRot());
        double lookX = -Math.sin(yawRad);
        double lookZ = Math.cos(yawRad);
        double strafeX = lookZ;
        double strafeZ = -lookX;

        double moveX = 0;
        double moveZ = 0;
        if (forward) {
            moveX += lookX;
            moveZ += lookZ;
        }
        if (backward) {
            moveX -= lookX;
            moveZ -= lookZ;
        }
        if (left) {
            moveX += strafeX;
            moveZ += strafeZ;
        }
        if (right) {
            moveX -= strafeX;
            moveZ -= strafeZ;
        }
        double moveLen = Math.sqrt(moveX * moveX + moveZ * moveZ);
        if (moveLen > 0.001) {
            moveX /= moveLen;
            moveZ /= moveLen;
        }

        if (forward && !player.onGround()) {
            targetPitchTilt = 18.0f;
        } else if (backward && !player.onGround()) {
            targetPitchTilt = -14.0f;
        } else {
            targetPitchTilt = 0.0f;
        }

        if (left && !player.onGround()) {
            targetRollTilt = -10.0f;
        } else if (right && !player.onGround()) {
            targetRollTilt = 10.0f;
        } else {
            targetRollTilt = 0.0f;
        }

        currentPitchTilt = Mth.lerp(0.18f, currentPitchTilt, targetPitchTilt);
        currentRollTilt = Mth.lerp(0.18f, currentRollTilt, targetRollTilt);

        if (jump) {
            activeFlying = true;
            double lift = 0.14;
            if (moveLen > 0.001) {
                double cruiseSpeed = 0.065;
                mx += moveX * cruiseSpeed;
                mz += moveZ * cruiseSpeed;
            }
            my = Math.min(my + lift, 0.48);
            player.resetFallDistance();
        } else if (hover && !player.onGround()) {
            activeFlying = true;
            if (sneak) {
                // Быстрое и отзывчивое опускание на Shift в режиме Hover
                my = -0.55;
            } else {
                my = Math.sin(player.tickCount * 0.2) * 0.01;
            }

            if (moveLen > 0.001) {
                double speed = 0.065;
                mx += moveX * speed;
                mz += moveZ * speed;
            } else {
                mx *= 0.86;
                mz *= 0.86;
            }
            player.resetFallDistance();
        } else if (!player.onGround()) {
            if (my < -0.22) {
                my = -0.22;
                activeFlying = true;
            }
            if (moveLen > 0.001) {
                double speed = 0.045;
                mx += moveX * speed;
                mz += moveZ * speed;
            }
            player.resetFallDistance();
        }

        double horizSpeed = Math.sqrt(mx * mx + mz * mz);
        double maxSpeed = 0.55;
        if (horizSpeed > maxSpeed) {
            mx = (mx / horizSpeed) * maxSpeed;
            mz = (mz / horizSpeed) * maxSpeed;
        }

        player.setDeltaMovement(mx, my, mz);

        // Партиклы от пропеллеров: раз в 8 тиков
        if (activeFlying && player.tickCount % 8 == 0) {
            spawnDownwashParticles(player);
        }

        if (jump || sneak || moveLen > 0.001) {
            PacketDistributor.sendToServer(new RotorpackInputPayload(jump, sneak, moveLen > 0.001, sprint));
        }
    }

    private static void spawnDownwashParticles(LocalPlayer player) {
        float rad = (float) Math.toRadians(player.getYRot());
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);

        double leftX = player.getX() + (-0.56 * cos - 0.22 * sin);
        double leftZ = player.getZ() + (-0.56 * sin + 0.22 * cos);
        double rightX = player.getX() + (0.56 * cos - 0.22 * sin);
        double rightZ = player.getZ() + (0.56 * sin + 0.22 * cos);

        player.level().addParticle(ParticleTypes.CLOUD, leftX, player.getY() + 2.05, leftZ, 0, -0.15, 0);
        player.level().addParticle(ParticleTypes.CLOUD, rightX, player.getY() + 2.05, rightZ, 0, -0.15, 0);
    }

    private static void spawnWingtipTrails(LocalPlayer player) {
        float rad = (float) Math.toRadians(player.getYRot());
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);

        double leftX = player.getX() + (-1.05 * cos - 0.15 * sin);
        double leftZ = player.getZ() + (-1.05 * sin + 0.15 * cos);
        double rightX = player.getX() + (1.05 * cos - 0.15 * sin);
        double rightZ = player.getZ() + (1.05 * sin + 0.15 * cos);

        player.level().addParticle(ParticleTypes.CLOUD, leftX, player.getY() + 0.45, leftZ, 0, 0, 0);
        player.level().addParticle(ParticleTypes.CLOUD, rightX, player.getY() + 0.45, rightZ, 0, 0, 0);
    }

    @SubscribeEvent
    public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
        LivingEntity entity = event.getEntity();
        if (entity != null && entity.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof AceHelmetItem) {
            if (event.getRenderer().getModel() instanceof HumanoidModel<?> humanoidModel) {
                humanoidModel.hat.visible = false;
            }
        }
    }

    private static Field cloakField = null;
    private static boolean cloakFieldInitialized = false;

    private static void setCloakVisible(PlayerModel<?> model, boolean visible) {
        if (!cloakFieldInitialized) {
            cloakFieldInitialized = true;
            try {
                cloakField = PlayerModel.class.getDeclaredField("cloak");
                cloakField.setAccessible(true);
            } catch (NoSuchFieldException e) {
                for (Field f : PlayerModel.class.getDeclaredFields()) {
                    if (f.getType() == ModelPart.class && !Modifier.isPublic(f.getModifiers()) && !Modifier.isStatic(f.getModifiers())) {
                        f.setAccessible(true);
                        cloakField = f;
                        break;
                    }
                }
            }
        }
        if (cloakField != null) {
            try {
                ModelPart part = (ModelPart) cloakField.get(model);
                if (part != null) {
                    part.visible = visible;
                }
            } catch (Exception ignored) {
            }
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (player == null || player.isSpectator()) return;

        var model = event.getRenderer().getModel();

        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (head.getItem() instanceof AceHelmetItem) {
            model.hat.visible = false;
        }

        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.getItem() instanceof RotorpackItem) {
            // Скрываем внешний слой торса и рукавов, а также плащ
            model.jacket.visible = false;
            model.leftSleeve.visible = false;
            model.rightSleeve.visible = false;
            setCloakVisible(model, false);
        }

        ItemStack legs = player.getItemBySlot(EquipmentSlot.LEGS);
        if (legs.getItem() instanceof ExoLegsItem) {
            // Скрываем внешний слой штанин (Left Pant Leg и Right Pant Leg)
            model.leftPants.visible = false;
            model.rightPants.visible = false;
        }

        if (!(chest.getItem() instanceof RotorpackItem) || !RotorpackItem.isEngineEnabled(chest)) {
            return;
        }

        if (player.isFallFlying()) {
            return;
        }

        if (!player.onGround() && (Math.abs(currentPitchTilt) > 0.1f || Math.abs(currentRollTilt) > 0.1f)) {
            PoseStack poseStack = event.getPoseStack();
            float partialTick = event.getPartialTick();
            float bodyYaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);

            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
            poseStack.translate(0.0, 0.9, 0.0);
            poseStack.mulPose(Axis.XP.rotationDegrees(-currentPitchTilt));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-currentRollTilt));
            poseStack.translate(0.0, -0.9, 0.0);
            poseStack.mulPose(Axis.YP.rotationDegrees(-(180.0F - bodyYaw)));
        }
    }

    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        ItemStack chest = mc.player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.getItem() instanceof RotorpackItem) {
            SoundInstance sound = event.getSound();
            if (sound != null && sound.getLocation() != null) {
                String path = sound.getLocation().getPath();
                // Блокируем звуки элитр, воздушного потока и фейерверков при экипированном ранце
                if (path.contains("elytra") || path.contains("wind")) {
                    event.setSound(null);
                }
            }
        }
    }

    public static Vec3 calculatePhaseSteerDirection(LocalPlayer player) {
        Minecraft mc = Minecraft.getInstance();
        boolean up = mc.options.keyUp.isDown();
        boolean down = mc.options.keyDown.isDown();
        boolean left = mc.options.keyLeft.isDown();
        boolean right = mc.options.keyRight.isDown();
        boolean jump = mc.options.keyJump.isDown();
        boolean shift = mc.options.keyShift.isDown();

        float forward = (up ? 1.0f : 0.0f) - (down ? 1.0f : 0.0f);
        float strafe = (left ? 1.0f : 0.0f) - (right ? 1.0f : 0.0f);

        Vec3 look = player.getLookAngle();

        // 1. Если игрок не использует стрейфы (только W, только S, или без клавиш):
        // Направление на 100% идеально следует за прицелом мыши в полном 3D пространстве
        if (strafe == 0) {
            Vec3 dir = (forward < 0) ? look.scale(-1.0) : look;
            double vy = dir.y;
            if (jump) vy += 0.35;
            if (shift) vy -= 0.35;
            return new Vec3(dir.x, vy, dir.z).normalize();
        }

        // 2. Если зажаты клавиши стрейфа (A или D):
        float rad = (float) Math.toRadians(player.getYRot());
        float sin = Mth.sin(rad);
        float cos = Mth.cos(rad);

        // Вектор вперёд: (-sin, cos), вектор влево (Left): (+cos, +sin)
        float dx = -sin * forward + cos * strafe;
        float dz = cos * forward + sin * strafe;
        Vec3 horiz = new Vec3(dx, 0, dz).normalize();

        double vy = 0.0;
        if (forward > 0) {
            vy = look.y;
        } else if (forward < 0) {
            vy = -look.y;
        }

        if (jump) vy += 0.4;
        if (shift) vy -= 0.4;

        return new Vec3(horiz.x, vy, horiz.z).normalize();
    }
}