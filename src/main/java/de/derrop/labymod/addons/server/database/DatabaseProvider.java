package de.derrop.labymod.addons.server.database;
/*
 * Created by derrop on 01.10.2019
 */

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import de.derrop.labymod.addons.server.Match;
import de.derrop.labymod.addons.server.PlayerData;
import de.derrop.labymod.addons.server.function.ThrowingFunction;
import io.netty.buffer.ByteBuf;

import java.io.File;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class DatabaseProvider {

    private static final Type STRING_MAP_TYPE = new TypeToken<Map<String, String>>() {
    }.getType();
    private static final Type STRING_COLLECTION_TYPE = new TypeToken<Collection<String>>() {
    }.getType();

    private final Gson gson = new Gson();
    private long validStatsMillis = TimeUnit.MINUTES.toMillis(5);
    private Connection databaseConnection;

    public void init() {
        try {
            Class.forName("org.h2.Driver");
            this.databaseConnection = DriverManager.getConnection("jdbc:h2:" + new File("database/h2").getAbsolutePath());

            this.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS stats(uniqueId BINARY(16), rank LONG, name VARCHAR(64) NOT NULL, gamemode VARCHAR(32) NOT NULL, stats TEXT, timestamp LONG, lastMatchId VARCHAR(36))",
                    PreparedStatement::executeUpdate
            );
            this.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS matches(matchId VARCHAR(36), gamemode VARCHAR(32), map VARCHAR(32), beginTimestamp LONG, endTimestamp LONG, finished BOOL, winners TEXT, beginPlayers TEXT, endPlayers TEXT)",
                    PreparedStatement::executeUpdate
            );
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private <T> T prepareStatement(String sql, ThrowingFunction<PreparedStatement, T, SQLException> function) {
        return this.prepareStatement(sql, function, null);
    }

    private <T> T prepareStatement(String sql, ThrowingFunction<PreparedStatement, T, SQLException> function, T def) {
        try {
            try (PreparedStatement statement = this.databaseConnection.prepareStatement(sql)) {
                return function.apply(statement);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return def;
    }

    public Collection<PlayerData> getStatistics(String name) {
        return this.prepareStatement(
                "SELECT * FROM stats WHERE name = ?",
                preparedStatement -> {
                    preparedStatement.setString(1, name);
                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        Collection<PlayerData> data = new ArrayList<>();
                        while (resultSet.next()) {
                            data.add(this.asPlayerData(resultSet));
                        }
                        return data;
                    }
                }
        );
    }

    public PlayerData getStatistics(String name, String gamemode) {
        return this.prepareStatement(
                "SELECT * FROM stats WHERE name = ? AND gamemode = ?",
                preparedStatement -> {
                    preparedStatement.setString(1, name);
                    preparedStatement.setString(2, gamemode);
                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        if (resultSet.next()) {
                            return this.asPlayerData(resultSet);
                        }
                    }
                    return null;
                }
        );
    }

    public PlayerData getBestStatistics(String gamemode) {
        return this.prepareStatement(
                "SELECT * FROM stats WHERE gamemode = ? ORDER BY rank LIMIT 1",
                preparedStatement -> {
                    preparedStatement.setString(1, gamemode);
                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        if (resultSet.next()) {
                            return this.asPlayerData(resultSet);
                        }
                    }
                    return null;
                }
        );
    }

    public PlayerData getWorstStatistics(String gamemode) {
        return this.prepareStatement(
                "SELECT * FROM stats WHERE gamemode = ? ORDER BY rank DESC LIMIT 1",
                preparedStatement -> {
                    preparedStatement.setString(1, gamemode);
                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        if (resultSet.next()) {
                            return this.asPlayerData(resultSet);
                        }
                    }
                    return null;
                }
        );
    }

    public Collection<PlayerData> getStatistics() {
        return this.getStatistics(-1);
    }

    public Collection<PlayerData> getStatistics(int maxCount) {
        return this.prepareStatement(
                maxCount > -1 ? "SELECT * FROM stats ORDER BY rank LIMIT " + maxCount : "SELECT * FROM stats",
                preparedStatement -> {
                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        Collection<PlayerData> data = new ArrayList<>();
                        while (resultSet.next()) {
                            data.add(this.asPlayerData(resultSet));
                        }
                        return data;
                    }
                }
        );
    }

    public Collection<PlayerData> getStatisticsOfGameMode(String gamemode) {
        return this.getStatisticsOfGameMode(gamemode, -1);
    }

    public Collection<PlayerData> getStatisticsOfGameMode(String gamemode, int maxCount) {
        return this.prepareStatement(
                maxCount > -1 ? "SELECT * FROM stats WHERE gamemode = ? LIMIT " + maxCount : "SELECT * FROM stats WHERE gamemode = ?",
                preparedStatement -> {
                    preparedStatement.setString(1, gamemode);
                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        Collection<PlayerData> data = new ArrayList<>();
                        while (resultSet.next()) {
                            data.add(this.asPlayerData(resultSet));
                        }
                        return data;
                    }
                }
        );
    }

    public Collection<PlayerData> getValidStatistics() {
        return this.prepareStatement(
                "SELECT * FROM stats",
                preparedStatement -> {
                    Collection<PlayerData> statistics = new ArrayList<>();
                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        while (resultSet.next()) {
                            long timestamp = resultSet.getLong("timestamp");
                            if (this.isValidStatistics(timestamp)) {
                                statistics.add(this.asPlayerData(resultSet));
                            }
                        }
                    }
                    return statistics;
                }
        );
    }

    public void updateStatistics(PlayerData statistics) {
        if (statistics.getRank() <= 0)
            return;

        boolean exists = this.prepareStatement(
                "SELECT * FROM stats WHERE name = ? AND gamemode = ?",
                preparedStatement -> {
                    preparedStatement.setString(1, statistics.getName());
                    preparedStatement.setString(2, statistics.getGamemode());
                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        return resultSet.next();
                    }
                }
        );
        if (exists) {
            this.prepareStatement(
                    "UPDATE stats SET timestamp = ?, stats = ?, lastMatchId = ?, rank = ? WHERE name = ? AND gamemode = ?",
                    preparedStatement -> {
                        preparedStatement.setLong(1, statistics.getTimestamp());
                        preparedStatement.setString(2, this.gson.toJson(statistics.getStats()));
                        preparedStatement.setString(3, statistics.getLastMatchId());
                        preparedStatement.setLong(4, statistics.getRank());
                        preparedStatement.setString(5, statistics.getName());
                        preparedStatement.setString(6, statistics.getGamemode());

                        return preparedStatement.executeUpdate();
                    }
            );
        } else {
            this.prepareStatement(
                    "INSERT INTO stats (uniqueId, rank, name, gamemode, stats, timestamp, lastMatchId) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    preparedStatement -> {
                        preparedStatement.setBytes(1, statistics.getUniqueId() != null ? this.convertUUIDToBytes(statistics.getUniqueId()) : null);
                        preparedStatement.setLong(2, statistics.getRank());
                        preparedStatement.setString(3, statistics.getName());
                        preparedStatement.setString(4, statistics.getGamemode());
                        preparedStatement.setString(5, this.gson.toJson(statistics.getStats()));
                        preparedStatement.setLong(6, statistics.getTimestamp());
                        preparedStatement.setString(7, statistics.getLastMatchId());
                        return preparedStatement.executeUpdate();
                    }
            );
        }
    }

    public boolean isValidStatistics(long timestamp) {
        return System.currentTimeMillis() - timestamp <= this.validStatsMillis;
    }

    public boolean isValidStatistics(PlayerData statistics) {
        return this.isValidStatistics(statistics.getTimestamp());
    }

    private PlayerData asPlayerData(ResultSet resultSet) throws SQLException {
        return new PlayerData(
                this.convertBytesToUUID(resultSet.getBytes("uniqueId")),
                resultSet.getString("name"),
                resultSet.getString("gamemode"),
                this.gson.fromJson(resultSet.getString("stats"), STRING_MAP_TYPE),
                resultSet.getString("lastMatchId"),
                resultSet.getLong("timestamp")
        );
    }

    private UUID convertBytesToUUID(byte[] bytes) {
        if (bytes != null) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            return new UUID(buffer.getLong(), buffer.getLong());
        }
        return null;
    }

    private byte[] convertUUIDToBytes(UUID uuid) {
        return uuid != null ?
                ByteBuffer.wrap(new byte[16]).putLong(uuid.getMostSignificantBits()).putLong(uuid.getLeastSignificantBits()).array() :
                null;
    }

    public int countAvailableStatistics() {
        return this.prepareStatement(
                "SELECT COUNT(*) FROM stats",
                preparedStatement -> {
                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        return resultSet.next() ? resultSet.getInt(1) : 0;
                    }
                },
                0
        );
    }

    public int countAvailableStatistics(String gamemode) {
        return this.prepareStatement(
                "SELECT COUNT(*) FROM stats WHERE gamemode = ?",
                preparedStatement -> {
                    preparedStatement.setString(1, gamemode);
                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        return resultSet.next() ? resultSet.getInt(1) : 0;
                    }
                },
                0
        );
    }

    public void insertMatch(Match match) {
        this.prepareStatement(
                "INSERT INTO matches (matchId, gamemode, map, beginTimestamp, endTimestamp, finished, winners, beginPlayers, endPlayers) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                preparedStatement -> {
                    preparedStatement.setString(1, match.getServerId());
                    preparedStatement.setString(2, match.getGamemode());
                    preparedStatement.setString(3, match.getMap());
                    preparedStatement.setLong(4, match.getBeginTimestamp());
                    preparedStatement.setLong(5, match.getEndTimestamp());
                    preparedStatement.setBoolean(6, match.isFinished());
                    preparedStatement.setString(7, this.gson.toJson(match.getWinners()));
                    preparedStatement.setString(8, this.gson.toJson(match.getBeginPlayers()));
                    preparedStatement.setString(9, this.gson.toJson(match.getEndPlayers()));
                    return preparedStatement.executeUpdate();
                }
        );
    }

    public Collection<Match> getMatches(String gamemode) {
        return this.getLastMatches(gamemode, -1);
    }

    public Collection<Match> getLastMatches(String gamemode, int maxCount) {
        return this.prepareStatement(
                maxCount > -1 ? "SELECT * FROM matches WHERE gamemode = ? LIMIT " + maxCount : "SELECT * FROM matches WHERE gamemode = ?",
                preparedStatement -> {
                    preparedStatement.setString(1, gamemode);
                    Collection<Match> matches = new ArrayList<>();
                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        while (resultSet.next()) {
                            matches.add(this.asMatch(resultSet));
                        }
                    }
                    return matches;
                }
        );
    }

    public Collection<Match> getMatches() {
        return this.getLastMatches(-1);
    }

    public Collection<Match> getLastMatches(int maxCount) {
        return this.prepareStatement(
                maxCount > -1 ? "SELECT * FROM matches LIMIT " + maxCount : "SELECT * FROM matches",
                preparedStatement -> {
                    Collection<Match> matches = new ArrayList<>();
                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        while (resultSet.next()) {
                            matches.add(this.asMatch(resultSet));
                        }
                    }
                    return matches;
                }
        );
    }

    private Match asMatch(ResultSet resultSet) throws SQLException {
        return new Match(
                resultSet.getString("map"),
                resultSet.getString("gamemode"),
                resultSet.getString("matchId"),
                resultSet.getLong("beginTimestamp"),
                resultSet.getLong("endTimestamp"),
                resultSet.getBoolean("finished"),
                this.gson.fromJson(resultSet.getString("winners"), STRING_COLLECTION_TYPE),
                this.gson.fromJson(resultSet.getString("beginPlayers"), STRING_COLLECTION_TYPE),
                this.gson.fromJson(resultSet.getString("endPlayers"), STRING_COLLECTION_TYPE),
                null,
                null
        );
    }

    public Match getLastMatch() {
        return this.prepareStatement(
                "SELECT * FROM matches ORDER BY beginTimestamp DESC LIMIT 1",
                preparedStatement -> {
                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        if (resultSet.next()) {
                            return this.asMatch(resultSet);
                        }
                    }
                    return null;
                }
        );
    }

    public Match getFastestMatch(String gamemode) {
        return this.prepareStatement(
                "SELECT * FROM matches WHERE gamemode = ? ORDER BY (endTimestamp - beginTimestamp) LIMIT 1",
                preparedStatement -> {
                    preparedStatement.setString(1, gamemode);
                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        if (resultSet.next()) {
                            return this.asMatch(resultSet);
                        }
                    }
                    return null;
                }
        );
    }

    public Match getFastestMatch(String gamemode, String winner) {
        return this.prepareStatement(
                "SELECT * FROM matches WHERE gamemode = ? AND winners LIKE '%' || ? || '%' ORDER BY (endTimestamp - beginTimestamp) LIMIT 1",
                preparedStatement -> {
                    preparedStatement.setString(1, gamemode);
                    preparedStatement.setString(2, winner);
                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        if (resultSet.next()) {
                            return this.asMatch(resultSet);
                        }
                    }
                    return null;
                }
        );
    }

    public Match getSlowestMatch(String gamemode) {
        return this.prepareStatement(
                "SELECT * FROM matches WHERE gamemode = ? ORDER BY (endTimestamp - beginTimestamp) DESC LIMIT 1",
                preparedStatement -> {
                    preparedStatement.setString(1, gamemode);
                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        if (resultSet.next()) {
                            return this.asMatch(resultSet);
                        }
                    }
                    return null;
                }
        );
    }

    public Match getSlowestMatch(String gamemode, String winner) {
        return this.prepareStatement(
                "SELECT * FROM matches WHERE gamemode = ? AND winners LIKE '%' || ? || '%' ORDER BY (endTimestamp - beginTimestamp) DESC LIMIT 1",
                preparedStatement -> {
                    preparedStatement.setString(1, gamemode);
                    preparedStatement.setString(2, winner);
                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        if (resultSet.next()) {
                            return this.asMatch(resultSet);
                        }
                    }
                    return null;
                }
        );
    }

    public int countMatches() {
        return this.prepareStatement(
                "SELECT COUNT(*) FROM matches",
                preparedStatement -> {
                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        return resultSet.next() ? resultSet.getInt(1) : 0;
                    }
                },
                0
        );
    }

    public int countMatches(String gamemode) {
        return this.prepareStatement(
                "SELECT COUNT(*) FROM matches WHERE gamemode = ?",
                preparedStatement -> {
                    preparedStatement.setString(1, gamemode);
                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        return resultSet.next() ? resultSet.getInt(1) : 0;
                    }
                },
                0
        );
    }

    public int countMatches(String gamemode, String map) {
        return this.prepareStatement(
                "SELECT COUNT(*) FROM matches WHERE gamemode = ? AND map = ?",
                preparedStatement -> {
                    preparedStatement.setString(1, gamemode);
                    preparedStatement.setString(2, map);
                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        return resultSet.next() ? resultSet.getInt(1) : 0;
                    }
                },
                0
        );
    }
}
