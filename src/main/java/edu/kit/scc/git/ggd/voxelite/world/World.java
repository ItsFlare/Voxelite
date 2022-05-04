package edu.kit.scc.git.ggd.voxelite.world;

import edu.kit.scc.git.ggd.voxelite.world.event.ChunkLoadEvent;
import edu.kit.scc.git.ggd.voxelite.world.event.ChunkUnloadEvent;
import edu.kit.scc.git.ggd.voxelite.world.generator.WorldGenerator;
import net.durchholz.beacon.math.Vec3i;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class World {

    private final Map<Vec3i, Chunk> chunks = new HashMap<>();
    private final WorldGenerator    generator;

    public World(WorldGenerator generator) {
        this.generator = generator;
    }

    public void loadChunk(Vec3i chunkPosition) {
        var chunk = chunks.computeIfAbsent(chunkPosition, generator::generate);
        new ChunkLoadEvent(chunk).fire();
    }

    public void unloadChunk(Vec3i chunkPosition) {
        var chunk = chunks.remove(chunkPosition);
        if(chunk != null) new ChunkUnloadEvent(chunk).fire();
    }

    public Chunk getChunk(Vec3i chunkPosition) {
        return chunks.get(chunkPosition);
    }

    public WorldGenerator getGenerator() {
        return generator;
    }

    public Collection<Chunk> getChunks() {
        return chunks.values();
    }
}
