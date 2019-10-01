package de.derrop.labymod.addons.server.sync.handler;
/*
 * Created by derrop on 01.10.2019
 */

import com.google.gson.JsonElement;
import de.derrop.labymod.addons.server.GommeStatsServer;
import de.derrop.labymod.addons.server.sync.PacketHandler;
import de.derrop.labymod.addons.server.sync.SyncPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;

public class MatchEndHandler implements PacketHandler {

    private GommeStatsServer statsServer;

    public MatchEndHandler(GommeStatsServer statsServer) {
        this.statsServer = statsServer;
    }

    @Override
    public void handlePacket(SyncPlayer player, JsonElement payload, Consumer<JsonElement> responseConsumer) {
        if (player.getCurrentMatch() == null) {
            return;
        }
        Collection<String> winners = null;
        if (payload.isJsonObject() && payload.getAsJsonObject().has("winners")) {
            winners = new ArrayList<>();
            for (JsonElement element : payload.getAsJsonObject().get("winners").getAsJsonArray()) {
                winners.add(element.getAsString());
            }
        }
        this.statsServer.endMatch(player.getName(), winners, player.getCurrentMatch().getServerId());
    }
}
