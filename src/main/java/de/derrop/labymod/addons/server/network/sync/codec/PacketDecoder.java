package de.derrop.labymod.addons.server.network.sync.codec;
/*
 * Created by derrop on 14.10.2019
 */

import de.derrop.labymod.addons.server.network.NetworkUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class PacketDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        out.add(NetworkUtils.readString(in));
    }
}
