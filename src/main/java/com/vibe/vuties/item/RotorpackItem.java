package com.vibe.vuties.item;

import com.vibe.vuties.client.RotorpackClientExtensions;
import com.vibe.vuties.client.RotorpackGeoRenderer;
import com.vibe.vuties.init.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.Vec3;
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

import java.util.List;
import java.util.function.Consumer;

public class RotorpackItem extends ArmorItem implements GeoItem {

    public enum Tier {
        MK1(1, false, false, "I"),
        MK2(2, true, false, "II"),
        MK3(3, true, true, "III");

        private final int level;
        private final boolean canHover;
        private final boolean hasWings;
        private final String roman;

        Tier(int level, boolean canHover, boolean hasWings, String roman) {
            this.level = level;
            this.canHover = canHover;
            this.hasWings = hasWings;
            this.roman = roman;
        }

        public int getLevel() {
            return this.level;
        }

        public boolean canHover() {
            return this.canHover;
        }

        public boolean hasWings() {
            return this.hasWings;
        }

        public String getRoman() {
            return this.roman;
        }
    }

    private final Tier tier;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public RotorpackItem(Tier tier, Holder<ArmorMaterial> material, Properties properties) {
        // Прочность убрана: stacksTo(1) без durability
        super(material, ArmorItem.Type.CHESTPLATE, properties.stacksTo(1));
        this.tier = tier;
    }

    public Tier getTier() {
        return this.tier;
    }

    public static boolean isEngineEnabled(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ENGINE_ENABLED.get(), true);
    }

    public static void setEngineEnabled(ItemStack stack, boolean enabled) {
        stack.set(ModDataComponents.ENGINE_ENABLED.get(), enabled);
    }

    public static boolean isHoverMode(ItemStack stack) {
        if (stack.getItem() instanceof RotorpackItem rotorpack && !rotorpack.getTier().canHover()) {
            return false;
        }
        return stack.getOrDefault(ModDataComponents.HOVER_MODE.get(), false);
    }

    public static void setHoverMode(ItemStack stack, boolean hover) {
        if (stack.getItem() instanceof RotorpackItem rotorpack && !rotorpack.getTier().canHover()) {
            stack.set(ModDataComponents.HOVER_MODE.get(), false);
            return;
        }
        stack.set(ModDataComponents.HOVER_MODE.get(), hover);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        boolean engine = isEngineEnabled(stack);
        boolean hover = isHoverMode(stack);

        // Описание тира
        tooltip.add(Component.translatable("tooltip.vuties.rotorpack.desc.mk" + this.tier.getLevel())
                .withStyle(ChatFormatting.GRAY));

        // Статус двигателя
        tooltip.add(Component.translatable("tooltip.vuties.rotorpack.engine",
                engine ? Component.translatable("tooltip.vuties.status.on").withStyle(ChatFormatting.GREEN)
                       : Component.translatable("tooltip.vuties.status.off").withStyle(ChatFormatting.RED))
                .withStyle(ChatFormatting.YELLOW));

        // Статус зависания (Hover)
        if (this.tier.canHover()) {
            tooltip.add(Component.translatable("tooltip.vuties.rotorpack.hover",
                    hover ? Component.translatable("tooltip.vuties.status.on").withStyle(ChatFormatting.AQUA)
                          : Component.translatable("tooltip.vuties.status.off").withStyle(ChatFormatting.DARK_GRAY))
                    .withStyle(ChatFormatting.YELLOW));
        } else {
            tooltip.add(Component.translatable("tooltip.vuties.rotorpack.hover_locked")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }

        // Подсказки управления
        tooltip.add(Component.translatable("tooltip.vuties.rotorpack.controls.mk" + this.tier.getLevel())
                .withStyle(ChatFormatting.DARK_AQUA));

        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public boolean canElytraFly(ItemStack stack, LivingEntity entity) {
        // Крылья доступны ТОЛЬКО у Mk III
        return this.tier.hasWings() && isEngineEnabled(stack) && !entity.onGround() && (entity.isSprinting() || entity.isFallFlying());
    }

    @Override
    public boolean elytraFlightTick(ItemStack stack, LivingEntity entity, int flightTicks) {
        if (!this.tier.hasWings() || !isEngineEnabled(stack)) {
            return false;
        }
        Vec3 look = entity.getLookAngle();
        double cruiseSpeed = 1.25;
        entity.setDeltaMovement(look.scale(cruiseSpeed));
        entity.resetFallDistance();
        return true;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "rotors", 0, state -> {
            ItemStack stack = state.getData(DataTickets.ITEMSTACK);
            if (stack != null && isEngineEnabled(stack)) {
                return state.setAndContinue(RawAnimation.begin().thenLoop("animation.rotorpack.spin"));
            }
            return PlayState.STOP;
        }));

        controllers.add(new AnimationController<>(this, "wings", 10, state -> {
            if (!this.tier.hasWings()) {
                return PlayState.STOP;
            }
            ItemStack stack = state.getData(DataTickets.ITEMSTACK);
            if (stack != null && isEngineEnabled(stack)) {
                return state.setAndContinue(RawAnimation.begin().thenLoop("animation.rotorpack.fly"));
            }
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.rotorpack.idle"));
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private RotorpackGeoRenderer renderer;

            @Override
            public <T extends LivingEntity> net.minecraft.client.model.HumanoidModel<?> getGeoArmorRenderer(@Nullable T livingEntity, ItemStack itemStack, @Nullable EquipmentSlot equipmentSlot, @Nullable net.minecraft.client.model.HumanoidModel<T> original) {
                if (this.renderer == null) {
                    this.renderer = new RotorpackGeoRenderer();
                }
                return this.renderer;
            }
        });
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(RotorpackClientExtensions.INSTANCE);
    }
}

