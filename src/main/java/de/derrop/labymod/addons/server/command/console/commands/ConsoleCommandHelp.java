package de.derrop.labymod.addons.server.command.console.commands;
/*
 * Created by derrop on 05.10.2019
 */

import de.derrop.labymod.addons.server.command.Command;
import de.derrop.labymod.addons.server.command.CommandMap;
import de.derrop.labymod.addons.server.command.CommandSender;

public class ConsoleCommandHelp extends Command {
    private CommandMap commandMap;

    public ConsoleCommandHelp(CommandMap commandMap) {
        super("help", "commands");
        this.commandMap = commandMap;
    }

    @Override
    public void execute(CommandSender sender, String label, String line, String[] args) {
        sender.sendMessage(this.commandMap.getCommands().keySet().toArray(new String[0]));
    }
}
