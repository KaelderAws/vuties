package com.vibe.vuties.init;

import com.mojang.serialization.Codec;
import com.vibe.vuties.Vuties;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
        DeferredRegister.createDataComponents(net.minecraft.core.registries.Registries.DATA_COMPONENT_TYPE, Vuties.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> ENGINE_ENABLED =
        DATA_COMPONENTS.registerComponentType("engine_enabled", builder ->
            builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> HOVER_MODE =
        DATA_COMPONENTS.registerComponentType("hover_mode", builder ->
            builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));
}
