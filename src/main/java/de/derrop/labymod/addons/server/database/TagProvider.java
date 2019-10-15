package de.derrop.labymod.addons.server.database;
/*
 * Created by derrop on 10.10.2019
 */

import com.google.gson.JsonObject;
import de.derrop.labymod.addons.server.GommeStatsServer;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

public class TagProvider {

    private DatabaseProvider databaseProvider;

    public TagProvider(DatabaseProvider databaseProvider) {
        this.databaseProvider = databaseProvider;
    }

    public boolean addTag(TagType tagType, String creator, String name, String tag) {
        boolean exists = this.databaseProvider.prepareStatement(
                "SELECT * FROM tags WHERE type = ? AND lower(name) = lower(?) AND lower(tag) = lower(?)",
                preparedStatement -> {
                    preparedStatement.setString(1, tagType.toString());
                    preparedStatement.setString(2, name);
                    preparedStatement.setString(3, tag);
                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        return resultSet.next();
                    }
                }
        );
        if (exists) {
            return false;
        }

        this.databaseProvider.prepareStatement(
                "INSERT INTO tags (type, creator, name, tag, creationTime) VALUES (?, ?, ?, ?, ?)",
                preparedStatement -> {
                    preparedStatement.setString(1, tagType.toString());
                    preparedStatement.setString(2, creator);
                    preparedStatement.setString(3, name);
                    preparedStatement.setString(4, tag);
                    preparedStatement.setLong(5, System.currentTimeMillis());
                    return preparedStatement.executeUpdate();
                }
        );

        this.notifyClients("add", tagType, name, tag);

        return true;
    }

    public void removeTag(TagType tagType, String name, String tag) {
        this.databaseProvider.prepareStatement(
                "DELETE FROM tags WHERE type = ? AND lower(name) = lower(?) AND lower(tag) = lower(?)",
                preparedStatement -> {
                    preparedStatement.setString(1, tagType.toString());
                    preparedStatement.setString(2, name);
                    preparedStatement.setString(3, tag);
                    return preparedStatement.executeUpdate();
                }
        );

        this.notifyClients("remove", tagType, name, tag);
    }

    private void notifyClients(String query, TagType tagType, String name, String tag) {
        JsonObject obj = new JsonObject();
        obj.addProperty("query", query);
        obj.addProperty("type", tagType.toString());
        obj.addProperty("name", name);
        obj.addProperty("tag", tag);
        this.databaseProvider.getStatsServer().getSyncServer().broadcastPacket((short) 1, obj);
    }

    public Collection<Tag> listTags(TagType tagType, String name) {
        return this.databaseProvider.prepareStatement(
                "SELECT * FROM tags WHERE type = ? AND lower(name) = lower(?)",
                preparedStatement -> {
                    preparedStatement.setString(1, tagType.toString());
                    preparedStatement.setString(2, name);
                    return this.mapPreparedStatementToTags(tagType, preparedStatement);
                }
        );
    }

    public Collection<String> listAllTags(TagType tagType) {
        return this.databaseProvider.prepareStatement(
                "SELECT tag FROM tags WHERE type = ?",
                preparedStatement -> {
                    preparedStatement.setString(1, tagType.toString());
                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        Collection<String> tags = new ArrayList<>();
                        while (resultSet.next()) {
                            tags.add(resultSet.getString("tag"));
                        }
                        return tags;
                    }
                }
        );
    }

    public Collection<Tag> listUsers(TagType tagType, String tag) {
        return this.databaseProvider.prepareStatement(
                "SELECT * FROM tags WHERE type = ? AND lower(tag) = lower(?)",
                preparedStatement -> {
                    preparedStatement.setString(1, tagType.toString());
                    preparedStatement.setString(2, tag);
                    return this.mapPreparedStatementToTags(tagType, preparedStatement);
                }
        );
    }

    private Collection<Tag> mapPreparedStatementToTags(TagType tagType, PreparedStatement preparedStatement) throws SQLException {
        try (ResultSet resultSet = preparedStatement.executeQuery()) {
            Collection<Tag> tags = new ArrayList<>();
            while (resultSet.next()) {
                tags.add(new Tag(
                        tagType,
                        resultSet.getString("creator"),
                        resultSet.getString("name"),
                        resultSet.getString("tag"),
                        resultSet.getLong("creationTime")
                ));
            }
            return tags;
        }
    }
}
