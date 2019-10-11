package de.derrop.labymod.addons.server.database;
/*
 * Created by derrop on 10.10.2019
 */

public enum TagType {
    PLAYER("player"),
    CLAN("clan");

    private String friendlyName;

    TagType(String friendlyName) {
        this.friendlyName = friendlyName;
    }

    public String getFriendlyName() {
        return friendlyName;
    }
}
