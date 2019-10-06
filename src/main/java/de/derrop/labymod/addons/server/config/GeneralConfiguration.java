package de.derrop.labymod.addons.server.config;
/*
 * Created by derrop on 05.10.2019
 */

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
public class GeneralConfiguration {

    private int webPort = 2410;
    private int minecraftPort = 1510;
    private DiscordConfiguration discordConfiguration = new DiscordConfiguration();

}
