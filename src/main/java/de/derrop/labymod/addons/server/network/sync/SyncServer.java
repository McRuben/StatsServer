package de.derrop.labymod.addons.server.network.sync;
/*
 * Created by derrop on 30.09.2019
 */

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.derrop.labymod.addons.server.GommeStatsServer;
import de.derrop.labymod.addons.server.network.sync.codec.PacketDecoder;
import de.derrop.labymod.addons.server.network.sync.codec.PacketEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;

import java.io.Closeable;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class SyncServer implements Closeable {

    private final JsonParser jsonParser = new JsonParser();

    private EventLoopGroup bossGroup = Epoll.isAvailable() ? new EpollEventLoopGroup() : new NioEventLoopGroup();
    private EventLoopGroup workerGroup = Epoll.isAvailable() ? new EpollEventLoopGroup() : new NioEventLoopGroup();

    private Channel channel;

    private GommeStatsServer statsServer;

    private Map<UUID, SyncPlayer> connectedPlayers = new HashMap<>();
    private Map<Short, Collection<PacketHandler>> packetHandlers = new HashMap<>();
    private Map<Short, CompletableFuture<JsonElement>> pendingQueries = new HashMap<>();

    private ExecutorService executorService = Executors.newCachedThreadPool();

    public SyncServer(GommeStatsServer statsServer) {
        this.statsServer = statsServer;
    }

    public void init(InetSocketAddress host) {
        this.channel = new ServerBootstrap()
                .group(this.bossGroup, this.workerGroup)
                .channel(Epoll.isAvailable() ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel channel) throws Exception {
                        System.out.println("Channel connecting [" + channel.remoteAddress() + "]...");
                        channel.pipeline()
                                .addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4),
                                        new LengthFieldPrepender(4))
                                .addLast(new PacketEncoder(), new PacketDecoder())
                                .addLast(new PacketReader(new SyncPlayer(null, null, null, channel)));
                    }
                })
                .bind(host)
                .syncUninterruptibly()
                .channel();
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
        player.getChannel().writeAndFlush(packet);

        return future;
    }

    public void registerHandler(short packetId, PacketHandler packetHandler) {
        if (!this.packetHandlers.containsKey(packetId))
            this.packetHandlers.put(packetId, new ArrayList<>());
        this.packetHandlers.get(packetId).add(packetHandler);
    }

    public Map<UUID, SyncPlayer> getConnectedPlayers() {
        return connectedPlayers;
    }

    @Override
    public void close() {
        if (this.channel != null) {
            this.channel.close().syncUninterruptibly();
        }
        this.executorService.shutdownNow();
    }

    private final class PacketReader extends SimpleChannelInboundHandler<String> {
        private SyncPlayer syncPlayer;
        private boolean authorized = false;

        public PacketReader(SyncPlayer syncPlayer) {
            this.syncPlayer = syncPlayer;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, String message) throws Exception {
            SyncServer.this.executorService.execute(() -> {
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
                        ctx.channel().writeAndFlush(response).syncUninterruptibly();
                    };
                }

                if (!jsonObject.has("id")) {
                    return;
                }
                short id = jsonObject.get("id").getAsShort();

                if (!this.authorized) {
                    if (!payload.isJsonObject()) {
                        if (responseConsumer != null) {
                            JsonObject response = new JsonObject();
                            response.addProperty("success", false);
                            response.addProperty("error", "No JsonObject");
                            responseConsumer.accept(response);
                        }
                        ctx.channel().close();
                        return;
                    }
                    JsonObject authData = payload.getAsJsonObject();
                    if (!authData.has("uniqueId") || !authData.has("name") || !authData.has("token")) {
                        if (responseConsumer != null) {
                            JsonObject response = new JsonObject();
                            response.addProperty("success", false);
                            response.addProperty("error", "Missing uuid, name or token");
                            responseConsumer.accept(response);
                        }
                        ctx.channel().close();
                        return;
                    }

                    UUID uniqueId;
                    try {
                        uniqueId = UUID.fromString(authData.get("uniqueId").getAsString());
                    } catch (IllegalArgumentException exception) {
                        if (responseConsumer != null) {
                            JsonObject response = new JsonObject();
                            response.addProperty("success", false);
                            response.addProperty("error", "Invalid uuid");
                            responseConsumer.accept(response);
                        }
                        ctx.channel().close();
                        return;
                    }
                    String name = authData.get("name").getAsString();
                    String token = authData.get("token").getAsString();

                    if (SyncServer.this.connectedPlayers.containsKey(uniqueId)) {
                        if (responseConsumer != null) {
                            JsonObject response = new JsonObject();
                            response.addProperty("success", false);
                            response.addProperty("error", "Already connected");
                            responseConsumer.accept(response);
                        }
                        ctx.channel().close();
                        return;
                    }

                    if (token == null || !SyncServer.this.statsServer.getDatabaseProvider().getUserAuthenticator().authUser(token, uniqueId)) {
                        if (responseConsumer != null) {
                            JsonObject response = new JsonObject();
                            response.addProperty("success", false);
                            response.addProperty("error", "Invalid token");
                            responseConsumer.accept(response);
                        }
                        ctx.channel().close();
                        return;
                    }

                    this.syncPlayer.setUniqueId(uniqueId);
                    this.syncPlayer.setName(name);

                    SyncServer.this.connectedPlayers.put(this.syncPlayer.getUniqueId(), this.syncPlayer);

                    this.authorized = true;
                    if (responseConsumer != null) {
                        JsonObject response = new JsonObject();
                        response.addProperty("success", true);
                        responseConsumer.accept(response);
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
            });
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
