package de.derrop.labymod.addons.server.command;
/*
 * Created by derrop on 05.10.2019
 */

import de.derrop.labymod.addons.server.command.console.ConsoleCommandSender;
import lombok.Getter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Getter
public class CommandMap {

    private Map<String, Command> commands = new HashMap<>();
    private CommandSender consoleCommandSender = new ConsoleCommandSender();
    private CommandMap parent;

    public CommandMap(CommandMap parent) {
        this.parent = parent;
    }

    public CommandMap() {
    }

    public void registerCommand(Command command) {
        for (String name : command.getNames()) {
            this.commands.put(name.toLowerCase(), command);
        }
    }

    public Command getCommand(String name) {
        return this.commands.computeIfAbsent(name.toLowerCase(), s -> this.parent != null ? this.parent.getCommand(s) : null);
    }

    public void dispatchCommand(String line) {
        this.dispatchCommand(this.consoleCommandSender, line);
    }

    public void dispatchCommand(CommandSender sender, String line) {
        String[] args = line.split(" ");

        String commandName = args[0];
        Command command = this.getCommand(commandName);

        if (command != null) {
            args = Arrays.copyOfRange(args, 1, args.length);

            command.execute(sender, commandName, line, args);
        }
    }

}
