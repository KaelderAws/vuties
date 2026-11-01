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

        if (mainHand.getItem() instanceof GauntletItem) {
            ModelPart armPart = isRightMain ? this.getParentModel().rightArm : this.getParentModel().leftArm;
            ModelPart sleevePart = isRightMain ? this.getParentModel().rightSleeve : this.getParentModel().leftSleeve;

            boolean isDigging = ClientDrillHandler.isDigging() && GauntletItem.isDrillActive(mainHand);
            if (isDigging) {
                // Restore skipDraw and aim arm forward directly at target
                armPart.skipDraw = false;
                sleevePart.skipDraw = false;

                float pitch = player.getXRot();
                float bodyYaw = player.yBodyRot;
                float headYaw = player.getYHeadRot();

                float aimX = (float) Math.toRadians(pitch) - 1.5707963f;
                float aimY = (float) Math.toRadians(headYaw - bodyYaw);

                armPart.xRot = aimX;
                armPart.yRot = aimY;
                armPart.zRot = 0.0f;
                sleevePart.copyFrom(armPart);

                // Render player's arm and sleeve at the forward aiming stance
                VertexConsumer skinBuffer = buffer.getBuffer(RenderType.entityCutoutNoCull(player.getSkin().texture()));
                armPart.render(poseStack, skinBuffer, packedLight, OverlayTexture.NO_OVERLAY);
                sleevePart.render(poseStack, skinBuffer, packedLight, OverlayTexture.NO_OVERLAY);
            }

            // Render gauntlet and spinning drill on the arm
            renderGauntletOnArm(poseStack, buffer, packedLight, partialTick, mainHand, armPart, !isRightMain);
        }

        if (offHand.getItem() instanceof GauntletItem) {
            ModelPart armPart = isRightMain ? this.getParentModel().leftArm : this.getParentModel().rightArm;
            renderGauntletOnArm(poseStack, buffer, packedLight, partialTick, offHand, armPart, isRightMain);
        }
    }

    private void renderGauntletOnArm(PoseStack poseStack, MultiBufferSource buffer, int packedLight, float partialTick, ItemStack stack, ModelPart armPart, boolean isLeftArm) {
        poseStack.pushPose();
        armPart.translateAndRotate(poseStack);

        float offsetX = isLeftArm ? (1.0f / 16.0f) : (-1.0f / 16.0f);
        poseStack.translate(offsetX, 7.8f / 16.0f, 0.0f);
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0f));

        if (isLeftArm) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(90.0f));
        } else {
            poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0f));
        }

        poseStack.scale(1.05f, 1.05f, 1.05f);

        gauntletRenderer.renderWornOnArm(stack, poseStack, buffer, packedLight, partialTick, isLeftArm);

        poseStack.popPose();
    }
}
