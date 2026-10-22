package com.vibe.vuties.network;

import com.vibe.vuties.Vuties;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RotorpackTogglePayload(int action) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RotorpackTogglePayload> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Vuties.MODID, "rotorpack_toggle"));

    public static final StreamCodec<ByteBuf, RotorpackTogglePayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, RotorpackTogglePayload::action,
        RotorpackTogglePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
