package com.vibe.vuties.network;

import com.vibe.vuties.Vuties;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SlidePayload(boolean sliding) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SlidePayload> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Vuties.MODID, "slide_state"));

    public static final StreamCodec<ByteBuf, SlidePayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL, SlidePayload::sliding,
        SlidePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
