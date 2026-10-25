package com.vibe.vuties.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public class ModKeyMappings {
    public static final KeyMapping TOGGLE_ENGINE = new KeyMapping(
        "key.vuties.engine_toggle",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_V,
        "key.categories.vuties"
    );

    public static final KeyMapping TOGGLE_HOVER = new KeyMapping(
        "key.vuties.hover_toggle",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_H,
        "key.categories.vuties"
    );

    public static final KeyMapping TOGGLE_HELMET = new KeyMapping(
        "key.vuties.helmet_toggle",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_X,
        "key.categories.vuties"
    );
}
