package com.vibe.vuties.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

public class RotorpackClientExtensions implements IClientItemExtensions {
    public static final RotorpackClientExtensions INSTANCE = new RotorpackClientExtensions();
    private static RotorpackGeoRenderer cachedRenderer;

    @Override
    public HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
        if (cachedRenderer == null) {
            cachedRenderer = new RotorpackGeoRenderer();
        }
        return cachedRenderer;
    }
}
