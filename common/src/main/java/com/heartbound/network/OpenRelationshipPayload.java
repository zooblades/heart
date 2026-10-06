package com.heartbound.network;

import com.heartbound.menu.ModMenus;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: "open the relationship window for this entity". */
public record OpenRelationshipPayload(int entityId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenRelationshipPayload> TYPE =
            new CustomPacketPayload.Type<>(ModMenus.id("open_relationship"));

    public static final StreamCodec<ByteBuf, OpenRelationshipPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, OpenRelationshipPayload::entityId,
            OpenRelationshipPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
