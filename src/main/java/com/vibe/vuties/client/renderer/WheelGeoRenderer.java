package com.vibe.vuties.client.renderer;

import com.vibe.vuties.block.entity.WheelBlockEntity;
import com.vibe.vuties.client.WheelGeoModel;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class WheelGeoRenderer extends GeoBlockRenderer<WheelBlockEntity> {
    public WheelGeoRenderer() {
        super(new WheelGeoModel());
    }

    @Override
    public boolean shouldRenderOffScreen(WheelBlockEntity blockEntity) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(WheelBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(3.0D, 5.0D, 3.0D);
    }
}
