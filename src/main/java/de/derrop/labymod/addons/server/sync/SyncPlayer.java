package de.derrop.labymod.addons.server.sync;
/*
 * Created by derrop on 30.09.2019
 */

import de.derrop.labymod.addons.server.Match;
import io.netty.channel.Channel;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class SyncPlayer {

    private UUID uniqueId;
    private String name;
    private Match currentMatch;
    private Channel channel;

}
