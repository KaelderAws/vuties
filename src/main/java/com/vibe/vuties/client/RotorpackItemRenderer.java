package com.vibe.vuties.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.vibe.vuties.item.RotorpackItem;
import net.minecraft.client.renderer.MultiBufferSource;
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

        // Hide armor-specific humanoid parts (shoulders, head) for standalone item view
        getGeoModel().getBone("armorRightArm").ifPresent(b -> b.setHidden(true));
        getGeoModel().getBone("armorLeftArm").ifPresent(b -> b.setHidden(true));
        getGeoModel().getBone("armorHead").ifPresent(b -> b.setHidden(true));

        // Hide wings on Mk I and Mk II, display on Mk III
        boolean hasWings = animatable.getTier().hasWings();
        getGeoModel().getBone("left_wing").ifPresent(b -> b.setHidden(!hasWings));
        getGeoModel().getBone("right_wing").ifPresent(b -> b.setHidden(!hasWings));
    }
}
