package de.derrop.labymod.addons.server;
/*
 * Created by derrop on 29.09.2019
 */

import com.google.gson.Gson;
import de.derrop.labymod.addons.server.database.DatabaseProvider;
import de.derrop.labymod.addons.server.sync.SyncPlayer;
import de.derrop.labymod.addons.server.sync.SyncServer;
import de.derrop.labymod.addons.server.sync.handler.MatchBeginHandler;
import de.derrop.labymod.addons.server.sync.handler.MatchEndHandler;
import de.derrop.labymod.addons.server.sync.handler.MatchPlayerRemoveHandler;
import de.derrop.labymod.addons.server.sync.handler.PlayerStatisticsUpdateHandler;
import io.javalin.Javalin;

import java.net.InetSocketAddress;
import java.util.*;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;

public class GommeStatsServer {

    private final Gson gson = new Gson();

    private Javalin webServer;
    private SyncServer syncServer = new SyncServer(this);
    private DatabaseProvider databaseProvider = new DatabaseProvider();

    private Map<String, Match> runningMatches = new HashMap<>();

    public static void main(String[] args) {
        GommeStatsServer statsServer = new GommeStatsServer();
        statsServer.init();
    }

    public void init() {
        this.databaseProvider.init();

        this.initWeb();
        this.initStatsServer();
    }

    private void initStatsServer() {
        this.syncServer.init(new InetSocketAddress("192.168.178.47", 1510)); //todo config

        this.syncServer.registerHandler((short) 1, new MatchBeginHandler(this));
        this.syncServer.registerHandler((short) 2, new MatchEndHandler(this));
        this.syncServer.registerHandler((short) 3, new MatchPlayerRemoveHandler());
        this.syncServer.registerHandler((short) 4, new PlayerStatisticsUpdateHandler(this));
    }

    private void initWeb() {
        this.webServer = Javalin.create().start(2410); //todo config
        this.webServer.routes(() -> {
            path("/api", () -> {
                path("/matches", () -> {
                    get("/list", context -> context.result(this.gson.toJson(this.databaseProvider.getMatches())));
                    get("/list/:gamemode", context -> context.result(this.gson.toJson(this.databaseProvider.getMatches(context.pathParam("gamemode")))));
                    get("/list/limit/:limit", context -> {
                        try {
                            context.result(this.gson.toJson(this.databaseProvider.getLastMatches(Integer.parseInt(context.pathParam("limit")))));
                        } catch (NumberFormatException exception) {
                            context.result("null");
                        }
                    });
                    get("/count", context -> context.result(this.gson.toJson(this.databaseProvider.countMatches())));
                    get("/running", context -> context.result(this.gson.toJson(this.runningMatches.values())));
                });
                path("/statistics", () -> {
                    get("/list", context -> context.result(this.gson.toJson(this.databaseProvider.getStatistics())));
                    get("/list/:gamemode", context -> context.result(this.gson.toJson(this.databaseProvider.getStatisticsOfGameMode(context.pathParam("gamemode")))));
                    get("/list/limit/:limit", context -> context.result(this.gson.toJson(this.databaseProvider.getStatistics(Integer.parseInt(context.pathParam("limit"))))));
                    get("/count", context -> context.result(String.valueOf(this.databaseProvider.countAvailableStatistics())));
                    get("/player/:name", context -> context.result(this.gson.toJson(this.databaseProvider.getStatistics(context.pathParam("name")))));
                });
            });
        });
    }

    public Gson getGson() {
        return gson;
    }

    public DatabaseProvider getDatabaseProvider() {
        return databaseProvider;
    }

    public Map<String, Match> getRunningMatches() {
        return runningMatches;
    }

    public void handleDisconnect(String player, String serverId) {
        if (this.runningMatches.containsKey(serverId)) {
            Match match = this.runningMatches.get(serverId);
            if (match.getTemporaryPlayers().remove(player) && match.getTemporaryPlayers().isEmpty()) {
                this.endMatchNotFinished(match);
            }
        }
    }

    public void endMatchNotFinished(Match match) {
        match.setFinished(false);
        match.setEndTimestamp(System.currentTimeMillis());
        this.runningMatches.remove(match.getServerId());
        this.databaseProvider.insertMatch(match);
    }

    public void startMatch(SyncPlayer player, String gamemode, String map, String serverId, Collection<String> players) {
        Match match;
        if (this.runningMatches.containsKey(serverId)) {
            match = this.runningMatches.get(serverId);
            match.getPlayers().addAll(players);
            match.getTemporaryPlayers().addAll(players);
        } else {
            match = new Match(
                    map,
                    gamemode,
                    serverId,
                    System.currentTimeMillis(),
                    -1,
                    false,
                    null,
                    players,
                    new HashSet<>(Collections.singletonList(player.getName()))
            );
            this.runningMatches.put(serverId, match);

            for (String name : players) {
                PlayerData playerData = this.databaseProvider.getStatistics(name, gamemode);
                if (playerData != null) {
                    playerData.setLastMatchId(match.getServerId());
                }
            }
        }
        player.setCurrentMatch(match);
    }

    public void endMatch(String player, Collection<String> winners, String serverId) {
        Match match = this.runningMatches.get(serverId);
        if (match == null)
            return;
        if (match.getTemporaryPlayers().remove(player)) {
            if (match.getTemporaryPlayers().isEmpty()) {
                if (winners != null) {
                    match.setWinners(winners);
                    match.setFinished(true);
                }
                match.setEndTimestamp(System.currentTimeMillis());
                this.runningMatches.remove(serverId);
                this.databaseProvider.insertMatch(match);
            }
        }
    }

}
