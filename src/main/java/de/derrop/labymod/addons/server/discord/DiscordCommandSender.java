package de.derrop.labymod.addons.server.discord;
/*
 * Created by derrop on 05.10.2019
 */

import de.derrop.labymod.addons.server.command.CommandSender;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.MessageChannel;

import java.util.ArrayList;
import java.util.Collection;

public class DiscordCommandSender implements CommandSender {

    private Member member;
    private MessageChannel channel;

    public DiscordCommandSender(Member member, MessageChannel channel) {
        this.member = member;
        this.channel = channel;
    }

    @Override
    public void sendMessage(String message) {
        Collection<String> messages = new ArrayList<>();
        while (message.length() > 1995) {
            messages.add(message.substring(0, 1995));
            message = message.substring(1995);
        }
        if (!message.isEmpty()) {
            messages.add(message);
        }
        for (String s : messages) {
            this.channel.sendMessage(s).queue();
        }
    }

    @Override
    public void sendMessage(String... messages) {
        StringBuilder builder = new StringBuilder();
        for (String message : messages) {
            builder.append(message).append("\n");
        }
        String message = builder.toString();
        this.sendMessage(message);
    }

    @Override
    public String getName() {
        return this.member.getUser().getName();
    }
}
