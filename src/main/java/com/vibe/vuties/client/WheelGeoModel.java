package com.vibe.vuties.client;

import com.vibe.vuties.block.entity.WheelBlockEntity;
import net.minecraft.util.Mth;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;

public class WheelGeoModel extends SimpleGeoModel<WheelBlockEntity> {
    public WheelGeoModel() {
        super("wheel", "textures/block/wheel.png");
    }

    @Override
    public void setCustomAnimations(WheelBlockEntity animatable, long instanceId, AnimationState<WheelBlockEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        GeoBone wheel = getAnimationProcessor().getBone("wheel");
        if (wheel != null) {
            float partialTick = animationState.getPartialTick();
            float angle = Mth.rotLerp(partialTick, animatable.prevRotationAngle, animatable.rotationAngle);
            wheel.setRotX((float) Math.toRadians(-angle));
        }
    }
}
