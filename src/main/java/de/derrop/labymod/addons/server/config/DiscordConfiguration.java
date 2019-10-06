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
public class DiscordConfiguration {

    private boolean enabled = false;
    private String token = "YOUR_TOKEN";
    private long matchLogChannelId = -1L;
    private String commandPrefix = "!";

}
