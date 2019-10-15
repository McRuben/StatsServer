package de.derrop.labymod.addons.server.network.sync;
/*
 * Created by derrop on 30.09.2019
 */

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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

    public void sendPacket(short packetId, JsonElement payload) {
        JsonObject packet = new JsonObject();
        packet.addProperty("id", packetId);
        packet.add("payload", payload);
        this.send(packet);
    }

    void send(JsonElement packet) {
        System.out.println("Sending to " + this.uniqueId + "#" + this.name + ": " + packet);
        this.channel.writeAndFlush(packet);
    }

}
