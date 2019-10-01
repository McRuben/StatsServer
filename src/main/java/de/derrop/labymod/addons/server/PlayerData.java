package de.derrop.labymod.addons.server;
/*
 * Created by derrop on 29.09.2019
 */

import lombok.*;

import java.util.Map;
import java.util.UUID;

@ToString
@EqualsAndHashCode
@Getter
@Setter
@AllArgsConstructor
public class PlayerData {

    private UUID uniqueId;
    private String name;
    private String gamemode;
    private Map<String, String> stats;
    private String lastMatchId;
    private long timestamp;

}
