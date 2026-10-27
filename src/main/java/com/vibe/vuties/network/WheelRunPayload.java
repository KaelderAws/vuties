package com.vibe.vuties.network;

import com.vibe.vuties.Vuties;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record WheelRunPayload(boolean running, boolean sprinting) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<WheelRunPayload> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Vuties.MODID, "wheel_run"));

    public static final StreamCodec<ByteBuf, WheelRunPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL, WheelRunPayload::running,
        ByteBufCodecs.BOOL, WheelRunPayload::sprinting,
        WheelRunPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
