package de.derrop.labymod.addons.server.command.console.commands;
/*
 * Created by derrop on 05.10.2019
 */

import de.derrop.labymod.addons.server.GommeStatsServer;
import de.derrop.labymod.addons.server.Match;
import de.derrop.labymod.addons.server.command.Command;
import de.derrop.labymod.addons.server.command.CommandSender;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ConsoleCommandMatch extends Command {

    private GommeStatsServer statsServer;

    private DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    public ConsoleCommandMatch(GommeStatsServer statsServer) {
        super("match");
        this.statsServer = statsServer;
    }

    @Override
    public void execute(CommandSender sender, String label, String line, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(
                    "match <gamemode> fastest [winner]",
                    "match <gamemode> slowest <winner>",
                    "match <gamemode> <map> count",
                    "match <gamemode> count"
            );
            return;
        }

        if (args.length == 2 && args[1].equalsIgnoreCase("fastest")) {
            this.displayMatch(sender, this.statsServer.getDatabaseProvider().getMatchProvider().getFastestMatch(args[0]));
        } else if (args.length == 3 && args[1].equalsIgnoreCase("fastest")) {
            this.displayMatch(sender, this.statsServer.getDatabaseProvider().getMatchProvider().getFastestMatch(args[0], args[2]));
        } else if (args.length == 2 && args[1].equalsIgnoreCase("slowest")) {
            this.displayMatch(sender, this.statsServer.getDatabaseProvider().getMatchProvider().getSlowestMatch(args[0]));
        } else if (args.length == 3 && args[1].equalsIgnoreCase("slowest")) {
            this.displayMatch(sender, this.statsServer.getDatabaseProvider().getMatchProvider().getSlowestMatch(args[0], args[2]));
        } else if (args.length == 3 && args[2].equalsIgnoreCase("count")) {
            sender.sendMessage("Matches played in " + args[0] + " on the map " + args[1] + ": " + this.statsServer.getDatabaseProvider().getMatchProvider().countMatches(args[0], args[1]));
        } else if (args.length == 2 && args[1].equalsIgnoreCase("count")) {
            sender.sendMessage("Matches played in " + args[0] + ": " + this.statsServer.getDatabaseProvider().getMatchProvider().countMatches(args[0]));
        }
    }

    private void displayMatch(CommandSender sender, Match match) {
        if (match == null) {
            sender.sendMessage("No match found");
            return;
        }
        long milliseconds = match.getEndTimestamp() - match.getBeginTimestamp();
        long seconds = (milliseconds / 1000) % 60;
        long minutes = ((milliseconds / (1000 * 60)) % 60);
        long hours = ((milliseconds / (1000 * 60 * 60)) % 24);

        List<String> messages = new ArrayList<>();
        messages.add("Match on " + match.getGamemode() + ":" + match.getMap() + ":");
        messages.add("Players on begin: " + String.join(", ", match.getBeginPlayers()));
        messages.add("Players on end: " + String.join(", ", match.getEndPlayers()));
        messages.add("Winners: " + String.join(", ", match.getWinners()));
        messages.add("Begin - End: " + this.dateFormat.format(new Date(match.getBeginTimestamp())) + " - " + this.dateFormat.format(new Date(match.getEndTimestamp())));
        messages.add("Time: " + String.format("%02d:%02d:%02d", hours, minutes, seconds));

        sender.sendMessage(messages.toArray(new String[0]));
    }
}
