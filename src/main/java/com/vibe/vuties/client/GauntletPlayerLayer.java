package com.vibe.vuties.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.vibe.vuties.item.GauntletItem;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;

public class GauntletPlayerLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private final GauntletItemRenderer gauntletRenderer = new GauntletItemRenderer();

    public GauntletPlayerLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.isInvisible()) return;

        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        boolean isRightMain = player.getMainArm() == HumanoidArm.RIGHT;

        ModelPart rightArmModel = this.getParentModel().rightArm;
        ModelPart leftArmModel = this.getParentModel().leftArm;
        ModelPart rightSleeveModel = this.getParentModel().rightSleeve;
        ModelPart leftSleeveModel = this.getParentModel().leftSleeve;

        boolean rDig = ClientDrillHandler.isRightArmDigging(player);
        boolean lDig = ClientDrillHandler.isLeftArmDigging(player);

        float aimX = ClientDrillHandler.getAimX(player);
        float aimY = ClientDrillHandler.getAimY(player, partialTick);

        // When right arm is digging, render the player's right arm and sleeve aimed forward
        if (rDig) {
            rightArmModel.skipDraw = false;
            rightSleeveModel.skipDraw = false;
            rightArmModel.xRot = aimX;
            rightArmModel.yRot = aimY;
            rightArmModel.zRot = 0.0f;
            rightSleeveModel.copyFrom(rightArmModel);

            VertexConsumer skinBuffer = buffer.getBuffer(RenderType.entityCutoutNoCull(player.getSkin().texture()));
            rightArmModel.render(poseStack, skinBuffer, packedLight, OverlayTexture.NO_OVERLAY);
            rightSleeveModel.render(poseStack, skinBuffer, packedLight, OverlayTexture.NO_OVERLAY);
        }

        // When left arm is digging, render the player's left arm and sleeve aimed forward
        if (lDig) {
            leftArmModel.skipDraw = false;
            leftSleeveModel.skipDraw = false;
            leftArmModel.xRot = aimX;
            leftArmModel.yRot = aimY;
            leftArmModel.zRot = 0.0f;
            leftSleeveModel.copyFrom(leftArmModel);

            VertexConsumer skinBuffer = buffer.getBuffer(RenderType.entityCutoutNoCull(player.getSkin().texture()));
            leftArmModel.render(poseStack, skinBuffer, packedLight, OverlayTexture.NO_OVERLAY);
            leftSleeveModel.render(poseStack, skinBuffer, packedLight, OverlayTexture.NO_OVERLAY);
        }

        // Render worn gauntlets
        if (mainHand.getItem() instanceof GauntletItem) {
            ModelPart armPart = isRightMain ? rightArmModel : leftArmModel;
            renderGauntletOnArm(poseStack, buffer, packedLight, partialTick, mainHand, armPart, !isRightMain);
        }

        if (offHand.getItem() instanceof GauntletItem) {
            ModelPart armPart = isRightMain ? leftArmModel : rightArmModel;
            renderGauntletOnArm(poseStack, buffer, packedLight, partialTick, offHand, armPart, isRightMain);
        }
    }

    private void renderGauntletOnArm(PoseStack poseStack, MultiBufferSource buffer, int packedLight, float partialTick, ItemStack stack, ModelPart armPart, boolean isLeftArm) {
        poseStack.pushPose();
        armPart.translateAndRotate(poseStack);

        float offsetX = isLeftArm ? (1.0f / 16.0f) : (-1.0f / 16.0f);
        poseStack.translate(offsetX, 7.8f / 16.0f, 0.0f);
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0f));

        poseStack.scale(1.05f, 1.05f, 1.05f);
        if (isLeftArm) {
            poseStack.last().pose().scale(-1.0f, 1.0f, 1.0f);
            poseStack.last().normal().scale(-1.0f, 1.0f, 1.0f);
        }

        gauntletRenderer.renderWornOnArm(stack, poseStack, buffer, packedLight, partialTick, isLeftArm);

        poseStack.popPose();
    }
}
