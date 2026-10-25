package com.vibe.vuties.item;

import com.vibe.vuties.client.AceHelmetClientExtensions;
import com.vibe.vuties.client.AceHelmetGeoRenderer;
import com.vibe.vuties.client.AceHelmetItemRenderer;
import com.vibe.vuties.init.ModArmorMaterials;
import com.vibe.vuties.init.ModDataComponents;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

public class AceHelmetItem extends ArmorItem implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public AceHelmetItem(Properties properties) {
        super(ModArmorMaterials.ACE_HELMET_MATERIAL, ArmorItem.Type.HELMET, properties.stacksTo(1));
    }

    public static boolean isHelmetOpen(ItemStack stack) {
        return stack != null && stack.getOrDefault(ModDataComponents.HELMET_OPEN.get(), false);
    }

    public static void setHelmetOpen(ItemStack stack, boolean open) {
        if (stack != null) {
            stack.set(ModDataComponents.HELMET_OPEN.get(), open);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Контроллер анимации открытия/закрытия маски и визора (с плавным переходом в 6 тиков)
        controllers.add(new AnimationController<>(this, "mask_visor_controller", 6, state -> {
            ItemStack stack = state.getData(DataTickets.ITEMSTACK);
            boolean open = isHelmetOpen(stack);
            if (open) {
                return state.setAndContinue(RawAnimation.begin().thenLoop("animation.ace_helmet.open"));
            } else {
                return state.setAndContinue(RawAnimation.begin().thenLoop("animation.ace_helmet.closed"));
            }
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private AceHelmetGeoRenderer armorRenderer;
            private AceHelmetItemRenderer itemRenderer;

            @Override
            public <T extends LivingEntity> HumanoidModel<?> getGeoArmorRenderer(@Nullable T livingEntity, ItemStack itemStack, @Nullable EquipmentSlot equipmentSlot, @Nullable HumanoidModel<T> original) {
                if (this.armorRenderer == null) {
                    this.armorRenderer = new AceHelmetGeoRenderer();
                }
                return this.armorRenderer;
            }

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (this.itemRenderer == null) {
                    this.itemRenderer = new AceHelmetItemRenderer();
                }
                return this.itemRenderer;
            }
        });
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(AceHelmetClientExtensions.INSTANCE);
    }
}
