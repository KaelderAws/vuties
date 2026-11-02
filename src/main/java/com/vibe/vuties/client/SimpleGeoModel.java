package com.vibe.vuties.client;

import com.vibe.vuties.Vuties;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

@SuppressWarnings("removal")
public class SimpleGeoModel<T extends GeoAnimatable> extends GeoModel<T> {

    protected final ResourceLocation model;
    protected final ResourceLocation texture;
    protected final ResourceLocation animations;

    public SimpleGeoModel(String modelPath, String texturePath, String animationPath) {
        this.model = ResourceLocation.fromNamespaceAndPath(Vuties.MODID, modelPath);
        this.texture = ResourceLocation.fromNamespaceAndPath(Vuties.MODID, texturePath);
        this.animations = animationPath != null ? ResourceLocation.fromNamespaceAndPath(Vuties.MODID, animationPath) : null;
    }

    public SimpleGeoModel(String name, String texturePath) {
        this("geo/" + name + ".geo.json", texturePath, "animations/" + name + ".animation.json");
    }

    @Override
    public ResourceLocation getModelResource(T animatable, GeoRenderer<T> renderer) {
        return this.model;
    }

    @Override
    public ResourceLocation getTextureResource(T animatable, GeoRenderer<T> renderer) {
        return this.texture;
    }

    @Override
    public ResourceLocation getModelResource(T animatable) {
        return this.model;
    }

    @Override
    public ResourceLocation getTextureResource(T animatable) {
        return this.texture;
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        return this.animations;
    }
}
