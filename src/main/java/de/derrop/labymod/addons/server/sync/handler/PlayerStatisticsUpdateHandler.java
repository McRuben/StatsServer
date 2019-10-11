package de.derrop.labymod.addons.server.sync.handler;
/*
 * Created by derrop on 01.10.2019
 */

import com.google.gson.JsonElement;
import de.derrop.labymod.addons.server.GommeStatsServer;
import de.derrop.labymod.addons.server.PlayerData;
import de.derrop.labymod.addons.server.sync.PacketHandler;
import de.derrop.labymod.addons.server.sync.SyncPlayer;

import java.util.function.Consumer;

public class PlayerStatisticsUpdateHandler implements PacketHandler {

    private GommeStatsServer statsServer;

    public PlayerStatisticsUpdateHandler(GommeStatsServer statsServer) {
        this.statsServer = statsServer;
    }

    @Override
    public void handlePacket(SyncPlayer player, JsonElement payload, Consumer<JsonElement> responseConsumer) {
        PlayerData newPlayerData = this.statsServer.getGson().fromJson(payload, PlayerData.class);
        if (newPlayerData != null) {
            newPlayerData.setTimestamp(System.currentTimeMillis());
            PlayerData oldPlayerData = this.statsServer.getDatabaseProvider().getStatisticsProvider().getStatistics(newPlayerData.getName(), newPlayerData.getGamemode());
            if (oldPlayerData != null) {
                newPlayerData.setLastMatchId(oldPlayerData.getLastMatchId());
            }
            this.statsServer.getDatabaseProvider().getStatisticsProvider().updateStatistics(newPlayerData);
        }
    }
}
