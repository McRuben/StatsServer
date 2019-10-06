package de.derrop.labymod.addons.server.discord;
/*
 * Created by derrop on 05.10.2019
 */

import de.derrop.labymod.addons.server.GommeStatsServer;
import de.derrop.labymod.addons.server.Match;
import de.derrop.labymod.addons.server.command.CommandMap;
import de.derrop.labymod.addons.server.config.DiscordConfiguration;
import de.derrop.labymod.addons.server.discord.listener.DiscordCommandListener;
import lombok.Getter;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import javax.security.auth.login.LoginException;
import java.io.Closeable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

@Getter
public class DiscordBot implements Closeable {

    private JDA jda;
    private DiscordConfiguration configuration;
    private CommandMap commandMap;

    private DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    public DiscordBot(GommeStatsServer statsServer) {
        this.commandMap = new CommandMap(statsServer.getCommandMap());
    }

    public void init(DiscordConfiguration configuration) {
        this.configuration = configuration;
        if (configuration.isEnabled()) {
            try {
                this.jda = new JDABuilder(configuration.getToken())
                        .setAudioEnabled(false)
                        .setAutoReconnect(true)
                        .addEventListener(new ListenerAdapter() {
                            @Override
                            public void onReady(ReadyEvent event) {
                                event.getJDA().removeEventListener(this);
                                DiscordBot.this.handleReady(event.getJDA());
                            }
                        })
                        .build();
            } catch (LoginException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void close() {
        if (this.jda != null) {
            this.jda.shutdownNow();
        }
    }

    private void handleReady(JDA jda) {
        jda.addEventListener(new DiscordCommandListener(this));
    }

    public void handleMatchBegin(Match match) {
        if (this.configuration.getMatchLogChannelId() < 0)
            return;
        MessageChannel channel = this.jda.getTextChannelById(this.configuration.getMatchLogChannelId());
        if (channel == null)
            return;

        channel.sendMessage(
                new EmbedBuilder()
                        .setTitle("**Match Begin**")
                        .addField("GameMode", match.getGamemode(), true)
                        .addField("Map", match.getMap(), true)
                        .addField("Begin", this.dateFormat.format(new Date(match.getBeginTimestamp())), true)
                        .addField("ID", match.getServerId(), false)
                        .addField("Players", this.formatName(String.join(", ", match.getBeginPlayers())), false)
                        .setFooter(match.getGamemode(), "https://gomme.derrop.gq/api/textures/" + (match.getMinecraftTexturePath().replace("/", ".")))
                        .build()
        ).queue();
    }

    public void handleMatchEnd(Match match) {
        if (this.configuration.getMatchLogChannelId() < 0)
            return;
        MessageChannel channel = this.jda.getTextChannelById(this.configuration.getMatchLogChannelId());
        if (channel == null)
            return;

        long milliseconds = match.getEndTimestamp() - match.getBeginTimestamp();
        long seconds = (milliseconds / 1000) % 60;
        long minutes = ((milliseconds / (1000 * 60)) % 60);
        long hours = ((milliseconds / (1000 * 60 * 60)) % 24);

        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setTitle("**Match End**")
                .addField("GameMode", match.getGamemode(), true)
                .addField("Map", match.getMap(), true)
                .addField("Begin - End", this.dateFormat.format(new Date(match.getBeginTimestamp())) + " - " + this.dateFormat.format(new Date(match.getEndTimestamp())), true)
                .addField("Time", String.format("%02d:%02d:%02d", hours, minutes, seconds), true)
                .addField("ID", match.getServerId(), false)
                .setFooter(match.getGamemode(), "https://gomme.derrop.gq/api/textures/" + (match.getMinecraftTexturePath().replace("/", ".")));

        if (match.getWinners() != null) {
            embedBuilder.addField("Winners", String.join(", ", match.getWinners()), false);
        }

        if (match.getBeginPlayers().equals(match.getEndPlayers())) {
            embedBuilder.addField("Players", this.formatName(String.join(", ", match.getBeginPlayers())), false);
        } else {
            embedBuilder.addField("Players on Begin", this.formatName(String.join(", ", match.getBeginPlayers())), false)
                    .addField("Players on End", this.formatName(String.join(", ", match.getEndPlayers())), false);
        }

        channel.sendMessage(embedBuilder.build()).queue();
    }

    private String formatName(String name) {
        return name.replace("_", "\\_");
    }


}
