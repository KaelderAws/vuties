package com.vibe.vuties.client;

import com.vibe.vuties.Vuties;
import com.vibe.vuties.item.JetBootsItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

public class JetBootsGeoModel extends GeoModel<JetBootsItem> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(Vuties.MODID, "geo/jet_boots.geo.json");
    private static final ResourceLocation ANIMATIONS = ResourceLocation.fromNamespaceAndPath(Vuties.MODID, "animations/jet_boots.animation.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Vuties.MODID, "textures/models/armor/jet_boots.png");

    @Override
    public ResourceLocation getModelResource(JetBootsItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getModelResource(JetBootsItem animatable, GeoRenderer<JetBootsItem> renderer) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(JetBootsItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getTextureResource(JetBootsItem animatable, GeoRenderer<JetBootsItem> renderer) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(JetBootsItem animatable) {
        return ANIMATIONS;
    }
}
