package de.derrop.labymod.addons.server;
/*
 * Created by derrop on 29.09.2019
 */

import lombok.*;

import java.util.Collection;

@ToString
@EqualsAndHashCode
@Getter
@Setter
@AllArgsConstructor
public class Match {

    private String map;
    private String gamemode;
    private String serverId;
    private long beginTimestamp;
    private long endTimestamp;
    private boolean finished; //did every player leave before the match even ended?
    private Collection<String> winners;
    private Collection<String> beginPlayers;
    private Collection<String> endPlayers;
    private transient Collection<String> temporaryPlayers;

}
