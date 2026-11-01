package com.vibe.vuties.client;

import com.vibe.vuties.item.GauntletItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class GauntletGeoRenderer extends GeoArmorRenderer<GauntletItem> {
    public GauntletGeoRenderer() {
        super(new GauntletGeoModel());
    }

    @Override
    public RenderType getRenderType(GauntletItem animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutoutNoCull(texture);
    }
}
