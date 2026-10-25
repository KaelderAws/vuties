package com.vibe.vuties.network;

import com.vibe.vuties.Vuties;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record HelmetTogglePayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<HelmetTogglePayload> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Vuties.MODID, "helmet_toggle"));

    public static final StreamCodec<ByteBuf, HelmetTogglePayload> STREAM_CODEC = StreamCodec.unit(new HelmetTogglePayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
