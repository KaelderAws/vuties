package com.vibe.vuties.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

public class JetBootsClientExtensions implements IClientItemExtensions {
    public static final JetBootsClientExtensions INSTANCE = new JetBootsClientExtensions();
    private static JetBootsGeoRenderer cachedArmorRenderer;
    private static JetBootsItemRenderer cachedItemRenderer;

    @Override
    public HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
        if (cachedArmorRenderer == null) {
            cachedArmorRenderer = new JetBootsGeoRenderer();
        }
        return cachedArmorRenderer;
    }

    @Override
    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
        if (cachedItemRenderer == null) {
            cachedItemRenderer = new JetBootsItemRenderer();
        }
        return cachedItemRenderer;
    }
}
