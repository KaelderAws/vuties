package com.vibe.vuties.item;

import com.vibe.vuties.Vuties;
import com.vibe.vuties.client.ExoLegsClientExtensions;
import com.vibe.vuties.client.ExoLegsGeoRenderer;
import com.vibe.vuties.client.ExoLegsItemRenderer;
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
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;

public class ExoLegsItem extends ArmorItem implements GeoItem {

    private static final ResourceLocation ARMOR_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(Vuties.MODID, "leggings_armor");
    private static final ResourceLocation TOUGHNESS_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(Vuties.MODID, "leggings_armor_toughness");
    private static final ResourceLocation KNOCKBACK_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(Vuties.MODID, "leggings_armor_knockback_resistance");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ExoLegsItem(Properties properties) {
        super(ModArmorMaterials.EXO_LEGS_MATERIAL, ArmorItem.Type.LEGGINGS, properties.stacksTo(1));
    }

    public static ArmorUpgradeTier getArmorUpgrade(ItemStack stack) {
        return stack != null ? stack.getOrDefault(ModDataComponents.UPGRADE_ARMOR.get(), ArmorUpgradeTier.NONE) : ArmorUpgradeTier.NONE;
    }

    public static void setArmorUpgrade(ItemStack stack, ArmorUpgradeTier tier) {
        if (stack != null) {
            stack.set(ModDataComponents.UPGRADE_ARMOR.get(), tier);
        }
    }

    public static boolean hasPhaseUpgrade(ItemStack stack) {
        return stack != null && stack.getOrDefault(ModDataComponents.UPGRADE_PHASE.get(), false);
    }

    public static void setPhaseUpgrade(ItemStack stack, boolean has) {
        if (stack != null) {
            stack.set(ModDataComponents.UPGRADE_PHASE.get(), has);
        }
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        ArmorUpgradeTier tier = getArmorUpgrade(stack);
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();

        int armor = tier.getLeggingsArmor();
        float toughness = tier.getToughness();
        float knockback = tier.getKnockbackResistance();

        builder.add(Attributes.ARMOR,
                new AttributeModifier(ARMOR_MODIFIER_ID, armor, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.LEGS);

        if (toughness > 0.0F) {
            builder.add(Attributes.ARMOR_TOUGHNESS,
                    new AttributeModifier(TOUGHNESS_MODIFIER_ID, toughness, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.LEGS);
        }

        if (knockback > 0.0F) {
            builder.add(Attributes.KNOCKBACK_RESISTANCE,
                    new AttributeModifier(KNOCKBACK_MODIFIER_ID, knockback, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.LEGS);
        }

        return builder.build();
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ArmorUpgradeTier tier = getArmorUpgrade(stack);
        String tierKey = switch (tier) {
            case IRON -> "iron";
            case DIAMOND -> "diamond";
            case NETHERITE -> "netherite";
            default -> "none";
        };

        boolean hasPhase = hasPhaseUpgrade(stack);
        if (hasPhase) {
            tooltip.add(Component.translatable("tooltip.vuties.exo_legs.ability")
                    .withStyle(ChatFormatting.AQUA));
        } else {
            tooltip.add(Component.translatable("tooltip.vuties.exo_legs.no_phase_upgrade")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }

        tooltip.add(Component.translatable("tooltip.vuties.exo_legs.armor_tier." + tierKey));

        if (hasPhase) {
            tooltip.add(Component.literal(""));
            tooltip.add(Component.translatable("tooltip.vuties.exo_legs.controls")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }

        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 2, state -> {
            state.setAnimation(RawAnimation.begin().thenLoop("idle"));
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private ExoLegsGeoRenderer armorRenderer;
            private ExoLegsItemRenderer itemRenderer;

            @Override
            public <T extends LivingEntity> HumanoidModel<?> getGeoArmorRenderer(@Nullable T livingEntity, ItemStack itemStack, @Nullable EquipmentSlot equipmentSlot, @Nullable HumanoidModel<T> original) {
                if (this.armorRenderer == null) {
                    this.armorRenderer = new ExoLegsGeoRenderer();
                }
                return this.armorRenderer;
            }

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (this.itemRenderer == null) {
                    this.itemRenderer = new ExoLegsItemRenderer();
                }
                return this.itemRenderer;
            }
        });
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(ExoLegsClientExtensions.INSTANCE);
    }
}
