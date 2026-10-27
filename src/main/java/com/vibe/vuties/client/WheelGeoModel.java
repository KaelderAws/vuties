package com.vibe.vuties.client;

import com.vibe.vuties.Vuties;
import com.vibe.vuties.block.entity.WheelBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class WheelGeoModel extends GeoModel<WheelBlockEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(Vuties.MODID, "geo/wheel.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Vuties.MODID, "textures/block/wheel.png");
    private static final ResourceLocation ANIMATIONS = ResourceLocation.fromNamespaceAndPath(Vuties.MODID, "animations/wheel.animation.json");

    @Override
    public ResourceLocation getModelResource(WheelBlockEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getModelResource(WheelBlockEntity animatable, software.bernie.geckolib.renderer.GeoRenderer<WheelBlockEntity> renderer) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(WheelBlockEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getTextureResource(WheelBlockEntity animatable, software.bernie.geckolib.renderer.GeoRenderer<WheelBlockEntity> renderer) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(WheelBlockEntity animatable) {
        return ANIMATIONS;
    }

    @Override
    public void setCustomAnimations(WheelBlockEntity animatable, long instanceId, software.bernie.geckolib.animation.AnimationState<WheelBlockEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        software.bernie.geckolib.cache.object.GeoBone wheel = getAnimationProcessor().getBone("wheel");
        if (wheel != null) {
            float partialTick = animationState.getPartialTick();
            float angle = net.minecraft.util.Mth.rotLerp(partialTick, animatable.prevRotationAngle, animatable.rotationAngle);
            wheel.setRotX((float) Math.toRadians(-angle));
        }
    }
}
