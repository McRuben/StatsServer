package de.derrop.labymod.addons.server.database;
/*
 * Created by derrop on 10.10.2019
 */

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;

public class TagProvider {

    private DatabaseProvider databaseProvider;

    TagProvider(DatabaseProvider databaseProvider) {
        this.databaseProvider = databaseProvider;
    }

    public boolean addTag(TagType tagType, String name, String tag) {
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
                "INSERT INTO tags (type, name, tag) VALUES (?, ?, ?)",
                preparedStatement -> {
                    preparedStatement.setString(1, tagType.toString());
                    preparedStatement.setString(2, name);
                    preparedStatement.setString(3, tag);
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

    public Collection<String> listTags(TagType tagType, String name) {
        return this.databaseProvider.prepareStatement(
                "SELECT * FROM tags WHERE type = ? AND lower(name) = lower(?)",
                preparedStatement -> {
                    preparedStatement.setString(1, tagType.toString());
                    preparedStatement.setString(2, name);
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
}
