package de.derrop.labymod.addons.server.command.console.commands;
/*
 * Created by derrop on 10.10.2019
 */

import de.derrop.labymod.addons.server.GommeStatsServer;
import de.derrop.labymod.addons.server.command.Command;
import de.derrop.labymod.addons.server.command.CommandSender;
import de.derrop.labymod.addons.server.database.Tag;
import de.derrop.labymod.addons.server.database.TagType;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

public class CommandTag extends Command {
    private GommeStatsServer statsServer;
    private TagType tagType;

    private DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    public CommandTag(GommeStatsServer statsServer, TagType tagType, String... names) {
        super(names);
        this.statsServer = statsServer;
        this.tagType = tagType;
    }

    @Override
    public void execute(CommandSender sender, String label, String line, String[] args) {
        if (args.length >= 3 && args[0].equalsIgnoreCase("add")) {
            String tag = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            boolean success = this.statsServer.getDatabaseProvider().getTagProvider().addTag(this.tagType, sender.getName(), args[1], tag);
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
            Collection<Tag> tags = this.statsServer.getDatabaseProvider().getTagProvider().listTags(this.tagType, args[1]);
            if (tags.isEmpty()) {
                sender.sendMessage("No tags available for the user \"" + args[1] + "\"");
            } else {
                sender.sendMessage("Tags for the user \"" + args[1] + "\": \n" +
                        tags.stream()
                                .map(tag -> "\"" + tag.getTag() + "\" by " + tag.getCreator() + " (" + this.dateFormat.format(new Date(tag.getCreationTime())) + ")")
                                .collect(Collectors.joining("\n"))
                );
            }
        } else if (args.length >= 2 && args[0].equalsIgnoreCase("tlist")) {
            String requestTag = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            Collection<Tag> tags = this.statsServer.getDatabaseProvider().getTagProvider().listUsers(this.tagType, requestTag);
            if (tags.isEmpty()) {
                sender.sendMessage("No users available with the tag \"" + requestTag + "\"");
            } else {
                sender.sendMessage("Users with the tag \"" + requestTag + "\": \n" +
                        tags.stream()
                                .map(tag -> "\"" + tag.getName() + "\" by " + tag.getCreator() + " (" + this.dateFormat.format(new Date(tag.getCreationTime())) + ")")
                                .collect(Collectors.joining("\n"))
                );
            }
        } else if (args.length == 1 && args[0].equalsIgnoreCase("tags")) {
            Collection<String> tags = this.statsServer.getDatabaseProvider().getTagProvider().listAllTags(this.tagType);
            sender.sendMessage("Available tags in our database: \n" + String.join("\n", tags));
        } else {
            sender.sendMessage(
                    super.getNames()[0] + " add <name> <tag>",
                    super.getNames()[0] + " remove <name> <tag>",
                    super.getNames()[0] + " tlist <tag>",
                    super.getNames()[0] + " list <name>"
            );
        }
    }
}
