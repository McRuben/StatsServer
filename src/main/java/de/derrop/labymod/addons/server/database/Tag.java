package de.derrop.labymod.addons.server.database;
/*
 * Created by derrop on 15.10.2019
 */

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class Tag {
    private TagType tagType;
    private String creator;
    private String name;
    private String tag;
    private long creationTime;
}
