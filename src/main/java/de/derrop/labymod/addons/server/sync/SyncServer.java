package de.derrop.labymod.addons.server.sync;
/*
 * Created by derrop on 30.09.2019
 */

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import de.derrop.labymod.addons.server.GommeStatsServer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class SyncServer {

    private final JsonParser jsonParser = new JsonParser();

    private EventLoopGroup bossGroup = Epoll.isAvailable() ? new EpollEventLoopGroup() : new NioEventLoopGroup();
    private EventLoopGroup workerGroup = Epoll.isAvailable() ? new EpollEventLoopGroup() : new NioEventLoopGroup();

    private GommeStatsServer statsServer;

    private Map<UUID, SyncPlayer> connectedPlayers = new HashMap<>();
    private Map<Short, Collection<PacketHandler>> packetHandlers = new HashMap<>();
    private Map<Short, CompletableFuture<JsonElement>> pendingQueries = new HashMap<>();

    public SyncServer(GommeStatsServer statsServer) {
        this.statsServer = statsServer;
    }

    public void init(InetSocketAddress host) {
        new ServerBootstrap()
                .group(this.bossGroup, this.workerGroup)
                .channel(Epoll.isAvailable() ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel channel) throws Exception {
                        System.out.println("Channel connecting [" + channel.remoteAddress() + "]...");
                        channel.pipeline()
                                .addLast(new StringEncoder(), new StringDecoder())
                                .addLast(new PacketReader(new SyncPlayer(null, null, null, channel)));
                    }
                })
                .bind(host);
    }

    public CompletableFuture<JsonElement> sendQuery(SyncPlayer player, short packetId, JsonElement payload) {
        short queryId;
        do {
            queryId = (short) ThreadLocalRandom.current().nextInt(Short.MAX_VALUE);
        } while (this.pendingQueries.containsKey(queryId));

        CompletableFuture<JsonElement> future = new CompletableFuture<>();
        this.pendingQueries.put(queryId, future);

        JsonObject packet = new JsonObject();
        packet.addProperty("id", packetId);
        packet.addProperty("queryId", queryId);
        packet.add("payload", payload);
        player.getChannel().writeAndFlush(packet.toString());

        return future;
    }

    public void registerHandler(short packetId, PacketHandler packetHandler) {
        if (!this.packetHandlers.containsKey(packetId))
            this.packetHandlers.put(packetId, new ArrayList<>());
        this.packetHandlers.get(packetId).add(packetHandler);
    }

    private final class PacketReader extends SimpleChannelInboundHandler<String> {
        private SyncPlayer syncPlayer;
        private boolean authorized = false;

        public PacketReader(SyncPlayer syncPlayer) {
            this.syncPlayer = syncPlayer;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, String message) throws Exception {
            System.out.println("Parsing json " + message);
            JsonObject jsonObject = SyncServer.this.jsonParser.parse(message).getAsJsonObject();
            JsonElement payload = jsonObject.get("payload");

            Consumer<JsonElement> responseConsumer = null;
            if (jsonObject.has("queryId")) {
                short queryId = jsonObject.get("queryId").getAsShort();
                if (SyncServer.this.pendingQueries.containsKey(queryId)) {
                    SyncServer.this.pendingQueries.remove(queryId).complete(payload);
                    return;
                }
                responseConsumer = element -> {
                    JsonObject response = new JsonObject();
                    response.addProperty("queryId", queryId);
                    response.add("payload", element);
                    ctx.channel().writeAndFlush(response.toString());
                };
            }

            if (!jsonObject.has("id")) {
                return;
            }
            short id = jsonObject.get("id").getAsShort();

            if (!this.authorized) {
                if (!payload.isJsonObject()) {
                    ctx.channel().close();
                    return;
                }
                JsonObject authData = payload.getAsJsonObject();
                if (!authData.has("uniqueId") || !authData.has("name")) {
                    ctx.channel().close();
                    return;
                }

                this.syncPlayer.setUniqueId(UUID.fromString(authData.get("uniqueId").getAsString()));
                this.syncPlayer.setName(authData.get("name").getAsString());
                if (SyncServer.this.connectedPlayers.containsKey(this.syncPlayer.getUniqueId())) {
                    ctx.channel().close();
                    return;
                }
                SyncServer.this.connectedPlayers.put(this.syncPlayer.getUniqueId(), this.syncPlayer);

                this.authorized = true;
                if (responseConsumer != null) {
                    responseConsumer.accept(new JsonPrimitive(true));
                }
                System.out.println("Authorized player " + this.syncPlayer.getUniqueId() + "#" + this.syncPlayer.getName() + "@" + this.syncPlayer.getChannel().remoteAddress());
                return;
            }

            Collection<PacketHandler> packetHandlers = SyncServer.this.packetHandlers.get(id);
            if (packetHandlers != null && !packetHandlers.isEmpty()) {
                for (PacketHandler packetHandler : packetHandlers) {
                    packetHandler.handlePacket(this.syncPlayer, payload, responseConsumer);
                }
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            if (this.syncPlayer.getUniqueId() != null) {
                SyncServer.this.connectedPlayers.remove(this.syncPlayer.getUniqueId());

                if (this.syncPlayer.getCurrentMatch() != null) {
                    this.syncPlayer.getCurrentMatch().getEndPlayers().remove(this.syncPlayer.getName());
                    this.syncPlayer.getCurrentMatch().getTemporaryPlayers().remove(this.syncPlayer.getName());
                    if (this.syncPlayer.getCurrentMatch().getTemporaryPlayers().isEmpty()) {
                        SyncServer.this.statsServer.endMatchNotFinished(this.syncPlayer.getCurrentMatch());
                    }
                }
            }
        }
    }

}
