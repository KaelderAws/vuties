package com.vibe.vuties.network;

import com.vibe.vuties.Vuties;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RotorpackInputPayload(boolean jumping, boolean sneaking, boolean forward, boolean sprinting) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RotorpackInputPayload> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Vuties.MODID, "rotorpack_input"));

    public static final StreamCodec<ByteBuf, RotorpackInputPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL, RotorpackInputPayload::jumping,
        ByteBufCodecs.BOOL, RotorpackInputPayload::sneaking,
        ByteBufCodecs.BOOL, RotorpackInputPayload::forward,
        ByteBufCodecs.BOOL, RotorpackInputPayload::sprinting,
        RotorpackInputPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
