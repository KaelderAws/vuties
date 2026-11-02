package com.vibe.vuties.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;

import java.util.function.Supplier;

public class ModGeoRenderProvider implements GeoRenderProvider {
    private final Supplier<HumanoidModel<?>> armorRendererSupplier;
    private final Supplier<BlockEntityWithoutLevelRenderer> itemRendererSupplier;
    private HumanoidModel<?> armorRenderer;
    private BlockEntityWithoutLevelRenderer itemRenderer;

    public ModGeoRenderProvider(@Nullable Supplier<HumanoidModel<?>> armorRendererSupplier, @Nullable Supplier<BlockEntityWithoutLevelRenderer> itemRendererSupplier) {
        this.armorRendererSupplier = armorRendererSupplier;
        this.itemRendererSupplier = itemRendererSupplier;
    }

    @Override
    public <T extends LivingEntity> HumanoidModel<?> getGeoArmorRenderer(@Nullable T livingEntity, ItemStack itemStack, @Nullable EquipmentSlot equipmentSlot, @Nullable HumanoidModel<T> original) {
        if (this.armorRenderer == null && this.armorRendererSupplier != null) {
            this.armorRenderer = this.armorRendererSupplier.get();
        }
        return this.armorRenderer;
    }

    @Override
    public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
        if (this.itemRenderer == null && this.itemRendererSupplier != null) {
            this.itemRenderer = this.itemRendererSupplier.get();
        }
        return this.itemRenderer;
    }
}
