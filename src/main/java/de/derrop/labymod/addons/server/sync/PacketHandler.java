package de.derrop.labymod.addons.server.sync;
/*
 * Created by derrop on 30.09.2019
 */

import com.google.gson.JsonElement;

import java.util.function.Consumer;

public interface PacketHandler {
    void handlePacket(SyncPlayer player, JsonElement payload, Consumer<JsonElement> responseConsumer);
}
