package com.vibe.vuties.item;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ArmorItem;

public enum ArmorUpgradeTier implements StringRepresentable {
    NONE("none", 2, 1, 0.0F, 0.0F),
    IRON("iron", 6, 2, 0.0F, 0.0F),
    DIAMOND("diamond", 8, 3, 2.0F, 0.0F),
    NETHERITE("netherite", 8, 3, 3.0F, 0.1F);

    public static final Codec<ArmorUpgradeTier> CODEC = StringRepresentable.fromEnum(ArmorUpgradeTier::values);
    public static final StreamCodec<ByteBuf, ArmorUpgradeTier> STREAM_CODEC = ByteBufCodecs.idMapper(
        byId -> values()[byId],
        ArmorUpgradeTier::ordinal
    );

    private final String name;
    private final int armor;
    private final int helmetArmor;
    private final float toughness;
    private final float knockbackResistance;

    ArmorUpgradeTier(String name, int armor, int helmetArmor, float toughness, float knockbackResistance) {
        this.name = name;
        this.armor = armor;
        this.helmetArmor = helmetArmor;
        this.toughness = toughness;
        this.knockbackResistance = knockbackResistance;
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

    public int getArmor(ArmorItem.Type type) {
        return type == ArmorItem.Type.HELMET ? this.helmetArmor : this.armor;
    }

    public float getToughness() {
        return this.toughness;
    }

    public float getKnockbackResistance() {
        return this.knockbackResistance;
    }
}
