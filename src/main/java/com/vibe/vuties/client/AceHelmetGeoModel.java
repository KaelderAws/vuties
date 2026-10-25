package com.vibe.vuties.client;

import com.vibe.vuties.Vuties;
import com.vibe.vuties.item.AceHelmetItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

public class AceHelmetGeoModel extends GeoModel<AceHelmetItem> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(Vuties.MODID, "geo/ace_helmet.geo.json");
    private static final ResourceLocation ANIMATIONS = ResourceLocation.fromNamespaceAndPath(Vuties.MODID, "animations/ace_helmet.animation.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Vuties.MODID, "textures/models/armor/ace_helmet.png");

    @Override
    public ResourceLocation getModelResource(AceHelmetItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getModelResource(AceHelmetItem animatable, GeoRenderer<AceHelmetItem> renderer) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(AceHelmetItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getTextureResource(AceHelmetItem animatable, GeoRenderer<AceHelmetItem> renderer) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(AceHelmetItem animatable) {
        return ANIMATIONS;
    }
}
