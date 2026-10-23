package com.vibe.vuties.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.vibe.vuties.item.RotorpackItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class RotorpackItemRenderer extends GeoItemRenderer<RotorpackItem> {

    public RotorpackItemRenderer() {
        super(new RotorpackGeoModel());
    }

    @Override
    public void preRender(PoseStack poseStack, RotorpackItem animatable, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);

        // Center the rotorpack model so rotations and scaling in GUI/hand occur around its center
        poseStack.translate(0, -1.05f, -0.2f);

        // Наплечники ранца (броня на плечах) видимы как часть конструкции
        getGeoModel().getBone("armorRightArm").ifPresent(b -> b.setHidden(false));
        getGeoModel().getBone("armorLeftArm").ifPresent(b -> b.setHidden(false));

        ItemStack stack = getCurrentItemStack();
        boolean hasWings = stack != null && RotorpackItem.hasWingsUpgrade(stack);
        getGeoModel().getBone("left_wing").ifPresent(b -> b.setHidden(!hasWings));
        getGeoModel().getBone("right_wing").ifPresent(b -> b.setHidden(!hasWings));
    }
}
