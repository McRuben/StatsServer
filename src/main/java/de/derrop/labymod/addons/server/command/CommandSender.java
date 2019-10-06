package de.derrop.labymod.addons.server.command;
/*
 * Created by derrop on 05.10.2019
 */

public interface CommandSender {

    void sendMessage(String message);

    void sendMessage(String... messages);

    String getName();

}
