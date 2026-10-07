package com.heartbound.network;

import com.heartbound.menu.ModMenus;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: "talk to this entity about this topic (Topic ordinal)". */
public record TalkPayload(int entityId, int topic) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<TalkPayload> TYPE =
            new CustomPacketPayload.Type<>(ModMenus.id("talk"));

    public static final StreamCodec<ByteBuf, TalkPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TalkPayload::entityId,
            ByteBufCodecs.VAR_INT, TalkPayload::topic,
            TalkPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
