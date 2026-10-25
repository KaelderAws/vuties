package com.vibe.vuties.network;

import com.vibe.vuties.Vuties;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record NightVisionTogglePayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<NightVisionTogglePayload> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Vuties.MODID, "night_vision_toggle"));

    public static final StreamCodec<ByteBuf, NightVisionTogglePayload> STREAM_CODEC =
        StreamCodec.unit(new NightVisionTogglePayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
