package com.vibe.vuties.client;

import com.vibe.vuties.item.GauntletItem;

public class ClawGeoModel extends SimpleGeoModel<GauntletItem> {
    public ClawGeoModel() {
        super("geo/claw.geo.json", "textures/entity/claw.png", "animations/gauntlet.animation.json");
    }
}
