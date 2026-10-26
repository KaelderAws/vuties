package com.vibe.vuties.network;

import com.vibe.vuties.Vuties;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PhaseShiftPayload(int action, double vx, double vy, double vz) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PhaseShiftPayload> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Vuties.MODID, "phase_shift"));

    public static final StreamCodec<ByteBuf, PhaseShiftPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, PhaseShiftPayload::action,
        ByteBufCodecs.DOUBLE, PhaseShiftPayload::vx,
        ByteBufCodecs.DOUBLE, PhaseShiftPayload::vy,
        ByteBufCodecs.DOUBLE, PhaseShiftPayload::vz,
        PhaseShiftPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
