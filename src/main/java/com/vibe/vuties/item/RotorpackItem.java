package com.vibe.vuties.item;

import com.vibe.vuties.Vuties;
import com.vibe.vuties.client.RotorpackClientExtensions;
import com.vibe.vuties.client.RotorpackGeoRenderer;
import com.vibe.vuties.client.RotorpackItemRenderer;
import com.vibe.vuties.init.ModArmorMaterials;
import com.vibe.vuties.init.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
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

    private static final ResourceLocation ARMOR_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(Vuties.MODID, "armor");
    private static final ResourceLocation TOUGHNESS_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(Vuties.MODID, "armor_toughness");
    private static final ResourceLocation KNOCKBACK_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(Vuties.MODID, "armor_knockback_resistance");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public RotorpackItem(Properties properties) {
        super(ModArmorMaterials.ROTORPACK_MATERIAL, ArmorItem.Type.CHESTPLATE, properties.stacksTo(1));
    }

    public static boolean isEngineEnabled(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ENGINE_ENABLED.get(), true);
    }

    public static void setEngineEnabled(ItemStack stack, boolean enabled) {
        stack.set(ModDataComponents.ENGINE_ENABLED.get(), enabled);
    }

    public static boolean isHoverMode(ItemStack stack) {
        if (!hasHoverUpgrade(stack)) {
            return false;
        }
        return stack.getOrDefault(ModDataComponents.HOVER_MODE.get(), false);
    }

    public static void setHoverMode(ItemStack stack, boolean hover) {
        if (!hasHoverUpgrade(stack)) {
            stack.set(ModDataComponents.HOVER_MODE.get(), false);
            return;
        }
        stack.set(ModDataComponents.HOVER_MODE.get(), hover);
    }

    public static boolean hasHoverUpgrade(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.UPGRADE_HOVER.get(), false);
    }

    public static void setHoverUpgrade(ItemStack stack, boolean hover) {
        stack.set(ModDataComponents.UPGRADE_HOVER.get(), hover);
        if (!hover) {
            stack.set(ModDataComponents.HOVER_MODE.get(), false);
        }
    }

    public static boolean hasWingsUpgrade(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.UPGRADE_WINGS.get(), false);
    }

    public static void setWingsUpgrade(ItemStack stack, boolean wings) {
        stack.set(ModDataComponents.UPGRADE_WINGS.get(), wings);
    }

    public static ArmorUpgradeTier getArmorUpgrade(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.UPGRADE_ARMOR.get(), ArmorUpgradeTier.NONE);
    }

    public static void setArmorUpgrade(ItemStack stack, ArmorUpgradeTier tier) {
        stack.set(ModDataComponents.UPGRADE_ARMOR.get(), tier);
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        ArmorUpgradeTier tier = getArmorUpgrade(stack);
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();

        int armor = tier.getArmor();
        float toughness = tier.getToughness();
        float knockback = tier.getKnockbackResistance();

        builder.add(Attributes.ARMOR,
                new AttributeModifier(ARMOR_MODIFIER_ID, armor, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.CHEST);

        if (toughness > 0.0F) {
            builder.add(Attributes.ARMOR_TOUGHNESS,
                    new AttributeModifier(TOUGHNESS_MODIFIER_ID, toughness, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.CHEST);
        }

        if (knockback > 0.0F) {
            builder.add(Attributes.KNOCKBACK_RESISTANCE,
                    new AttributeModifier(KNOCKBACK_MODIFIER_ID, knockback, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.CHEST);
        }

        return builder.build();
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        boolean engine = isEngineEnabled(stack);
        boolean hoverInstalled = hasHoverUpgrade(stack);
        boolean wingsInstalled = hasWingsUpgrade(stack);
        ArmorUpgradeTier armorTier = getArmorUpgrade(stack);

        // Статус двигателя
        tooltip.add(Component.translatable("tooltip.vuties.rotorpack.engine",
                engine ? Component.translatable("tooltip.vuties.status.on").withStyle(ChatFormatting.GREEN)
                       : Component.translatable("tooltip.vuties.status.off").withStyle(ChatFormatting.RED))
                .withStyle(ChatFormatting.YELLOW));

        // Статус зависания (Hover)
        if (hoverInstalled) {
            boolean hoverActive = isHoverMode(stack);
            tooltip.add(Component.translatable("tooltip.vuties.rotorpack.hover",
                    hoverActive ? Component.translatable("tooltip.vuties.status.on").withStyle(ChatFormatting.AQUA)
                                : Component.translatable("tooltip.vuties.status.off").withStyle(ChatFormatting.DARK_GRAY))
                    .withStyle(ChatFormatting.YELLOW));
        } else {
            tooltip.add(Component.translatable("tooltip.vuties.rotorpack.hover_locked")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }

        // Статус крыльев
        if (wingsInstalled) {
            tooltip.add(Component.translatable("tooltip.vuties.rotorpack.wings_installed")
                    .withStyle(ChatFormatting.GREEN));
        } else {
            tooltip.add(Component.translatable("tooltip.vuties.rotorpack.wings_locked")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }

        // Статус брони
        tooltip.add(Component.translatable("tooltip.vuties.rotorpack.armor_tier." + armorTier.getSerializedName())
                .withStyle(ChatFormatting.GOLD));

        // Подсказки управления
        tooltip.add(Component.translatable("tooltip.vuties.rotorpack.controls")
                .withStyle(ChatFormatting.DARK_AQUA));

        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public boolean canElytraFly(ItemStack stack, LivingEntity entity) {
        return hasWingsUpgrade(stack) && isEngineEnabled(stack) && !entity.onGround() && (entity.isSprinting() || entity.isFallFlying());
    }

    @Override
    public boolean elytraFlightTick(ItemStack stack, LivingEntity entity, int flightTicks) {
        if (!hasWingsUpgrade(stack) || !isEngineEnabled(stack)) {
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
            ItemStack stack = state.getData(DataTickets.ITEMSTACK);
            if (stack == null || !hasWingsUpgrade(stack)) {
                return PlayState.STOP;
            }
            if (isEngineEnabled(stack)) {
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
            private RotorpackGeoRenderer armorRenderer;
            private RotorpackItemRenderer itemRenderer;

            @Override
            public <T extends LivingEntity> net.minecraft.client.model.HumanoidModel<?> getGeoArmorRenderer(@Nullable T livingEntity, ItemStack itemStack, @Nullable EquipmentSlot equipmentSlot, @Nullable net.minecraft.client.model.HumanoidModel<T> original) {
                if (this.armorRenderer == null) {
                    this.armorRenderer = new RotorpackGeoRenderer();
                }
                return this.armorRenderer;
            }

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (this.itemRenderer == null) {
                    this.itemRenderer = new RotorpackItemRenderer();
                }
                return this.itemRenderer;
            }
        });
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(RotorpackClientExtensions.INSTANCE);
    }
}
