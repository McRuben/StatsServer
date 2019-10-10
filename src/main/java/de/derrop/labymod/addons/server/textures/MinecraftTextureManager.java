package de.derrop.labymod.addons.server.textures;
/*
 * Created by derrop on 05.10.2019
 */

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MinecraftTextureManager {

    private Path texturePackDirectory = Paths.get("assets");

    public boolean isAvailable() {
        return Files.exists(this.texturePackDirectory);
    }

    public byte[] getMinecraftTexture(String texture) {
        Path path = this.getPathFromMinecraftTexture(texture);
        if (Files.exists(path)) {
            try {
                return Files.readAllBytes(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new byte[0];
    }

    public Path getPathFromMinecraftTexture(String texture) {
        return this.texturePackDirectory.resolve("minecraft/textures").resolve(texture + ".png");
    }

    public Path getPathFromMinecraftTexturePack(String texture) {
        return this.texturePackDirectory.resolve("minecraft").resolve(texture + ".png");
    }

    public Path getPathFromTexturePack(String texture) {
        return this.texturePackDirectory.resolve(texture + ".png");
    }

}
