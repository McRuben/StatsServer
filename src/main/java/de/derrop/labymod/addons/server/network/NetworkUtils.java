package de.derrop.labymod.addons.server.network;
/*
 * Created by derrop on 14.10.2019
 */

import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;

public class NetworkUtils {

    private NetworkUtils() {
        throw new UnsupportedOperationException();
    }

    public static int readVarInt(ByteBuf byteBuf) {
        int numRead = 0;
        int result = 0;
        byte read;
        do {
            read = byteBuf.readByte();
            int value = (read & 0b01111111);
            result |= (value << (7 * numRead));

            numRead++;
            if (numRead > 5) {
                throw new RuntimeException("VarInt is too big");
            }
        } while ((read & 0b10000000) != 0);

        return result;
    }

    public static void writeVarInt(ByteBuf byteBuf, int value) {
        do {
            byte temp = (byte) (value & 0b01111111);
            value >>>= 7;
            if (value != 0) {
                temp |= 0b10000000;
            }
            byteBuf.writeByte(temp);
        } while (value != 0);
    }

    public static void writeString(ByteBuf byteBuf, String write) {
        byte[] values = write.getBytes(StandardCharsets.UTF_8);
        writeVarInt(byteBuf, values.length);
        byteBuf.writeBytes(values);
    }

    public static String readString(ByteBuf byteBuf) {
        int length = readVarInt(byteBuf);

        byte[] buffer = new byte[length];
        byteBuf.readBytes(buffer, 0, length);

        return new String(buffer, StandardCharsets.UTF_8);
    }

}
