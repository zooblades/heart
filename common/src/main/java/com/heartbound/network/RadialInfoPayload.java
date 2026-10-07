package com.heartbound.network;

import com.heartbound.menu.ModMenus;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server to client: what the gesture menu needs to know (affinity and partner flag). */
public record RadialInfoPayload(int entityId, int affinity, boolean partner) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RadialInfoPayload> TYPE =
            new CustomPacketPayload.Type<>(ModMenus.id("radial_info"));

    public static final StreamCodec<ByteBuf, RadialInfoPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RadialInfoPayload::entityId,
            ByteBufCodecs.VAR_INT, RadialInfoPayload::affinity,
            ByteBufCodecs.BOOL, RadialInfoPayload::partner,
            RadialInfoPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
