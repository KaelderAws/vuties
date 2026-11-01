package com.vibe.vuties.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.vibe.vuties.item.GauntletItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class GauntletItemRenderer extends GeoItemRenderer<GauntletItem> {

    private final DrillGeoModel drillModel = new DrillGeoModel();

    public GauntletItemRenderer() {
        super(new GauntletGeoModel());
    }

    @Override
    public RenderType getRenderType(GauntletItem animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutoutNoCull(texture);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext transformType, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        // Suppress generic handheld item rendering in third person!
        // GauntletPlayerLayer renders the glove directly worn on the player's hand/wrist.
        if (transformType == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND ||
            transformType == ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
            return;
        }
        super.renderByItem(stack, transformType, poseStack, bufferSource, packedLight, packedOverlay);
    }

    @Override
    public void preRender(PoseStack poseStack, GauntletItem animatable, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);

        ItemStack stack = getCurrentItemStack();
        boolean drillActive = stack != null && GauntletItem.isDrillActive(stack);
        float thrust = drillActive ? ClientDrillHandler.getInterpolatedThrust(partialTick) : 0.0f;

        if (this.renderPerspective == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
            // When digging/holding LMB, smoothly thrust arm and drill forward towards target
            float x = Mth.lerp(thrust, 0.24f, 0.12f);
            float y = Mth.lerp(thrust, -0.22f, -0.16f);
            float z = Mth.lerp(thrust, -0.32f, -0.55f);

            float yRot = Mth.lerp(thrust, 155.0f, 170.0f);
            float xRot = Mth.lerp(thrust, -20.0f, -10.0f);
            float zRot = Mth.lerp(thrust, 18.0f, 5.0f);

            poseStack.translate(x, y, z);
            poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
            poseStack.mulPose(Axis.XP.rotationDegrees(xRot));
            poseStack.mulPose(Axis.ZP.rotationDegrees(zRot));
            poseStack.scale(1.15f, 1.15f, 1.15f);
        } else if (this.renderPerspective == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
            float x = Mth.lerp(thrust, -0.24f, -0.12f);
            float y = Mth.lerp(thrust, -0.22f, -0.16f);
            float z = Mth.lerp(thrust, -0.32f, -0.55f);

            float yRot = Mth.lerp(thrust, 205.0f, 190.0f);
            float xRot = Mth.lerp(thrust, -20.0f, -10.0f);
            float zRot = Mth.lerp(thrust, -18.0f, -5.0f);

            poseStack.translate(x, y, z);
            poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
            poseStack.mulPose(Axis.XP.rotationDegrees(xRot));
            poseStack.mulPose(Axis.ZP.rotationDegrees(zRot));
            poseStack.scale(1.15f, 1.15f, 1.15f);
        } else if (this.renderPerspective == ItemDisplayContext.GUI) {
            // Isometric view in inventory slot
            poseStack.translate(0.0f, -0.04f, 0.0f);
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0f));
            poseStack.mulPose(Axis.YP.rotationDegrees(135.0f));
            poseStack.scale(0.80f, 0.80f, 0.80f);
        } else if (this.renderPerspective == ItemDisplayContext.GROUND) {
            poseStack.translate(0.0f, 0.1f, 0.0f);
            poseStack.scale(0.70f, 0.70f, 0.70f);
        } else if (this.renderPerspective == ItemDisplayContext.FIXED) {
            poseStack.scale(0.80f, 0.80f, 0.80f);
        }
    }

    @Override
    public void postRender(PoseStack poseStack, GauntletItem animatable, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        super.postRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
        // Render separate drill model attached to the gauntlet emitter in first-person
        if (!isReRender) {
            renderDrillIfActive(poseStack, animatable, bufferSource, partialTick, packedLight, packedOverlay);
        }
    }

    private void renderDrillIfActive(PoseStack poseStack, GauntletItem animatable, MultiBufferSource bufferSource, float partialTick, int packedLight, int packedOverlay) {
        ItemStack stack = getCurrentItemStack();
        if (stack != null && GauntletItem.isDrillActive(stack)) {
            BakedGeoModel bakedDrill = drillModel.getBakedModel(drillModel.getModelResource(animatable, this));
            if (bakedDrill != null) {
                float angle = ClientDrillHandler.getInterpolatedAngle(partialTick);
                bakedDrill.getBone("drill").ifPresent(b -> b.setRotZ((float) Math.toRadians(angle)));

                RenderType drillRenderType = RenderType.entityCutoutNoCull(drillModel.getTextureResource(animatable, this));
                VertexConsumer drillBuffer = bufferSource.getBuffer(drillRenderType);

                actuallyRender(poseStack, animatable, bakedDrill, drillRenderType, bufferSource, drillBuffer, true, partialTick, packedLight, packedOverlay, 0xFFFFFFFF);
            }
        }
    }

    /**
     * Dedicated render routine used by GauntletPlayerLayer to render the glove
     * directly sleeved onto the player's hand and wrist in third person.
     */
    public void renderWornOnArm(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, float partialTick, boolean isLeftArm) {
        this.animatable = (GauntletItem) stack.getItem();
        this.currentItemStack = stack;
        this.renderPerspective = ItemDisplayContext.NONE;

        BakedGeoModel bakedModel = getGeoModel().getBakedModel(getGeoModel().getModelResource(this.animatable));
        RenderType renderType = getRenderType(this.animatable, getTextureLocation(this.animatable), bufferSource, partialTick);
        VertexConsumer buffer = bufferSource.getBuffer(renderType);

        actuallyRender(poseStack, this.animatable, bakedModel, renderType, bufferSource, buffer, false, partialTick, packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);

        // Render separate drill model attached to the gauntlet in third-person!
        renderDrillIfActive(poseStack, this.animatable, bufferSource, partialTick, packedLight, OverlayTexture.NO_OVERLAY);
    }
}
