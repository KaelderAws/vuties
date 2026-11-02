package com.vibe.vuties.client;

import net.minecraft.world.entity.WalkAnimationState;

import java.lang.reflect.Field;

public class WalkAnimHelper {
    private static Field walkPositionField = null;
    private static Field walkSpeedOldField = null;
    private static boolean walkFieldsInit = false;

    private static void initWalkFields() {
        if (walkFieldsInit) return;
        walkFieldsInit = true;
        try {
            walkPositionField = WalkAnimationState.class.getDeclaredField("position");
            walkPositionField.setAccessible(true);
        } catch (Exception e) {
            Field[] fields = WalkAnimationState.class.getDeclaredFields();
            for (Field f : fields) {
                if (f.getType() == float.class) {
                    f.setAccessible(true);
                    if (walkPositionField == null && f.getName().toLowerCase().contains("pos")) {
                        walkPositionField = f;
                    }
                }
            }
        }
        try {
            walkSpeedOldField = WalkAnimationState.class.getDeclaredField("speedOld");
            walkSpeedOldField.setAccessible(true);
        } catch (Exception e) {
            Field[] fields = WalkAnimationState.class.getDeclaredFields();
            for (Field f : fields) {
                if (f.getType() == float.class) {
                    f.setAccessible(true);
                    if (walkSpeedOldField == null && f.getName().toLowerCase().contains("old")) {
                        walkSpeedOldField = f;
                    }
                }
            }
        }
    }

    public static void setAnimation(WalkAnimationState walkAnimation, float position, float speed) {
        initWalkFields();
        if (walkPositionField != null) {
            try {
                walkPositionField.setFloat(walkAnimation, position);
            } catch (Exception ignored) {}
        }
        walkAnimation.setSpeed(speed);
        if (walkSpeedOldField != null) {
            try {
                walkSpeedOldField.setFloat(walkAnimation, speed);
            } catch (Exception ignored) {}
        }
    }
}
