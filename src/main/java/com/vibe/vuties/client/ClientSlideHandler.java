package com.vibe.vuties.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import com.vibe.vuties.Vuties;
import com.vibe.vuties.item.RotorpackItem;
import com.vibe.vuties.logic.PhaseShiftManager;
import com.vibe.vuties.logic.SlideManager;
import com.vibe.vuties.network.SlidePayload;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.WalkAnimationState;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix4f;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@EventBusSubscriber(modid = Vuties.MODID, value = Dist.CLIENT)
public class ClientSlideHandler {

    private static final ResourceLocation SLIDE_STEP_ID = ResourceLocation.fromNamespaceAndPath(Vuties.MODID, "slide_step_height");
    private static final AttributeModifier SLIDE_STEP_MODIFIER = new AttributeModifier(SLIDE_STEP_ID, 0.55, AttributeModifier.Operation.ADD_VALUE);

    // Custom RenderType with COLOR_WRITE (no depth buffer writing = zero z-fighting/flicker) and NO_CULL (visible from 360 degrees)
    private static final RenderType SLIDE_TRAIL_RENDER_TYPE = RenderType.create(
        "vuties_slide_trail",
        DefaultVertexFormat.POSITION_COLOR,
        VertexFormat.Mode.QUADS,
        1536,
        false,
        false,
        RenderType.CompositeState.builder()
            .setShaderState(RenderStateShard.RENDERTYPE_LIGHTNING_SHADER)
            .setWriteMaskState(RenderStateShard.COLOR_WRITE)
            .setCullState(RenderStateShard.NO_CULL)
            .setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
            .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
            .createCompositeState(false)
    );

    private static boolean wasSliding = false;
    private static boolean wasJumpDown = false;
    private static int airGraceTicks = 0;

    // Плавный наклон корпуса и крен в поворотах с интерполяцией
    private static float currentPitchTilt = 0.0f;
    private static float prevPitchTilt = 0.0f;
    private static float targetPitchTilt = 0.0f;
    private static float currentRollTilt = 0.0f;
    private static float prevRollTilt = 0.0f;
    private static float targetRollTilt = 0.0f;

    // Анимация скольжения ног (cadence)
    private static final float SLIDE_LIMB_STEP = 0.17f;
    private static float slideLimbPos = 0.0f;

    // Рефлексия WalkAnimationState для управления перебором ног
    private static Field walkPositionField = null;
    private static Field walkSpeedOldField = null;
    private static boolean walkFieldsInit = false;

    // 3D огненные следы от подошв пяток
    private static final List<SkidNode> skidNodes = new ArrayList<>();

    public static class SkidNode {
        public final Vec3 leftA;
        public final Vec3 leftB;
        public final Vec3 rightA;
        public final Vec3 rightB;
        public int age = 0;
        public final int maxAge = 16;

