package de.derrop.labymod.addons.server.command.console.commands;
/*
 * Created by derrop on 13.10.2019
 */

import de.derrop.labymod.addons.server.GommeStatsServer;
import de.derrop.labymod.addons.server.command.Command;
import de.derrop.labymod.addons.server.command.CommandSender;

import java.util.stream.Collectors;

public class CommandList extends Command {
    private GommeStatsServer statsServer;

    public CommandList(GommeStatsServer statsServer) {
        super("list");
        this.statsServer = statsServer;
    }

    @Override
    public void execute(CommandSender sender, String label, String line, String[] args) {
        sender.sendMessage(
                this.statsServer.getSyncServer().getConnectedPlayers().values().stream()
                        .map(player -> player.getUniqueId() + "#" + player.getName() + (player.getCurrentMatch() != null ? " Playing " + player.getCurrentMatch().getGamemode() : " Playing nothing"))
                        .collect(Collectors.joining("\n"))
        );
    }
}
