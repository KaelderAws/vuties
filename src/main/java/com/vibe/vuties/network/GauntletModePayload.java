package com.vibe.vuties.network;

import com.vibe.vuties.Vuties;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record GauntletModePayload(int handSlot, int mode) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<GauntletModePayload> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Vuties.MODID, "gauntlet_mode"));

    public static final StreamCodec<ByteBuf, GauntletModePayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, GauntletModePayload::handSlot,
        ByteBufCodecs.VAR_INT, GauntletModePayload::mode,
        GauntletModePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
