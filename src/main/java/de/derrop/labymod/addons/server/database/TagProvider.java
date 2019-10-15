package de.derrop.labymod.addons.server.database;
/*
 * Created by derrop on 10.10.2019
 */

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;

public class TagProvider { //todo add time when a tag was added

    private DatabaseProvider databaseProvider;

    TagProvider(DatabaseProvider databaseProvider) {
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
    }

    public Collection<Tag> listTags(TagType tagType, String name) {
        return this.databaseProvider.prepareStatement(
                "SELECT * FROM tags WHERE type = ? AND lower(name) = lower(?)",
                preparedStatement -> {
                    preparedStatement.setString(1, tagType.toString());
                    preparedStatement.setString(2, name);
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
        );
    }
}
