package com.vibe.vuties.client;

import com.vibe.vuties.Vuties;
import com.vibe.vuties.item.ExoLegsItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

public class ExoLegsGeoModel extends GeoModel<ExoLegsItem> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(Vuties.MODID, "geo/exo_legs.geo.json");
    private static final ResourceLocation ANIMATIONS = ResourceLocation.fromNamespaceAndPath(Vuties.MODID, "animations/exo_legs.animation.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Vuties.MODID, "textures/models/armor/exo_legs.png");

    @Override
    public ResourceLocation getModelResource(ExoLegsItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getModelResource(ExoLegsItem animatable, GeoRenderer<ExoLegsItem> renderer) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(ExoLegsItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getTextureResource(ExoLegsItem animatable, GeoRenderer<ExoLegsItem> renderer) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(ExoLegsItem animatable) {
        return ANIMATIONS;
    }
}
