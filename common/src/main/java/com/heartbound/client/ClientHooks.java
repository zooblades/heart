package com.heartbound.client;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.function.Consumer;

/** Lets the common client code send packets; each loader sets the real sender on client start. */
public final class ClientHooks {

    public static Consumer<CustomPacketPayload> sender = payload -> { };

    private ClientHooks() {
    }

    public static void send(CustomPacketPayload payload) {
        sender.accept(payload);
    }
}
