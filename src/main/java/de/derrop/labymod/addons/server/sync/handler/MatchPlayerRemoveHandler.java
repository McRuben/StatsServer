package de.derrop.labymod.addons.server.sync.handler;
/*
 * Created by derrop on 01.10.2019
 */

import com.google.gson.JsonElement;
import de.derrop.labymod.addons.server.sync.PacketHandler;
import de.derrop.labymod.addons.server.sync.SyncPlayer;

import java.util.function.Consumer;

public class MatchPlayerRemoveHandler implements PacketHandler {
    @Override
    public void handlePacket(SyncPlayer player, JsonElement payload, Consumer<JsonElement> responseConsumer) {
        if (player.getCurrentMatch() == null)
            return;

        player.getCurrentMatch().getEndPlayers().remove(payload.getAsString());
    }
}
