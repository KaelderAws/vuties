package com.vibe.vuties.client;

import com.vibe.vuties.item.AceHelmetItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class AceHelmetGeoRenderer extends GeoArmorRenderer<AceHelmetItem> {
    public AceHelmetGeoRenderer() {
        super(new AceHelmetGeoModel());
    }

    @Override
    public RenderType getRenderType(AceHelmetItem animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        // entityTranslucent allows the visor glass to render with smooth semi-transparency
        return RenderType.entityTranslucent(texture);
    }
}
