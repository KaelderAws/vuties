package com.vibe.vuties.item;

import com.vibe.vuties.Vuties;
import com.vibe.vuties.client.AceHelmetClientExtensions;
import com.vibe.vuties.client.AceHelmetGeoRenderer;
import com.vibe.vuties.client.AceHelmetItemRenderer;
import com.vibe.vuties.init.ModArmorMaterials;
import com.vibe.vuties.init.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
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

public class AceHelmetItem extends ArmorItem implements GeoItem {

    private static final ResourceLocation ARMOR_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(Vuties.MODID, "helmet_armor");
    private static final ResourceLocation TOUGHNESS_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(Vuties.MODID, "helmet_armor_toughness");
    private static final ResourceLocation KNOCKBACK_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(Vuties.MODID, "helmet_armor_knockback_resistance");

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

    public static boolean hasWaterBreathingUpgrade(ItemStack stack) {
        return stack != null && stack.getOrDefault(ModDataComponents.UPGRADE_WATER_BREATHING.get(), false);
    }

    public static void setWaterBreathingUpgrade(ItemStack stack, boolean has) {
        if (stack != null) {
            stack.set(ModDataComponents.UPGRADE_WATER_BREATHING.get(), has);
        }
    }

    public static boolean hasNightVisionUpgrade(ItemStack stack) {
        return stack != null && stack.getOrDefault(ModDataComponents.UPGRADE_NIGHT_VISION.get(), false);
    }

    public static void setNightVisionUpgrade(ItemStack stack, boolean has) {
        if (stack != null) {
            stack.set(ModDataComponents.UPGRADE_NIGHT_VISION.get(), has);
        }
    }

    public static boolean isNightVisionEnabled(ItemStack stack) {
        return stack != null && stack.getOrDefault(ModDataComponents.NIGHT_VISION_ENABLED.get(), true);
    }

    public static void setNightVisionEnabled(ItemStack stack, boolean enabled) {
        if (stack != null) {
            stack.set(ModDataComponents.NIGHT_VISION_ENABLED.get(), enabled);
        }
    }

    public static boolean isNightVisionActive(ItemStack stack) {
        return hasNightVisionUpgrade(stack) && !isHelmetOpen(stack) && isNightVisionEnabled(stack);
    }

    public static boolean isWaterBreathingActive(ItemStack stack) {
        return hasWaterBreathingUpgrade(stack) && !isHelmetOpen(stack);
    }

    public static ArmorUpgradeTier getArmorUpgrade(ItemStack stack) {
        return stack != null ? stack.getOrDefault(ModDataComponents.UPGRADE_ARMOR.get(), ArmorUpgradeTier.NONE) : ArmorUpgradeTier.NONE;
    }

    public static void setArmorUpgrade(ItemStack stack, ArmorUpgradeTier tier) {
        if (stack != null) {
            stack.set(ModDataComponents.UPGRADE_ARMOR.get(), tier);
        }
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        ArmorUpgradeTier tier = getArmorUpgrade(stack);
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();

        int armor = tier.getHelmetArmor();
        float toughness = tier.getToughness();
        float knockback = tier.getKnockbackResistance();

        builder.add(Attributes.ARMOR,
                new AttributeModifier(ARMOR_MODIFIER_ID, armor, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.HEAD);

        if (toughness > 0.0F) {
            builder.add(Attributes.ARMOR_TOUGHNESS,
                    new AttributeModifier(TOUGHNESS_MODIFIER_ID, toughness, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.HEAD);
        }

        if (knockback > 0.0F) {
            builder.add(Attributes.KNOCKBACK_RESISTANCE,
                    new AttributeModifier(KNOCKBACK_MODIFIER_ID, knockback, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.HEAD);
        }

        return builder.build();
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        boolean maskOpen = isHelmetOpen(stack);
        boolean waterBreathing = hasWaterBreathingUpgrade(stack);
        boolean nightVisionInstalled = hasNightVisionUpgrade(stack);
        boolean nightVisionEnabled = isNightVisionEnabled(stack);
        ArmorUpgradeTier armorTier = getArmorUpgrade(stack);

        // Статус забрала / маски
        tooltip.add(Component.translatable("tooltip.vuties.ace_helmet.mask",
                maskOpen ? Component.translatable("tooltip.vuties.ace_helmet.mask.open").withStyle(ChatFormatting.RED)
                         : Component.translatable("tooltip.vuties.ace_helmet.mask.closed").withStyle(ChatFormatting.GREEN))
                .withStyle(ChatFormatting.YELLOW));

        // Статус дыхания под водой
        if (waterBreathing) {
            if (!maskOpen) {
                tooltip.add(Component.translatable("tooltip.vuties.ace_helmet.water_breathing_active")
                        .withStyle(ChatFormatting.GREEN));
            } else {
                tooltip.add(Component.translatable("tooltip.vuties.ace_helmet.water_breathing_unsealed")
                        .withStyle(ChatFormatting.RED));
            }
        } else {
            tooltip.add(Component.translatable("tooltip.vuties.ace_helmet.water_breathing_locked")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }

        // Статус ночного зрения
        if (nightVisionInstalled) {
            if (!maskOpen) {
                if (nightVisionEnabled) {
                    tooltip.add(Component.translatable("tooltip.vuties.ace_helmet.night_vision_active")
                            .withStyle(ChatFormatting.GREEN));
                } else {
                    tooltip.add(Component.translatable("tooltip.vuties.ace_helmet.night_vision_off")
                            .withStyle(ChatFormatting.GRAY));
                }
            } else {
                tooltip.add(Component.translatable("tooltip.vuties.ace_helmet.night_vision_unsealed")
                        .withStyle(ChatFormatting.RED));
            }
        } else {
            tooltip.add(Component.translatable("tooltip.vuties.ace_helmet.night_vision_locked")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }

        // Статус брони
        tooltip.add(Component.translatable("tooltip.vuties.ace_helmet.armor_tier." + armorTier.getSerializedName())
                .withStyle(ChatFormatting.GOLD));

        // Подсказки управления
        tooltip.add(Component.translatable("tooltip.vuties.ace_helmet.controls")
                .withStyle(ChatFormatting.DARK_AQUA));

        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
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
