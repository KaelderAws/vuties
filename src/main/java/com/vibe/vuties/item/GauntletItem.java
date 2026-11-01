package com.vibe.vuties.item;

import com.vibe.vuties.Vuties;
import com.vibe.vuties.client.GauntletClientExtensions;
import com.vibe.vuties.client.GauntletGeoRenderer;
import com.vibe.vuties.client.GauntletItemRenderer;
import com.vibe.vuties.init.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
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

public class GauntletItem extends Item implements GeoItem {

    public static final int MODE_NORMAL = 0;
    public static final int MODE_DRILL = 1;

    private static final ResourceLocation ARMOR_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(Vuties.MODID, "gauntlet_armor");
    private static final ResourceLocation TOUGHNESS_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(Vuties.MODID, "gauntlet_toughness");
    private static final ResourceLocation KNOCKBACK_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(Vuties.MODID, "gauntlet_knockback_resistance");
    private static final ResourceLocation ATTACK_DAMAGE_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(Vuties.MODID, "gauntlet_attack_damage");
    private static final ResourceLocation ATTACK_SPEED_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(Vuties.MODID, "gauntlet_attack_speed");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public GauntletItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static boolean hasDrillUpgrade(ItemStack stack) {
        return stack != null && stack.getOrDefault(ModDataComponents.UPGRADE_DRILL.get(), false);
    }

    public static void setDrillUpgrade(ItemStack stack, boolean has) {
        if (stack != null) {
            stack.set(ModDataComponents.UPGRADE_DRILL.get(), has);
            if (!has && getMode(stack) == MODE_DRILL) {
                setMode(stack, MODE_NORMAL);
            }
        }
    }

    public static int getMode(ItemStack stack) {
        return stack != null ? stack.getOrDefault(ModDataComponents.GAUNTLET_MODE.get(), MODE_NORMAL) : MODE_NORMAL;
    }

    public static void setMode(ItemStack stack, int mode) {
        if (stack != null) {
            stack.set(ModDataComponents.GAUNTLET_MODE.get(), mode);
        }
    }

    public static boolean isDrillActive(ItemStack stack) {
        return stack != null && hasDrillUpgrade(stack) && getMode(stack) == MODE_DRILL;
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
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        if (isDrillActive(stack)) {
            // Drill harvests any block that is not unbreakable (destroySpeed >= 0)
            return state.getDestroySpeed(null, BlockPos.ZERO) >= 0;
        }
        return super.isCorrectToolForDrops(stack, state);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (isDrillActive(stack)) {
            if (state.getDestroySpeed(null, BlockPos.ZERO) < 0) {
                return 0.0F; // Bedrock and unbreakable blocks cannot be mined
            }
            return 35.0F;
        }
        return super.getDestroySpeed(stack, state);
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        ArmorUpgradeTier tier = getArmorUpgrade(stack);
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();

        int armor = switch (tier) {
            case IRON -> 1;
            case DIAMOND -> 2;
            case NETHERITE -> 3;
            default -> 0;
        };
        float toughness = tier.getToughness();
        float knockback = tier.getKnockbackResistance();

        if (armor > 0) {
            builder.add(Attributes.ARMOR,
                    new AttributeModifier(ARMOR_MODIFIER_ID, armor, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.HAND);
        }

        if (toughness > 0.0F) {
            builder.add(Attributes.ARMOR_TOUGHNESS,
                    new AttributeModifier(TOUGHNESS_MODIFIER_ID, toughness, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.HAND);
        }

        if (knockback > 0.0F) {
            builder.add(Attributes.KNOCKBACK_RESISTANCE,
                    new AttributeModifier(KNOCKBACK_MODIFIER_ID, knockback, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.HAND);
        }

        if (isDrillActive(stack)) {
            builder.add(Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(ATTACK_DAMAGE_MODIFIER_ID, 8.0, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND);
            builder.add(Attributes.ATTACK_SPEED,
                    new AttributeModifier(ATTACK_SPEED_MODIFIER_ID, 1000.0, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND);
        }

        return builder.build();
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int mode = getMode(stack);
        if (mode == MODE_DRILL && hasDrillUpgrade(stack)) {
            tooltip.add(Component.translatable("tooltip.vuties.gauntlet.mode.drill").withStyle(ChatFormatting.GREEN));
        } else {
            tooltip.add(Component.translatable("tooltip.vuties.gauntlet.mode.normal").withStyle(ChatFormatting.AQUA));
        }

        if (hasDrillUpgrade(stack)) {
            tooltip.add(Component.translatable("tooltip.vuties.gauntlet.drill_installed").withStyle(ChatFormatting.GREEN));
        } else {
            tooltip.add(Component.translatable("tooltip.vuties.gauntlet.drill_locked").withStyle(ChatFormatting.DARK_GRAY));
        }

        ArmorUpgradeTier armorTier = getArmorUpgrade(stack);
        Component armorComponent = switch (armorTier) {
            case IRON -> Component.translatable("tooltip.vuties.gauntlet.armor_tier.iron");
            case DIAMOND -> Component.translatable("tooltip.vuties.gauntlet.armor_tier.diamond");
            case NETHERITE -> Component.translatable("tooltip.vuties.gauntlet.armor_tier.netherite");
            default -> Component.translatable("tooltip.vuties.gauntlet.armor_tier.none");
        };
        tooltip.add(armorComponent);

        tooltip.add(Component.translatable("tooltip.vuties.gauntlet.controls").withStyle(ChatFormatting.GOLD));

        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.gauntlet.idle"));
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private GauntletItemRenderer itemRenderer;

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (this.itemRenderer == null) {
                    this.itemRenderer = new GauntletItemRenderer();
                }
                return this.itemRenderer;
            }
        });
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(GauntletClientExtensions.INSTANCE);
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity, InteractionHand hand) {
        // When the drill is active, cancel swing animation completely so it stays static during mining / using
        if (isDrillActive(stack)) {
            return true;
        }
        return false;
    }



    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        if (slotChanged) return true;
        return oldStack.getItem() != newStack.getItem();
    }
}