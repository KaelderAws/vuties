package com.vibe.vuties.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

public class AceHelmetClientExtensions implements IClientItemExtensions {
    public static final AceHelmetClientExtensions INSTANCE = new AceHelmetClientExtensions();
    private static AceHelmetGeoRenderer cachedArmorRenderer;
    private static AceHelmetItemRenderer cachedItemRenderer;

    @Override
    public HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
        if (cachedArmorRenderer == null) {
            cachedArmorRenderer = new AceHelmetGeoRenderer();
        }
        return cachedArmorRenderer;
    }

    @Override
    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
        if (cachedItemRenderer == null) {
            cachedItemRenderer = new AceHelmetItemRenderer();
        }
        return cachedItemRenderer;
    }
}
