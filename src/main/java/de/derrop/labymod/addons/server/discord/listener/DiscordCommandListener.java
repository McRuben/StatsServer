package de.derrop.labymod.addons.server.discord.listener;
/*
 * Created by derrop on 05.10.2019
 */

import de.derrop.labymod.addons.server.discord.DiscordBot;
import de.derrop.labymod.addons.server.discord.DiscordCommandSender;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class DiscordCommandListener extends ListenerAdapter {

    private DiscordBot discordBot;

    public DiscordCommandListener(DiscordBot discordBot) {
        this.discordBot = discordBot;
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        String message = event.getMessage().getContentRaw();
        if (!message.startsWith(this.discordBot.getConfiguration().getCommandPrefix())) {
            return;
        }
        String commandLine = message.substring(this.discordBot.getConfiguration().getCommandPrefix().length());
        this.discordBot.getCommandMap().dispatchCommand(new DiscordCommandSender(event.getMember(), event.getChannel()), commandLine);
    }

    private void sendMessage() {

    }
}
