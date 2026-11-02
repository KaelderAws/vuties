package com.vibe.vuties.client;

import com.vibe.vuties.item.GauntletItem;

public class DrillGeoModel extends SimpleGeoModel<GauntletItem> {
    public DrillGeoModel() {
        super("geo/drill.geo.json", "textures/entity/drill.png", "animations/gauntlet.animation.json");
    }
}
