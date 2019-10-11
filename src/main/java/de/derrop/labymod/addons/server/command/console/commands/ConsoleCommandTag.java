package de.derrop.labymod.addons.server.command.console.commands;
/*
 * Created by derrop on 10.10.2019
 */

import de.derrop.labymod.addons.server.GommeStatsServer;
import de.derrop.labymod.addons.server.command.Command;
import de.derrop.labymod.addons.server.command.CommandSender;
import de.derrop.labymod.addons.server.database.TagType;

import java.util.Collection;

public class ConsoleCommandTag extends Command {
    private GommeStatsServer statsServer;
    private TagType tagType;

    public ConsoleCommandTag(GommeStatsServer statsServer, TagType tagType, String... names) {
        super(names);
        this.statsServer = statsServer;
        this.tagType = tagType;
    }

    @Override
    public void execute(CommandSender sender, String label, String line, String[] args) {
        if (args.length == 3 && args[0].equalsIgnoreCase("add")) {
            boolean success = this.statsServer.getDatabaseProvider().getTagProvider().addTag(this.tagType, args[1], args[2]);
            if (success) {
                sender.sendMessage("Successfully added the tag \"" + args[2] + "\" to the user \"" + args[1] + "\"");
            } else {
                sender.sendMessage("This user already has a tag called \"" + args[2] + "\"");
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("remove")) {
            this.statsServer.getDatabaseProvider().getTagProvider().removeTag(this.tagType, args[1], args[2]);
            sender.sendMessage("Successfully removed the tag \"" + args[2] + "\" from the user \"" + args[1] + "\"");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("list")) {
            Collection<String> tags = this.statsServer.getDatabaseProvider().getTagProvider().listTags(this.tagType, args[1]);
            if (tags.isEmpty()) {
                sender.sendMessage("No tags available for the user \"" + args[1] + "\"");
            } else {
                sender.sendMessage("Tags for the user \"" + args[1] + "\": " + String.join(", ", tags));
            }
        } else {
            sender.sendMessage(
                    super.getNames()[0] + " add <name> <tag>",
                    super.getNames()[0] + " remove <name> <tag>",
                    super.getNames()[0] + " list <name>"
            );
        }
    }
}
