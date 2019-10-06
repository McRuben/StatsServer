package de.derrop.labymod.addons.server.command.console;
/*
 * Created by derrop on 05.10.2019
 */

import de.derrop.labymod.addons.server.command.CommandSender;

public class ConsoleCommandSender implements CommandSender {
    @Override
    public void sendMessage(String message) {
        System.out.println(message);
    }

    @Override
    public void sendMessage(String... messages) {
        for (String message : messages) {
            this.sendMessage(message);
        }
    }

    @Override
    public String getName() {
        return "Console";
    }
}
