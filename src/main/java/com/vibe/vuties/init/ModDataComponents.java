package com.vibe.vuties.init;

import com.mojang.serialization.Codec;
import com.vibe.vuties.Vuties;
import com.vibe.vuties.item.ArmorUpgradeTier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
        DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Vuties.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> ENGINE_ENABLED =
        DATA_COMPONENTS.registerComponentType("engine_enabled", builder ->
            builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> HOVER_MODE =
        DATA_COMPONENTS.registerComponentType("hover_mode", builder ->
            builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> UPGRADE_HOVER =
        DATA_COMPONENTS.registerComponentType("upgrade_hover", builder ->
            builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> UPGRADE_WINGS =
        DATA_COMPONENTS.registerComponentType("upgrade_wings", builder ->
            builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ArmorUpgradeTier>> UPGRADE_ARMOR =
        DATA_COMPONENTS.registerComponentType("upgrade_armor", builder ->
            builder.persistent(ArmorUpgradeTier.CODEC).networkSynchronized(ArmorUpgradeTier.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> HELMET_OPEN =
        DATA_COMPONENTS.registerComponentType("helmet_open", builder ->
            builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> UPGRADE_WATER_BREATHING =
        DATA_COMPONENTS.registerComponentType("upgrade_water_breathing", builder ->
            builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> UPGRADE_NIGHT_VISION =
        DATA_COMPONENTS.registerComponentType("upgrade_night_vision", builder ->
            builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> NIGHT_VISION_ENABLED =
        DATA_COMPONENTS.registerComponentType("night_vision_enabled", builder ->
            builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> UPGRADE_PHASE =
        DATA_COMPONENTS.registerComponentType("upgrade_phase", builder ->
            builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));
}