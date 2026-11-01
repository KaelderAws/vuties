package com.vibe.vuties.client;

import com.vibe.vuties.Vuties;
import com.vibe.vuties.item.GauntletItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

public class GauntletGeoModel extends GeoModel<GauntletItem> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(Vuties.MODID, "geo/gauntlet.geo.json");
    private static final ResourceLocation ANIMATIONS = ResourceLocation.fromNamespaceAndPath(Vuties.MODID, "animations/gauntlet.animation.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Vuties.MODID, "textures/models/armor/gauntlet.png");

    @Override
    public ResourceLocation getModelResource(GauntletItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getModelResource(GauntletItem animatable, GeoRenderer<GauntletItem> renderer) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(GauntletItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getTextureResource(GauntletItem animatable, GeoRenderer<GauntletItem> renderer) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(GauntletItem animatable) {
        return ANIMATIONS;
    }
}
