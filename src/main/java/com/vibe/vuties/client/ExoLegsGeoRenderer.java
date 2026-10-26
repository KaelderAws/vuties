package com.vibe.vuties.client;

import com.vibe.vuties.item.ExoLegsItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class ExoLegsGeoRenderer extends GeoArmorRenderer<ExoLegsItem> {
    public ExoLegsGeoRenderer() {
        super(new ExoLegsGeoModel());
    }

    @Override
    public RenderType getRenderType(ExoLegsItem animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }

    @Override
    protected void applyBoneVisibilityBySlot(EquipmentSlot currentSlot) {
        super.applyBoneVisibilityBySlot(currentSlot);
        if (currentSlot == EquipmentSlot.LEGS && this.body != null) {
            setBoneVisible(this.body, true);
        }
    }
}
