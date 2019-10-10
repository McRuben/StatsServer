package de.derrop.labymod.addons.server.command.console.commands;
/*
 * Created by derrop on 05.10.2019
 */

import de.derrop.labymod.addons.server.GommeStatsServer;
import de.derrop.labymod.addons.server.PlayerData;
import de.derrop.labymod.addons.server.command.Command;
import de.derrop.labymod.addons.server.command.CommandSender;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ConsoleCommandStats extends Command {
    private GommeStatsServer statsServer;

    private DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    public ConsoleCommandStats(GommeStatsServer statsServer) {
        super("stats", "statistics");
        this.statsServer = statsServer;
    }

    @Override
    public void execute(CommandSender sender, String label, String line, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(
                    "stats <gamemode> <PLAYER>",
                    "stats <gamemode> count",
                    "stats <gamemode> best",
                    "stats <gamemode> worst"
            );
            return;
        }

        String gamemode = args[0].toUpperCase();

        if (args[1].equalsIgnoreCase("count")) {
            sender.sendMessage("Statistics saved in our database for gamemode " + gamemode + ": " + this.statsServer.getDatabaseProvider().countAvailableStatistics(gamemode));
        } else if (args[1].equalsIgnoreCase("best")) {
            this.displayStats(sender, this.statsServer.getDatabaseProvider().getBestStatistics(gamemode));
        } else if (args[1].equalsIgnoreCase("worst")) {
            this.displayStats(sender, this.statsServer.getDatabaseProvider().getWorstStatistics(gamemode));
        } else {
            this.displayStats(sender, this.statsServer.getDatabaseProvider().getStatistics(args[1], gamemode));
        }
    }

    private void displayStats(CommandSender sender, PlayerData playerData) {
        if (playerData != null) {
            List<String> messages = new ArrayList<>();
            messages.add("Stats for " + playerData.getName() + " in " + playerData.getGamemode() + ":");
            playerData.getStats().forEach((key, value) -> messages.add(key + ": " + value));
            messages.add("Last update: " + this.dateFormat.format(new Date(playerData.getTimestamp())));
            sender.sendMessage(messages.toArray(new String[0]));
        } else {
            sender.sendMessage("No statistics found");
        }
    }
}
