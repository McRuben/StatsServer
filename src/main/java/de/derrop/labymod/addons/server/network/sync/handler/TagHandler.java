package de.derrop.labymod.addons.server.network.sync.handler;
/*
 * Created by derrop on 10.10.2019
 */

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import de.derrop.labymod.addons.server.GommeStatsServer;
import de.derrop.labymod.addons.server.database.TagType;
import de.derrop.labymod.addons.server.network.sync.PacketHandler;
import de.derrop.labymod.addons.server.network.sync.SyncPlayer;

import java.util.function.Consumer;

public class TagHandler implements PacketHandler {
    private GommeStatsServer statsServer;

    public TagHandler(GommeStatsServer statsServer) {
        this.statsServer = statsServer;
    }

    @Override
    public void handlePacket(SyncPlayer player, JsonElement payload, Consumer<JsonElement> responseConsumer) {
        if (!payload.isJsonObject())
            return;

        try {
            TagType tagType = TagType.valueOf(payload.getAsJsonObject().get("type").getAsString());
            String name = payload.getAsJsonObject().get("name").getAsString();
            String queryType = payload.getAsJsonObject().get("query").getAsString();

            if (name == null || queryType == null) {
                responseConsumer.accept(JsonNull.INSTANCE);
                return;
            }

            if (queryType.equals("list") && responseConsumer != null) {
                responseConsumer.accept(
                        this.statsServer.getGson().toJsonTree(
                                this.statsServer.getDatabaseProvider().getTagProvider().listTags(tagType, name)
                        )
                );
            } else if (queryType.equals("add")) {
                boolean success = this.statsServer.getDatabaseProvider().getTagProvider().addTag(tagType, player.getUniqueId() + "#" + player.getName(), name, payload.getAsJsonObject().get("tag").getAsString());
                if (responseConsumer != null) {
                    responseConsumer.accept(new JsonPrimitive(success));
                }
            } else if (queryType.equals("remove")) {
                this.statsServer.getDatabaseProvider().getTagProvider().removeTag(tagType, name, payload.getAsJsonObject().get("tag").getAsString());
            }
        } catch (Exception e) {
            if (responseConsumer != null) {
                JsonObject response = new JsonObject();
                response.addProperty("error", e.getClass().getName());
                response.addProperty("message", e.getMessage());
                responseConsumer.accept(response);
            }
        }
    }
}
