package de.derrop.labymod.addons.server.database;
/*
 * Created by derrop on 01.10.2019
 */

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import de.derrop.labymod.addons.server.Match;
import de.derrop.labymod.addons.server.function.ThrowingFunction;

import java.io.File;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public class DatabaseProvider {

    static final Type STRING_MAP_TYPE = new TypeToken<Map<String, String>>() {
    }.getType();
    static final Type STRING_COLLECTION_TYPE = new TypeToken<Collection<String>>() {
    }.getType();

    final Gson gson = new Gson();
    Connection databaseConnection;

    private StatisticsProvider statisticsProvider = new StatisticsProvider(this);
    private MatchProvider matchProvider = new MatchProvider(this);
    private TagProvider tagProvider = new TagProvider(this);

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
            this.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS tags(type VARCHAR(16), name VARCHAR(64), tag TEXT)",
                    PreparedStatement::executeUpdate
            );
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    <T> T prepareStatement(String sql, ThrowingFunction<PreparedStatement, T, SQLException> function) {
        return this.prepareStatement(sql, function, null);
    }

    <T> T prepareStatement(String sql, ThrowingFunction<PreparedStatement, T, SQLException> function, T def) {
        try {
            try (PreparedStatement statement = this.databaseConnection.prepareStatement(sql)) {
                return function.apply(statement);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return def;
    }

    UUID convertBytesToUUID(byte[] bytes) {
        if (bytes != null) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            return new UUID(buffer.getLong(), buffer.getLong());
        }
        return null;
    }

    byte[] convertUUIDToBytes(UUID uuid) {
        return uuid != null ?
                ByteBuffer.wrap(new byte[16]).putLong(uuid.getMostSignificantBits()).putLong(uuid.getLeastSignificantBits()).array() :
                null;
    }

    public MatchProvider getMatchProvider() {
        return matchProvider;
    }

    public StatisticsProvider getStatisticsProvider() {
        return statisticsProvider;
    }

    public TagProvider getTagProvider() {
        return tagProvider;
    }
}
