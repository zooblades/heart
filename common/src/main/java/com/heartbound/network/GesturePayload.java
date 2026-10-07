package com.heartbound.network;

import com.heartbound.menu.ModMenus;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: "perform this gesture (Gesture ordinal) with this entity". */
public record GesturePayload(int entityId, int gesture) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<GesturePayload> TYPE =
            new CustomPacketPayload.Type<>(ModMenus.id("gesture"));

    public static final StreamCodec<ByteBuf, GesturePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, GesturePayload::entityId,
            ByteBufCodecs.VAR_INT, GesturePayload::gesture,
            GesturePayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
