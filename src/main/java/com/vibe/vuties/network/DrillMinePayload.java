package com.vibe.vuties.network;

import com.vibe.vuties.Vuties;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DrillMinePayload(BlockPos pos) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<DrillMinePayload> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Vuties.MODID, "drill_mine"));

    public static final StreamCodec<ByteBuf, DrillMinePayload> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, DrillMinePayload::pos,
        DrillMinePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
