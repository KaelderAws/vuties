package com.vibe.vuties.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.vibe.vuties.item.RotorpackItem;
import net.minecraft.client.renderer.MultiBufferSource;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class RotorpackGeoRenderer extends GeoArmorRenderer<RotorpackItem> {
    public RotorpackGeoRenderer() {
        super(new RotorpackGeoModel());
    }

    @Override
    public void preRender(PoseStack poseStack, RotorpackItem animatable, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);

        boolean hasWings = animatable.getTier().hasWings();
        getGeoModel().getBone("left_wing").ifPresent(b -> b.setHidden(!hasWings));
        getGeoModel().getBone("right_wing").ifPresent(b -> b.setHidden(!hasWings));
    }
}
