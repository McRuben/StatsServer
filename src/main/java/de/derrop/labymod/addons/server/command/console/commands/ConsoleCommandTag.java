package de.derrop.labymod.addons.server.command.console.commands;
/*
 * Created by derrop on 10.10.2019
 */

import de.derrop.labymod.addons.server.GommeStatsServer;
import de.derrop.labymod.addons.server.command.Command;
import de.derrop.labymod.addons.server.command.CommandSender;
import de.derrop.labymod.addons.server.database.TagType;

import java.util.Arrays;
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
        if (args.length >= 3 && args[0].equalsIgnoreCase("add")) {
            String tag = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            boolean success = this.statsServer.getDatabaseProvider().getTagProvider().addTag(this.tagType, args[1], tag);
            if (success) {
                sender.sendMessage("Successfully added the tag \"" + tag + "\" to the user \"" + args[1] + "\"");
            } else {
                sender.sendMessage("This user already has a tag called \"" + tag + "\"");
            }
        } else if (args.length >= 3 && args[0].equalsIgnoreCase("remove")) {
            String tag = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            this.statsServer.getDatabaseProvider().getTagProvider().removeTag(this.tagType, args[1], tag);
            sender.sendMessage("Successfully removed the tag \"" + tag + "\" from the user \"" + args[1] + "\"");
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
