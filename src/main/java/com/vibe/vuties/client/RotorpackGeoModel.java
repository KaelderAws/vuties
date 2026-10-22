package com.vibe.vuties.client;

import com.vibe.vuties.Vuties;
import com.vibe.vuties.item.RotorpackItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

public class RotorpackGeoModel extends GeoModel<RotorpackItem> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(Vuties.MODID, "geo/rotorpack.geo.json");
    private static final ResourceLocation ANIMATIONS = ResourceLocation.fromNamespaceAndPath(Vuties.MODID, "animations/rotorpack.animation.json");

    @Override
    public ResourceLocation getModelResource(RotorpackItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getModelResource(RotorpackItem animatable, GeoRenderer<RotorpackItem> renderer) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(RotorpackItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(Vuties.MODID,
            "textures/models/armor/rotorpack_mk" + animatable.getTier().getLevel() + "_layer_1.png");
    }

    @Override
    public ResourceLocation getTextureResource(RotorpackItem animatable, GeoRenderer<RotorpackItem> renderer) {
        return getTextureResource(animatable);
    }

    @Override
    public ResourceLocation getAnimationResource(RotorpackItem animatable) {
        return ANIMATIONS;
    }
}
