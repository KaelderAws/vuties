package com.vibe.vuties.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.vibe.vuties.Vuties;
import com.vibe.vuties.logic.PhaseShiftManager;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@EventBusSubscriber(modid = Vuties.MODID, value = Dist.CLIENT)
public class PhaseShiftClientRenderer {

    private static boolean isCustomRendering = false;
    private static final List<RibbonNode> ribbonNodes = new ArrayList<>();

    public static class RibbonNode {
        public final Vec3 top;
        public final Vec3 bottom;
        public final Vec3 left;
        public final Vec3 right;
        public int age = 0;
        public final int maxAge = 22;

        public RibbonNode(Vec3 top, Vec3 bottom, Vec3 left, Vec3 right) {
            this.top = top;
            this.bottom = bottom;
            this.left = left;
            this.right = right;
        }
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (PhaseShiftManager.isPhaseShiftActive(event.getEntity())) {
            event.getEntity().noPhysics = true;
        }
    }

    public static void recordRibbonNode(Player player) {
        Vec3 pos = player.position();
        float yawRad = (float) Math.toRadians(player.getYRot());
        float sin = Mth.sin(yawRad);
        float cos = Mth.cos(yawRad);

        Vec3 leftOffset = new Vec3(-cos * 0.45, 0.35, -sin * 0.45);
        Vec3 rightOffset = new Vec3(cos * 0.45, 0.35, sin * 0.45);

        ribbonNodes.add(new RibbonNode(
            pos.add(0, 1.4, 0),
            pos.add(0, 0.1, 0),
            pos.add(leftOffset),
            pos.add(rightOffset)
        ));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Iterator<RibbonNode> it = ribbonNodes.iterator();
        while (it.hasNext()) {
            RibbonNode node = it.next();
            node.age++;
            if (node.age >= node.maxAge) {
                it.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (player == null || player.isSpectator()) return;

        if (PhaseShiftManager.isPhaseShiftActive(player)) {
            if (isCustomRendering) return;
            event.setCanceled(true);
            isCustomRendering = true;
            try {
                MultiBufferSource phaseBuffer = new PhaseBufferSource(event.getMultiBufferSource(), player);
                event.getRenderer().render(
                        (AbstractClientPlayer) player,
                        player.getYRot(),
                        event.getPartialTick(),
                        event.getPoseStack(),
                        phaseBuffer,
                        0xF000F0
                );
            } finally {
                isCustomRendering = false;
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (ribbonNodes.size() < 2) return;

        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();

        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);
        Matrix4f matrix = poseStack.last().pose();

        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer builder = bufferSource.getBuffer(RenderType.lightning());

        for (int i = 0; i < ribbonNodes.size() - 1; i++) {
            RibbonNode a = ribbonNodes.get(i);
            RibbonNode b = ribbonNodes.get(i + 1);

            float alphaA = 1.0f - (float) a.age / a.maxAge;
            float alphaB = 1.0f - (float) b.age / b.maxAge;
            if (alphaA <= 0 && alphaB <= 0) continue;

            int alphaAInt = Math.max(0, Math.min(255, (int) (alphaA * 210)));
            int alphaBInt = Math.max(0, Math.min(255, (int) (alphaB * 210)));

            // 1. Центральная неоновая световая стена Трона (Tron Light Wall)
            drawQuad(builder, matrix,
                    a.top, a.bottom, b.bottom, b.top,
                    0, 235, 255, alphaAInt,
                    0, 235, 255, alphaBInt
            );

            // Обратная сторона световой стены
            drawQuad(builder, matrix,
                    b.top, b.bottom, a.bottom, a.top,
                    0, 235, 255, alphaBInt,
                    0, 235, 255, alphaAInt
            );

            // 2. Левая световая полоса сопла (Left Thruster Trail)
            int purpA = Math.max(0, Math.min(255, (int) (alphaA * 160)));
            int purpB = Math.max(0, Math.min(255, (int) (alphaB * 160)));
            drawQuad(builder, matrix,
                    a.top, a.left, b.left, b.top,
                    200, 30, 255, purpA,
                    200, 30, 255, purpB
            );
            drawQuad(builder, matrix,
                    b.top, b.left, a.left, a.top,
                    200, 30, 255, purpB,
                    200, 30, 255, purpA
            );

            // 3. Правая световая полоса сопла (Right Thruster Trail)
            drawQuad(builder, matrix,
                    a.top, a.right, b.right, b.top,
                    200, 30, 255, purpA,
                    200, 30, 255, purpB
            );
            drawQuad(builder, matrix,
                    b.top, b.right, a.right, a.top,
                    200, 30, 255, purpB,
                    200, 30, 255, purpA
            );

            // 4. Яркие лазерные ребра по верхнему и нижнему краю (Laser Borders)
            int glowA = Math.max(0, Math.min(255, (int) (alphaA * 255)));
            int glowB = Math.max(0, Math.min(255, (int) (alphaB * 255)));

            Vec3 topUpA = a.top.add(0, 0.04, 0);
            Vec3 topUpB = b.top.add(0, 0.04, 0);
            drawQuad(builder, matrix,
                    topUpA, a.top, b.top, topUpB,
                    220, 255, 255, glowA,
                    220, 255, 255, glowB
            );
            drawQuad(builder, matrix,
                    topUpB, b.top, a.top, topUpA,
                    220, 255, 255, glowB,
                    220, 255, 255, glowA
            );
        }

        bufferSource.endBatch(RenderType.lightning());
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

    public static class PhaseBufferSource implements MultiBufferSource {
        private final MultiBufferSource delegate;
        private final Player player;

        public PhaseBufferSource(MultiBufferSource delegate, Player player) {
            this.delegate = delegate;
            this.player = player;
        }

        @Override
        public VertexConsumer getBuffer(RenderType renderType) {
            RenderType target = renderType;
            if (player instanceof AbstractClientPlayer clientPlayer) {
                String name = renderType.toString().toLowerCase();
                if (name.contains("cutout") || name.contains("solid") || name.contains("armor")) {
                    target = RenderType.entityTranslucent(clientPlayer.getSkin().texture());
                }
            }
            VertexConsumer baseConsumer = delegate.getBuffer(target);
            return new PhaseVertexConsumer(baseConsumer);
        }
    }

    public static class PhaseVertexConsumer implements VertexConsumer {
        private final VertexConsumer parent;

        public PhaseVertexConsumer(VertexConsumer parent) {
            this.parent = parent;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            parent.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int r, int g, int b, int a) {
            // Неоновый синевато-полупрозрачный оттенок фантома (40% прозрачности)
            int nr = (int) (r * 0.10f);
            int ng = Math.min(255, (int) (g * 0.70f + 55));
            int nb = 255;
            int na = (int) (a * 0.40f);
            parent.setColor(nr, ng, nb, na);
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            parent.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            parent.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            parent.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float normalX, float normalY, float normalZ) {
            parent.setNormal(normalX, normalY, normalZ);
            return this;
        }

        @Override
        public VertexConsumer setLight(int packedLight) {
            // Полная самосветящаяся неоновая эмиссия
            parent.setLight(0xF000F0);
            return this;
        }
    }
}
