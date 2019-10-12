package de.derrop.labymod.addons.server.database;
/*
 * Created by derrop on 11.10.2019
 */

import de.derrop.labymod.addons.server.util.Utility;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

public class UserAuthenticator {

    private DatabaseProvider databaseProvider;

    public UserAuthenticator(DatabaseProvider databaseProvider) {
        this.databaseProvider = databaseProvider;
    }

    public boolean authUser(String authToken, UUID uniqueId) {
        return this.databaseProvider.prepareStatement(
                "SELECT * FROM auth WHERE token = ? AND uniqueId = ?",
                preparedStatement -> {
                    preparedStatement.setString(1, Utility.hashString(authToken));
                    preparedStatement.setBytes(2, Utility.convertUUIDToBytes(uniqueId));
                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        return resultSet.next();
                    }
                }
        );
    }

    public boolean containsUser(UUID uniqueId) {
        return this.databaseProvider.prepareStatement(
                "SELECT * FROM auth WHERE uniqueId = ?",
                preparedStatement -> {
                    preparedStatement.setBytes(1, Utility.convertUUIDToBytes(uniqueId));
                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        return resultSet.next();
                    }
                }
        );
    }

    public boolean addUser(String authToken, UUID uniqueId) {
        if (this.containsUser(uniqueId)) {
            return false;
        }

        return this.databaseProvider.prepareStatement(
                "INSERT INTO auth (token, uniqueId, registrationTime) VALUES (?, ?, ?)",
                preparedStatement -> {
                    preparedStatement.setString(1, Utility.hashString(authToken));
                    preparedStatement.setBytes(2, Utility.convertUUIDToBytes(uniqueId));
                    preparedStatement.setLong(3, System.currentTimeMillis());
                    return preparedStatement.executeUpdate();
                }
        ) > 0;
    }

    public boolean deleteUser(UUID uniqueId) {
        return this.databaseProvider.prepareStatement(
                "DELETE * FROM auth WHERE uniqueId = ?",
                preparedStatement -> {
                    preparedStatement.setBytes(1, Utility.convertUUIDToBytes(uniqueId));
                    return preparedStatement.executeUpdate();
                }
        ) > 0;
    }

    public Collection<UUID> listUsers() {
        return this.databaseProvider.prepareStatement(
                "SELECT uniqueId FROM auth",
                preparedStatement -> {
                    Collection<UUID> uniqueIds = new ArrayList<>();
                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        while (resultSet.next()) {
                            uniqueIds.add(Utility.convertBytesToUUID(resultSet.getBytes("uniqueId")));
                        }
                    }
                    return uniqueIds;
                }
        );
    }

}
