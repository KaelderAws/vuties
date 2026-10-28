package com.vibe.vuties.client;

import com.vibe.vuties.Vuties;
import com.vibe.vuties.item.ShockBootsItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

public class ShockBootsGeoModel extends GeoModel<ShockBootsItem> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(Vuties.MODID, "geo/shock_boots.geo.json");
    private static final ResourceLocation ANIMATIONS = ResourceLocation.fromNamespaceAndPath(Vuties.MODID, "animations/shock_boots.animation.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Vuties.MODID, "textures/models/armor/shock_boots.png");

    @Override
    public ResourceLocation getModelResource(ShockBootsItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getModelResource(ShockBootsItem animatable, GeoRenderer<ShockBootsItem> renderer) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(ShockBootsItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getTextureResource(ShockBootsItem animatable, GeoRenderer<ShockBootsItem> renderer) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(ShockBootsItem animatable) {
        return ANIMATIONS;
    }
}
