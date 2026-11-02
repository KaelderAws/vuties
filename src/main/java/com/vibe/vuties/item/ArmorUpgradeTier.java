package com.vibe.vuties.item;

import com.mojang.serialization.Codec;
import com.vibe.vuties.init.ModDataComponents;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

public enum ArmorUpgradeTier implements StringRepresentable {
    NONE("none", 2, 1, 2, 1, 0.0F, 0.0F),
    IRON("iron", 6, 2, 5, 2, 0.0F, 0.0F),
    DIAMOND("diamond", 8, 3, 6, 3, 2.0F, 0.0F),
    NETHERITE("netherite", 8, 3, 6, 3, 3.0F, 0.1F);

    public static final Codec<ArmorUpgradeTier> CODEC = StringRepresentable.fromEnum(ArmorUpgradeTier::values);
    public static final StreamCodec<ByteBuf, ArmorUpgradeTier> STREAM_CODEC = ByteBufCodecs.idMapper(
        byId -> values()[byId],
        ArmorUpgradeTier::ordinal
    );

    private final String name;
    private final int armor;
    private final int helmetArmor;
    private final int leggingsArmor;
    private final int bootsArmor;
    private final float toughness;
    private final float knockbackResistance;

    ArmorUpgradeTier(String name, int armor, int helmetArmor, int leggingsArmor, int bootsArmor, float toughness, float knockbackResistance) {
        this.name = name;
        this.armor = armor;
        this.helmetArmor = helmetArmor;
        this.leggingsArmor = leggingsArmor;
        this.bootsArmor = bootsArmor;
        this.toughness = toughness;
        this.knockbackResistance = knockbackResistance;
    }

    public static ArmorUpgradeTier get(ItemStack stack) {
        return stack != null ? stack.getOrDefault(ModDataComponents.UPGRADE_ARMOR.get(), ArmorUpgradeTier.NONE) : ArmorUpgradeTier.NONE;
    }

    public static void set(ItemStack stack, ArmorUpgradeTier tier) {
        if (stack != null) {
            stack.set(ModDataComponents.UPGRADE_ARMOR.get(), tier);
        }
    }

    public ItemAttributeModifiers createModifiers(ResourceLocation baseId, EquipmentSlotGroup slotGroup, int armorValue) {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
        if (armorValue > 0) {
            builder.add(Attributes.ARMOR,
                    new AttributeModifier(ResourceLocation.fromNamespaceAndPath(baseId.getNamespace(), baseId.getPath() + "_armor"), armorValue, AttributeModifier.Operation.ADD_VALUE),
                    slotGroup);
        }
        if (this.toughness > 0.0F) {
            builder.add(Attributes.ARMOR_TOUGHNESS,
                    new AttributeModifier(ResourceLocation.fromNamespaceAndPath(baseId.getNamespace(), baseId.getPath() + "_armor_toughness"), this.toughness, AttributeModifier.Operation.ADD_VALUE),
                    slotGroup);
        }
        if (this.knockbackResistance > 0.0F) {
            builder.add(Attributes.KNOCKBACK_RESISTANCE,
                    new AttributeModifier(ResourceLocation.fromNamespaceAndPath(baseId.getNamespace(), baseId.getPath() + "_armor_knockback_resistance"), this.knockbackResistance, AttributeModifier.Operation.ADD_VALUE),
                    slotGroup);
        }
        return builder.build();
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public int getArmor() {
        return this.armor;
    }

    public int getHelmetArmor() {
        return this.helmetArmor;
    }

    public int getLeggingsArmor() {
        return this.leggingsArmor;
    }

    public int getBootsArmor() {
        return this.bootsArmor;
    }

    public int getGauntletArmor() {
        return switch (this) {
            case IRON -> 1;
            case DIAMOND -> 2;
            case NETHERITE -> 3;
            default -> 0;
        };
    }

    public int getArmor(ArmorItem.Type type) {
        if (type == ArmorItem.Type.HELMET) {
            return this.helmetArmor;
        } else if (type == ArmorItem.Type.LEGGINGS) {
            return this.leggingsArmor;
        } else if (type == ArmorItem.Type.BOOTS) {
            return this.bootsArmor;
        }
        return this.armor;
    }

    public float getToughness() {
        return this.toughness;
    }

    public float getKnockbackResistance() {
        return this.knockbackResistance;
    }
}