        public SkidNode(Vec3 leftA, Vec3 leftB, Vec3 rightA, Vec3 rightB) {
            this.leftA = leftA;
            this.leftB = leftB;
            this.rightA = rightA;
            this.rightB = rightB;
        }
    }

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
        } catch (Exception ignored) {}
    }

    @SubscribeEvent
    public static void onClientTickPre(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null && mc.player != null && SlideManager.isSliding(mc.player)) {
            stopSliding(mc.player);
            return;
        }
        if (mc.player != null && SlideManager.isSliding(mc.player)) {
            mc.player.bob = 0.0f;
            mc.player.oBob = 0.0f;
            mc.player.walkDist = mc.player.walkDistO;
            mc.player.moveDist = 0.0f; // Полностью предотвращает вызов playStepSound() внутри Entity.move()!
        }
    }

    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        SoundInstance sound = event.getOriginalSound();
        if (sound == null) return;
        String name = event.getName();
        if (name == null) return;

        if (isStepOrWalkingSound(name)) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && SlideManager.isSliding(mc.player)) {
                double dx = sound.getX() - mc.player.getX();
                double dy = sound.getY() - mc.player.getY();
                double dz = sound.getZ() - mc.player.getZ();
                if (dx * dx + dy * dy + dz * dz < 25.0) {
                    event.setSound(null);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayLevelSound(PlayLevelSoundEvent event) {
        Holder<SoundEvent> soundHolder = event.getSound();
        if (soundHolder == null) return;
        ResourceLocation loc = soundHolder.unwrapKey().map(k -> k.location()).orElse(null);
        if (loc == null) return;
        if (!isStepOrWalkingSound(loc.getPath())) return;

        if (event instanceof PlayLevelSoundEvent.AtEntity atEntity) {
            if (atEntity.getEntity() instanceof Player p && SlideManager.isSliding(p)) {
                event.setCanceled(true);
            }
        } else if (event instanceof PlayLevelSoundEvent.AtPosition atPos) {
            Vec3 pos = atPos.getPosition();
            Level level = event.getLevel();
            if (level != null) {
                for (Player p : level.players()) {
                    if (SlideManager.isSliding(p) && p.distanceToSqr(pos) < 25.0) {
                        event.setCanceled(true);
                        return;
                    }
                }
            }
        }
    }

    private static boolean isStepOrWalkingSound(String path) {
        if (path == null) return false;
        String lower = path.toLowerCase();
        return lower.contains(".step")
                || lower.endsWith("step")
                || lower.contains("step.")
                || lower.contains(".swim")
                || lower.contains(".splash")
                || lower.contains("amethyst_block.chime");
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && SlideManager.isSliding(mc.player)) {
            mc.player.bob = 0.0f;
            mc.player.oBob = 0.0f;
            mc.player.walkDist = mc.player.walkDistO;
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        // Обновление возраста 3D-следов скольжения
        Iterator<SkidNode> it = skidNodes.iterator();
        while (it.hasNext()) {
            SkidNode node = it.next();
            node.age++;
            if (node.age >= node.maxAge) {
                it.remove();
            }
        }

        if (player == null || player.isSpectator() || mc.screen != null) {
            stopSliding(player);
            return;
        }

        // Проверка наличия ботинок и модуля скольжения
        if (!SlideManager.canSlide(player)) {
            stopSliding(player);
            return;
        }

        // Несовместимые режимы (фазовый сдвиг или активный полет на ранце/элитрах в воздухе)
        if (PhaseShiftManager.isPhaseShiftActive(player) || player.isFallFlying()) {
            stopSliding(player);
            return;
        }

        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.getItem() instanceof RotorpackItem && RotorpackItem.isEngineEnabled(chest) && !player.onGround()) {
            stopSliding(player);
            return;
        }

        boolean forward = mc.options.keyUp.isDown();
        boolean backward = mc.options.keyDown.isDown();
        boolean left = mc.options.keyLeft.isDown();
        boolean right = mc.options.keyRight.isDown();
        boolean jump = mc.options.keyJump.isDown();
        boolean jumpJustPressed = jump && !wasJumpDown;
        wasJumpDown = jump;
        boolean sprintKey = mc.options.keySprint.isDown() || net.minecraft.client.gui.screens.Screen.hasControlDown();
        boolean isSprinting = player.isSprinting() || sprintKey;
        boolean moving = forward || backward || left || right;

        boolean nearSurface = SlideManager.isNearSurface(player);

        if (nearSurface) {
            airGraceTicks = 45; // Достаточно времени для высоких прыжков и свободного падения с обрывов
        } else if (airGraceTicks > 0) {
            airGraceTicks--;
        }

        boolean canSlideSurface = nearSurface || airGraceTicks > 0;
        // Для скольжения достаточно просто зажать Ctrl (или кнопку спринта), либо начать бег
        boolean shouldSlide = (isSprinting && (moving || sprintKey)) && canSlideSurface;

        if (shouldSlide) {
            if (!wasSliding) {
                wasSliding = true;
                SlideManager.setSliding(player, true);
                PacketDistributor.sendToServer(new SlidePayload(true));
                // Звук старта реактивного скольжения (один раз при включении)
                player.level().playLocalSound(
                    player.getX(), player.getY(), player.getZ(),
                    SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS,
                    0.28F, 1.35F, false
                );
            }

            // 1. Глиссирование по воде и лаве (без погружения)
            SlideManager.applyLiquidSkimming(player);

            // 2. Вектор движения и крейсерская скорость
            float fwd = (forward ? 1.0f : 0.0f) - (backward ? 1.0f : 0.0f);
            float str = (left ? 1.0f : 0.0f) - (right ? 1.0f : 0.0f);

            // Если зажат только Ctrl без клавиш WASD — автоматически скользим вперед по взгляду
            if (fwd == 0.0f && str == 0.0f && sprintKey) {
                fwd = 1.0f;
            }

            float yawRad = (float) Math.toRadians(player.getYRot());
            double lookX = -Math.sin(yawRad);
            double lookZ = Math.cos(yawRad);
            double strafeX = lookZ;
            double strafeZ = -lookX;

            double dirX = lookX * fwd + strafeX * str;
            double dirZ = lookZ * fwd + strafeZ * str;
            double len = Math.sqrt(dirX * dirX + dirZ * dirZ);
            if (len > 0.001) {
                dirX /= len;
                dirZ /= len;
            }

            Vec3 motion = player.getDeltaMovement();

            // Скорость скольжения: при зажатом Ctrl — SLIDE_CRUISE_SPEED (0.725), а при связке WASD+Ctrl — ускоренная SLIDE_BOOST_SPEED (0.98)
            double cruiseSpeed = moving ? SlideManager.SLIDE_BOOST_SPEED : SlideManager.SLIDE_CRUISE_SPEED;
            double targetMx = dirX * cruiseSpeed;
            double targetMz = dirZ * cruiseSpeed;
            double lerpRate = nearSurface ? 0.28 : 0.08;
            double newMx = Mth.lerp(lerpRate, motion.x, targetMx);
            double newMz = Mth.lerp(lerpRate, motion.z, targetMz);
            double newMy = motion.y;

            // Высота прыжка: при простом Ctrl — стандартная ванильная (0.42), а в связке с WASD — на 1 блок больше (0.585)
            if (jump && nearSurface) {
                if (moving) {
                    // WASD + Ctrl: прыжок на 1 блок выше (apex ~2.25 блока)
                    if (motion.y >= 0.38 && motion.y <= 0.46) {
                        newMy = 0.585;
                    } else if (jumpJustPressed && motion.y <= 0.08) {
                        newMy = 0.585;
                    }
                } else {
                    // Только Ctrl: стандартная высота прыжка
                    if (jumpJustPressed && motion.y <= 0.08) {
                        newMy = 0.42;
                    }
                }
            }

            player.setDeltaMovement(newMx, newMy, newMz);

            // Увеличение шага для заезда на 1 блок
            AttributeInstance stepAttr = player.getAttribute(Attributes.STEP_HEIGHT);
            if (stepAttr != null && !stepAttr.hasModifier(SLIDE_STEP_ID)) {
                stepAttr.addTransientModifier(SLIDE_STEP_MODIFIER);
            }

            player.resetFallDistance();

            // 3. Запись огненного следа (позиция начала текущего тика)
            recordSkidNode(player);

            // 4. Плавное движение ног и подавление тряски камеры
            slideLimbPos += SLIDE_LIMB_STEP;
            player.bob = 0.0f;
            player.oBob = 0.0f;
            player.walkDist = player.walkDistO;
            player.moveDist = 0.0f;

            targetPitchTilt = 13.0f;
            targetRollTilt = (str > 0) ? -8.0f : ((str < 0) ? 8.0f : 0.0f);
        } else {
            stopSliding(player);
        }

        prevPitchTilt = currentPitchTilt;
        prevRollTilt = currentRollTilt;
        currentPitchTilt = Mth.lerp(0.20f, currentPitchTilt, targetPitchTilt);
        currentRollTilt = Mth.lerp(0.20f, currentRollTilt, targetRollTilt);
    }

    private static void stopSliding(LocalPlayer player) {
        wasJumpDown = false;
        if (wasSliding) {
            wasSliding = false;
            airGraceTicks = 0;
            targetPitchTilt = 0.0f;
            targetRollTilt = 0.0f;
            if (player != null) {
                player.bob = 0.0f;
                player.oBob = 0.0f;
                AttributeInstance stepAttr = player.getAttribute(Attributes.STEP_HEIGHT);
                if (stepAttr != null && stepAttr.hasModifier(SLIDE_STEP_ID)) {
                    stepAttr.removeModifier(SLIDE_STEP_ID);
                }
                SlideManager.setSliding(player, false);
                PacketDistributor.sendToServer(new SlidePayload(false));
            }
        } else {
            targetPitchTilt = 0.0f;
            targetRollTilt = 0.0f;
            if (player != null) {
                AttributeInstance stepAttr = player.getAttribute(Attributes.STEP_HEIGHT);
                if (stepAttr != null && stepAttr.hasModifier(SLIDE_STEP_ID)) {
                    stepAttr.removeModifier(SLIDE_STEP_ID);
                }
            }
        }
    }

    private static SkidNode createSkidNode(Player player, float partialTick) {
        double curX = Mth.lerp(partialTick, player.xo, player.getX());
        double curY = Mth.lerp(partialTick, player.yo, player.getY());
        double curZ = Mth.lerp(partialTick, player.zo, player.getZ());
        float bodyYaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);

        float renderPitch = Mth.lerp(partialTick, prevPitchTilt, currentPitchTilt);
        float renderRoll = Mth.lerp(partialTick, prevRollTilt, currentRollTilt);

        // Интерполяция угла взмаха ног в точном соответствии с HumanoidModel
        float limbPos = (slideLimbPos - SLIDE_LIMB_STEP) + SLIDE_LIMB_STEP * partialTick;
        float limbSpeed = 0.26f;

        float rightLegPitch = Mth.cos(limbPos * 0.6662F) * 1.4F * limbSpeed;
        float leftLegPitch = Mth.cos(limbPos * 0.6662F + (float) Math.PI) * 1.4F * limbSpeed;

        Vec3 rightLocal = computeHeelLocal(0.11875, rightLegPitch, renderPitch, renderRoll);
        Vec3 leftLocal = computeHeelLocal(-0.11875, leftLegPitch, renderPitch, renderRoll);

        float yawRad = (float) Math.toRadians(bodyYaw);
        double cosYaw = Math.cos(yawRad);
        double sinYaw = Math.sin(yawRad);

        // Преобразование из локальной системы модели в глобальные мировые координаты
        Vec3 rightCenter = new Vec3(
            curX - rightLocal.x * cosYaw + rightLocal.z * sinYaw,
            curY + Math.max(0.04, rightLocal.y),
            curZ - rightLocal.x * sinYaw - rightLocal.z * cosYaw
        );

        Vec3 leftCenter = new Vec3(
            curX - leftLocal.x * cosYaw + leftLocal.z * sinYaw,
            curY + Math.max(0.04, leftLocal.y),
            curZ - leftLocal.x * sinYaw - leftLocal.z * cosYaw
        );

        double halfW = 0.045;
        double sideX = -cosYaw * halfW;
        double sideZ = -sinYaw * halfW;

        Vec3 leftA = leftCenter.add(-sideX, 0, -sideZ);
        Vec3 leftB = leftCenter.add(sideX, 0, sideZ);
        Vec3 rightA = rightCenter.add(-sideX, 0, -sideZ);
        Vec3 rightB = rightCenter.add(sideX, 0, sideZ);

        return new SkidNode(leftA, leftB, rightA, rightB);
    }

    private static Vec3 computeHeelLocal(double hipXe, float legPitch, float renderPitch, float renderRoll) {
        // Координаты модели HumanoidModel:
        // hipXe: +0.11875 для правой ноги, -0.11875 для левой ноги
        // Высота тазобедренного сустава: 0.75 блока.
        // Пятка находится на задней грани обуви (offset 0.125 блока).
        // Смещение вглубь ботинка
        double heelBack = 0.04;
        double legLen = 0.75;

        // При взмахе ноги пятка поднимается над землей (с небольшим смещением вглубь подошвы)
        double yFoot = legLen * (1.0 - Math.cos(legPitch)) + 0.04;
        // При махе вперед пятка смещается вперед, при махе назад — назад
        double zFoot = heelBack + legLen * Math.sin(legPitch);

        // Положение относительно центра поворота корпуса (waist Y = 0.9)
        double px = hipXe;
        double py = yFoot - 0.9;
        double pz = zFoot;

        // Крен в поворотах (Roll вокруг Z)
        if (Math.abs(renderRoll) > 0.01f) {
            double rollRad = Math.toRadians(-renderRoll);
            double cosRoll = Math.cos(rollRad);
            double sinRoll = Math.sin(rollRad);
            double rpx = px * cosRoll - py * sinRoll;
            double rpy = px * sinRoll + py * cosRoll;
            px = rpx;
            py = rpy;
        }

        // Наклон корпуса вперед (Pitch вокруг X)
        if (Math.abs(renderPitch) > 0.01f) {
            double pitchRad = Math.toRadians(-renderPitch);
            double cosP = Math.cos(pitchRad);
            double sinP = Math.sin(pitchRad);
            double fpy = py * cosP - pz * sinP;
            double fpz = py * sinP + pz * cosP;
            py = fpy;
            pz = fpz;
        }

        return new Vec3(px, py + 0.9, pz);
    }

    private static void recordSkidNode(Player player) {
        // partialTick = 0.0f сохраняет позицию xo, yo, zo (начало текущего тика)
        skidNodes.add(createSkidNode(player, 0.0f));
        if (skidNodes.size() > 18) {
            skidNodes.remove(0);
        }
    }

    @SubscribeEvent
    public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
        if (event.getEntity() instanceof Player player && SlideManager.isSliding(player)) {
            // Плавная и непрерывная анимация ног без скачков скорости (устраняет вибрацию)
            initWalkFields();
            if (walkPositionField != null) {
                try {
                    walkPositionField.setFloat(player.walkAnimation, slideLimbPos);
                } catch (Exception ignored) {}
            }
            player.walkAnimation.setSpeed(0.26f);
            if (walkSpeedOldField != null) {
                try {
                    walkSpeedOldField.setFloat(player.walkAnimation, 0.26f);
                } catch (Exception ignored) {}
            }
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (player == null || player.isSpectator()) return;

        if (!SlideManager.isSliding(player)) return;

        float partialTick = event.getPartialTick();
        float renderPitch = Mth.lerp(partialTick, prevPitchTilt, currentPitchTilt);
        float renderRoll = Mth.lerp(partialTick, prevRollTilt, currentRollTilt);

        // Плавный наклон корпуса вперед и крен с полной интерполяцией
        if (Math.abs(renderPitch) > 0.1f || Math.abs(renderRoll) > 0.1f) {
            PoseStack poseStack = event.getPoseStack();
            float bodyYaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);

            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
            poseStack.translate(0.0, 0.9, 0.0);
            poseStack.mulPose(Axis.XP.rotationDegrees(-renderPitch));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-renderRoll));
            poseStack.translate(0.0, -0.9, 0.0);
            poseStack.mulPose(Axis.YP.rotationDegrees(-(180.0F - bodyYaw)));
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (skidNodes.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        boolean sliding = player != null && SlideManager.isSliding(player);

        List<SkidNode> renderList = new ArrayList<>(skidNodes);

        // Динамическое добавление головы следа строго к текущей позиции пяток
        if (sliding && player != null) {
            float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
            SkidNode head = createSkidNode(player, partialTick);
            head.age = 0;
            renderList.add(head);
        }

        if (renderList.size() < 2) return;

        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();

        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);
        Matrix4f matrix = poseStack.last().pose();

        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer builder = bufferSource.getBuffer(SLIDE_TRAIL_RENDER_TYPE);

        for (int i = 0; i < renderList.size() - 1; i++) {
            SkidNode a = renderList.get(i);
            SkidNode b = renderList.get(i + 1);

            float alphaA = 1.0f - (float) a.age / a.maxAge;
            float alphaB = 1.0f - (float) b.age / b.maxAge;
            if (alphaA <= 0 && alphaB <= 0) continue;

            int coreAlphaA = Math.max(0, Math.min(255, (int) (alphaA * 180)));
            int coreAlphaB = Math.max(0, Math.min(255, (int) (alphaB * 180)));
            int outerAlphaA = Math.max(0, Math.min(255, (int) (alphaA * 120)));
            int outerAlphaB = Math.max(0, Math.min(255, (int) (alphaB * 120)));

            Vec3 upOffset = new Vec3(0, 0.035, 0);

            // --- 1. Левая огненная колея ---
            // Горизонтальная полоса (NO_CULL рендерит обе стороны без z-fighting)
            drawQuad(builder, matrix,
                    a.leftA, a.leftB, b.leftB, b.leftA,
                    255, 120, 20, outerAlphaA,
                    255, 120, 20, outerAlphaB
            );

            // Вертикальная 3D-полоса (объем со всех углов)
            Vec3 aLeftCenter = a.leftA.add(a.leftB).scale(0.5);
            Vec3 bLeftCenter = b.leftA.add(b.leftB).scale(0.5);
            drawQuad(builder, matrix,
                    aLeftCenter.add(upOffset), aLeftCenter.subtract(upOffset),
                    bLeftCenter.subtract(upOffset), bLeftCenter.add(upOffset),
                    255, 120, 20, outerAlphaA,
                    255, 120, 20, outerAlphaB
            );

            // Бело-желтый раскаленный центр левой колеи
            drawQuad(builder, matrix,
                    a.leftA.lerp(a.leftB, 0.25), a.leftA.lerp(a.leftB, 0.75),
                    b.leftA.lerp(b.leftB, 0.75), b.leftA.lerp(b.leftB, 0.25),
                    255, 230, 90, coreAlphaA,
                    255, 230, 90, coreAlphaB
            );

            // --- 2. Правая огненная колея ---
            // Горизонтальная полоса
            drawQuad(builder, matrix,
                    a.rightA, a.rightB, b.rightB, b.rightA,
                    255, 120, 20, outerAlphaA,
                    255, 120, 20, outerAlphaB
            );

            // Вертикальная 3D-полоса
            Vec3 aRightCenter = a.rightA.add(a.rightB).scale(0.5);
            Vec3 bRightCenter = b.rightA.add(b.rightB).scale(0.5);
            drawQuad(builder, matrix,
                    aRightCenter.add(upOffset), aRightCenter.subtract(upOffset),
                    bRightCenter.subtract(upOffset), bRightCenter.add(upOffset),
                    255, 120, 20, outerAlphaA,
                    255, 120, 20, outerAlphaB
            );

            // Бело-желтый раскаленный центр правой колеи
            drawQuad(builder, matrix,
                    a.rightA.lerp(a.rightB, 0.25), a.rightA.lerp(a.rightB, 0.75),
                    b.rightA.lerp(b.rightB, 0.75), b.rightA.lerp(b.rightB, 0.25),
                    255, 230, 90, coreAlphaA,
                    255, 230, 90, coreAlphaB
            );
        }

        bufferSource.endBatch(SLIDE_TRAIL_RENDER_TYPE);
        poseStack.popPose();
    }

    private static void drawQuad(VertexConsumer builder, Matrix4f matrix,
                                 Vec3 v1, Vec3 v2, Vec3 v3, Vec3 v4,
                                 int r1, int g1, int b1, int a1,
                                 int r2, int g2, int b2, int a2) {
        builder.addVertex(matrix, (float) v1.x, (float) v1.y, (float) v1.z).setColor(r1, g1, b1, a1);
        builder.addVertex(matrix, (float) v2.x, (float) v2.y, (float) v2.z).setColor(r1, g1, b1, a1);
        builder.addVertex(matrix, (float) v3.x, (float) v3.y, (float) v3.z).setColor(r2, g2, b2, a2);
        builder.addVertex(matrix, (float) v4.x, (float) v4.y, (float) v4.z).setColor(r2, g2, b2, a2);
    }
}
