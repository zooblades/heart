package com.heartbound.network;

import com.heartbound.menu.ModMenus;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: "I want the gesture menu for this entity". */
public record OpenRadialPayload(int entityId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenRadialPayload> TYPE =
            new CustomPacketPayload.Type<>(ModMenus.id("open_radial"));

    public static final StreamCodec<ByteBuf, OpenRadialPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, OpenRadialPayload::entityId,
            OpenRadialPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
