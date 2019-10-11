package de.derrop.labymod.addons.server.database;
/*
 * Created by derrop on 10.10.2019
 */

import de.derrop.labymod.addons.server.Match;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

import static de.derrop.labymod.addons.server.database.DatabaseProvider.STRING_COLLECTION_TYPE;

public class MatchProvider {

    private DatabaseProvider databaseProvider;

    MatchProvider(DatabaseProvider databaseProvider) {
        this.databaseProvider = databaseProvider;
    }

    public void insertMatch(Match match) {
        this.databaseProvider.prepareStatement(
                "INSERT INTO matches (matchId, gamemode, map, beginTimestamp, endTimestamp, finished, winners, beginPlayers, endPlayers) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                preparedStatement -> {
                    preparedStatement.setString(1, match.getServerId());
                    preparedStatement.setString(2, match.getGamemode());
                    preparedStatement.setString(3, match.getMap());
                    preparedStatement.setLong(4, match.getBeginTimestamp());
                    preparedStatement.setLong(5, match.getEndTimestamp());
                    preparedStatement.setBoolean(6, match.isFinished());
                    preparedStatement.setString(7, this.databaseProvider.gson.toJson(match.getWinners()));
                    preparedStatement.setString(8, this.databaseProvider.gson.toJson(match.getBeginPlayers()));
                    preparedStatement.setString(9, this.databaseProvider.gson.toJson(match.getEndPlayers()));
                    return preparedStatement.executeUpdate();
                }
        );
    }

    public Collection<Match> getMatches(String gamemode) {
        return this.getLastMatches(gamemode, -1);
    }

    public Collection<Match> getLastMatches(String gamemode, int maxCount) {
        return this.databaseProvider.prepareStatement(
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
        return this.databaseProvider.prepareStatement(
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
                this.databaseProvider.gson.fromJson(resultSet.getString("winners"), STRING_COLLECTION_TYPE),
                this.databaseProvider.gson.fromJson(resultSet.getString("beginPlayers"), STRING_COLLECTION_TYPE),
                this.databaseProvider.gson.fromJson(resultSet.getString("endPlayers"), STRING_COLLECTION_TYPE),
                null,
                null
        );
    }

    public Match getLastMatch() {
        return this.databaseProvider.prepareStatement(
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
        return this.databaseProvider.prepareStatement(
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
        return this.databaseProvider.prepareStatement(
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
        return this.databaseProvider.prepareStatement(
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
        return this.databaseProvider.prepareStatement(
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
        return this.databaseProvider.prepareStatement(
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
        return this.databaseProvider.prepareStatement(
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
        return this.databaseProvider.prepareStatement(
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
