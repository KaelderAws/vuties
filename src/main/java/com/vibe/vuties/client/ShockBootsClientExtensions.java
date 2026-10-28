package com.vibe.vuties.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

public class ShockBootsClientExtensions implements IClientItemExtensions {
    public static final ShockBootsClientExtensions INSTANCE = new ShockBootsClientExtensions();
    private static ShockBootsGeoRenderer cachedArmorRenderer;
    private static ShockBootsItemRenderer cachedItemRenderer;

    @Override
    public HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
        if (cachedArmorRenderer == null) {
            cachedArmorRenderer = new ShockBootsGeoRenderer();
        }
        return cachedArmorRenderer;
    }

    @Override
    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
        if (cachedItemRenderer == null) {
            cachedItemRenderer = new ShockBootsItemRenderer();
        }
        return cachedItemRenderer;
    }
}
