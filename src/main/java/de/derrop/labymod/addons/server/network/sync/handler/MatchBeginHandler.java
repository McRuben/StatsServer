package de.derrop.labymod.addons.server.network.sync.handler;
/*
 * Created by derrop on 30.09.2019
 */

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import de.derrop.labymod.addons.server.GommeStatsServer;
import de.derrop.labymod.addons.server.network.sync.PacketHandler;
import de.derrop.labymod.addons.server.network.sync.SyncPlayer;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.function.Consumer;

public class MatchBeginHandler implements PacketHandler {
    private static final Type STRING_COLLECTION_TYPE = new TypeToken<Collection<String>>() {
    }.getType();

    private final Gson gson = new Gson();

    private GommeStatsServer statsServer;

    public MatchBeginHandler(GommeStatsServer statsServer) {
        this.statsServer = statsServer;
    }

    @Override
    public void handlePacket(SyncPlayer player, JsonElement payload, Consumer<JsonElement> responseConsumer) {
        String serverId = payload.getAsJsonObject().get("serverId").getAsString();
        String serverType = payload.getAsJsonObject().get("serverType").getAsString();
        String map = payload.getAsJsonObject().get("map").getAsString();
        Collection<String> players = this.gson.fromJson(payload.getAsJsonObject().get("players"), STRING_COLLECTION_TYPE);
        String minecraftTexturePath = payload.getAsJsonObject().get("texturePath").getAsString();

        this.statsServer.startMatch(player, serverType, map, serverId, minecraftTexturePath, players);
    }
}
