package net.defade.towerbow.map;

import net.defade.minestom.amethyst.AmethystSource;
import org.jetbrains.annotations.Nullable;
import java.io.InputStream;
import java.io.OutputStream;

public class LobbyAmethystSource implements AmethystSource {
    @Override
    public @Nullable InputStream getAmethystSource() {
        return getClass().getClassLoader().getResourceAsStream("lobby.amethyst");
    }

    @Override
    public @Nullable OutputStream getAmethystOutput() {
        return null; // We don't want to save
    }
}
