package de.derrop.labymod.addons.server.database;
/*
 * Created by derrop on 10.10.2019
 */

import de.derrop.labymod.addons.server.PlayerData;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static de.derrop.labymod.addons.server.database.DatabaseProvider.STRING_MAP_TYPE;

public class StatisticsProvider {

    private DatabaseProvider databaseProvider;

    private long validStatsMillis = TimeUnit.MINUTES.toMillis(5);

    StatisticsProvider(DatabaseProvider databaseProvider) {
        this.databaseProvider = databaseProvider;
    }

    public Collection<PlayerData> getStatistics(String name) {
        return this.databaseProvider.prepareStatement(
                "SELECT * FROM stats WHERE lower(name) = lower(?)",
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
        return this.databaseProvider.prepareStatement(
                "SELECT * FROM stats WHERE lower(name) = lower(?) AND gamemode = ?",
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
        return this.databaseProvider.prepareStatement(
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
        return this.databaseProvider.prepareStatement(
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
        return this.databaseProvider.prepareStatement(
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
        return this.databaseProvider.prepareStatement(
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
        return this.databaseProvider.prepareStatement(
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

        boolean exists = this.databaseProvider.prepareStatement(
                "SELECT * FROM stats WHERE lower(name) = lower(?) AND gamemode = ?",
                preparedStatement -> {
                    preparedStatement.setString(1, statistics.getName());
                    preparedStatement.setString(2, statistics.getGamemode());
                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        return resultSet.next();
                    }
                }
        );
        if (exists) {
            this.databaseProvider.prepareStatement(
                    "UPDATE stats SET timestamp = ?, stats = ?, lastMatchId = ?, rank = ? WHERE name = ? AND gamemode = ?",
                    preparedStatement -> {
                        preparedStatement.setLong(1, statistics.getTimestamp());
                        preparedStatement.setString(2, this.databaseProvider.gson.toJson(statistics.getStats()));
                        preparedStatement.setString(3, statistics.getLastMatchId());
                        preparedStatement.setLong(4, statistics.getRank());
                        preparedStatement.setString(5, statistics.getName());
                        preparedStatement.setString(6, statistics.getGamemode());

                        return preparedStatement.executeUpdate();
                    }
            );
        } else {
            this.databaseProvider.prepareStatement(
                    "INSERT INTO stats (uniqueId, rank, name, gamemode, stats, timestamp, lastMatchId) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    preparedStatement -> {
                        preparedStatement.setBytes(1, statistics.getUniqueId() != null ? this.databaseProvider.convertUUIDToBytes(statistics.getUniqueId()) : null);
                        preparedStatement.setLong(2, statistics.getRank());
                        preparedStatement.setString(3, statistics.getName());
                        preparedStatement.setString(4, statistics.getGamemode());
                        preparedStatement.setString(5, this.databaseProvider.gson.toJson(statistics.getStats()));
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
                this.databaseProvider.convertBytesToUUID(resultSet.getBytes("uniqueId")),
                resultSet.getString("name"),
                resultSet.getString("gamemode"),
                this.databaseProvider.gson.fromJson(resultSet.getString("stats"), STRING_MAP_TYPE),
                resultSet.getString("lastMatchId"),
                resultSet.getLong("timestamp")
        );
    }

    public int countAvailableStatistics() {
        return this.databaseProvider.prepareStatement(
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
        return this.databaseProvider.prepareStatement(
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
    
}
