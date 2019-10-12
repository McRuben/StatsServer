package de.derrop.labymod.addons.server;
/*
 * Created by derrop on 29.09.2019
 */

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.derrop.labymod.addons.server.command.CommandMap;
import de.derrop.labymod.addons.server.command.console.commands.*;
import de.derrop.labymod.addons.server.config.GeneralConfiguration;
import de.derrop.labymod.addons.server.database.DatabaseProvider;
import de.derrop.labymod.addons.server.database.TagType;
import de.derrop.labymod.addons.server.discord.DiscordBot;
import de.derrop.labymod.addons.server.sync.SyncPlayer;
import de.derrop.labymod.addons.server.sync.SyncServer;
import de.derrop.labymod.addons.server.sync.handler.*;
import de.derrop.labymod.addons.server.textures.MinecraftTextureManager;
import io.javalin.Javalin;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;

public class GommeStatsServer {

    private final Gson gson = new Gson();

    private Javalin webServer;
    private SyncServer syncServer = new SyncServer(this);
    private DatabaseProvider databaseProvider = new DatabaseProvider();

    private Map<String, Match> runningMatches = new HashMap<>();

    private CommandMap commandMap = new CommandMap();

    private DiscordBot discordBot = new DiscordBot(this);

    private GeneralConfiguration configuration;

    private MinecraftTextureManager textureManager = new MinecraftTextureManager();

    public static void main(String[] args) {
        GommeStatsServer statsServer = new GommeStatsServer();

        GeneralConfiguration configuration = new GeneralConfiguration();

        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        Path path = Paths.get("config.json");
        if (Files.exists(path)) {
            try (Reader reader = new InputStreamReader(Files.newInputStream(path))) {
                configuration = gson.fromJson(reader, GeneralConfiguration.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try (Writer writer = new OutputStreamWriter(Files.newOutputStream(path))) {
                gson.toJson(configuration, writer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        statsServer.init(configuration);
    }

    public void init(GeneralConfiguration configuration) {
        this.configuration = configuration;

        this.databaseProvider.init();

        this.commandMap.registerCommand(new CommandHelp(this.commandMap));

        this.commandMap.registerCommand(new CommandStats(this));
        this.commandMap.registerCommand(new CommandMatch(this));
        this.commandMap.registerCommand(new CommandUser(this));

        this.commandMap.registerCommand(new CommandTag(this, TagType.CLAN, "ctag", "clantag", "clan-tag"));
        this.commandMap.registerCommand(new CommandTag(this, TagType.PLAYER, "utag", "usertag", "user-tag"));

        this.initWeb();
        this.initStatsServer();

        this.discordBot.init(configuration.getDiscordConfiguration());
    }

    public void shutdown() {
        this.discordBot.close();
        this.webServer.stop();
        this.syncServer.close();
    }

    private void initStatsServer() {
        this.syncServer.init(new InetSocketAddress(this.configuration.getMinecraftPort()));

        this.syncServer.registerHandler((short) 1, new MatchBeginHandler(this));
        this.syncServer.registerHandler((short) 2, new MatchEndHandler(this));
        this.syncServer.registerHandler((short) 3, new MatchPlayerRemoveHandler());
        this.syncServer.registerHandler((short) 4, new PlayerStatisticsUpdateHandler(this));
        this.syncServer.registerHandler((short) 5, new TagHandler(this));
    }

    private void initWeb() {
        this.webServer = Javalin.create().start(this.configuration.getWebPort());
        this.webServer.routes(() -> {
            path("/api", () -> {
                path("/matches", () -> {
                    get("/list", context -> context.result(this.gson.toJson(this.databaseProvider.getMatchProvider().getMatches())));
                    get("/list/:gamemode", context -> context.result(this.gson.toJson(this.databaseProvider.getMatchProvider().getMatches(context.pathParam("gamemode")))));
                    get("/list/limit/:limit", context -> {
                        try {
                            context.result(this.gson.toJson(this.databaseProvider.getMatchProvider().getLastMatches(Integer.parseInt(context.pathParam("limit")))));
                        } catch (NumberFormatException exception) {
                            context.result("null");
                        }
                    });
                    get("/count", context -> context.result(this.gson.toJson(this.databaseProvider.getMatchProvider().countMatches())));
                    get("/running", context -> context.result(this.gson.toJson(this.runningMatches.values())));
                });
                path("/statistics", () -> {
                    get("/list", context -> context.result(this.gson.toJson(this.databaseProvider.getStatisticsProvider().getStatistics())));
                    get("/list/:gamemode", context -> context.result(this.gson.toJson(this.databaseProvider.getStatisticsProvider().getStatisticsOfGameMode(context.pathParam("gamemode")))));
                    get("/list/limit/:limit", context -> context.result(this.gson.toJson(this.databaseProvider.getStatisticsProvider().getStatistics(Integer.parseInt(context.pathParam("limit"))))));
                    get("/count", context -> context.result(String.valueOf(this.databaseProvider.getStatisticsProvider().countAvailableStatistics())));
                    get("/player/:name", context -> context.result(this.gson.toJson(this.databaseProvider.getStatisticsProvider().getStatistics(context.pathParam("name")))));
                });
                path("/textures", () -> {
                    get("/:texture", context -> {
                        byte[] image = this.textureManager.getMinecraftTexture(context.pathParam("texture").replace(".", "/"));
                        if (image != null) {
                            context.contentType("image/png").result(new ByteArrayInputStream(image));
                        } else {
                            context.status(404).result("Not found");
                        }
                    });
                });
            });
        });
    }

    public CommandMap getCommandMap() {
        return commandMap;
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
        this.databaseProvider.getMatchProvider().insertMatch(match);
    }

    public void startMatch(SyncPlayer player, String gamemode, String map, String serverId, String minecraftTexturePath, Collection<String> players) {
        Match match;
        if (this.runningMatches.containsKey(serverId)) {
            match = this.runningMatches.get(serverId);
            match.getEndPlayers().addAll(players);
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
                    new ArrayList<>(players),
                    minecraftTexturePath,
                    new HashSet<>(Collections.singletonList(player.getName()))
            );
            this.runningMatches.put(serverId, match);

            for (String name : players) {
                PlayerData playerData = this.databaseProvider.getStatisticsProvider().getStatistics(name, gamemode);
                if (playerData != null) {
                    playerData.setLastMatchId(match.getServerId());
                }
            }

            this.discordBot.handleMatchBegin(match);
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
                this.databaseProvider.getMatchProvider().insertMatch(match);

                this.discordBot.handleMatchEnd(match);
            }
        }
    }

}
