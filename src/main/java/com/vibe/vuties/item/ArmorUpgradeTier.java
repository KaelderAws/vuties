package com.vibe.vuties.item;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

public enum ArmorUpgradeTier implements StringRepresentable {
    NONE("none", 2, 0.0F, 0.0F),
    IRON("iron", 6, 0.0F, 0.0F),
    DIAMOND("diamond", 8, 2.0F, 0.0F),
    NETHERITE("netherite", 8, 3.0F, 0.1F);

    public static final Codec<ArmorUpgradeTier> CODEC = StringRepresentable.fromEnum(ArmorUpgradeTier::values);
    public static final StreamCodec<ByteBuf, ArmorUpgradeTier> STREAM_CODEC = ByteBufCodecs.idMapper(
        byId -> values()[byId],
        ArmorUpgradeTier::ordinal
    );

    private final String name;
    private final int armor;
    private final float toughness;
    private final float knockbackResistance;

    ArmorUpgradeTier(String name, int armor, float toughness, float knockbackResistance) {
        this.name = name;
        this.armor = armor;
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

    public float getToughness() {
        return this.toughness;
    }

    public float getKnockbackResistance() {
        return this.knockbackResistance;
    }
}
