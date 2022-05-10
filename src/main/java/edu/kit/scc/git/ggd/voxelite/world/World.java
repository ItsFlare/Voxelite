package edu.kit.scc.git.ggd.voxelite.world;

import edu.kit.scc.git.ggd.voxelite.Main;
import edu.kit.scc.git.ggd.voxelite.util.Direction;
import edu.kit.scc.git.ggd.voxelite.world.event.ChunkLoadEvent;
import edu.kit.scc.git.ggd.voxelite.world.event.ChunkUnloadEvent;
import edu.kit.scc.git.ggd.voxelite.world.generator.WorldGenerator;
import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.math.Vec3i;

import java.util.*;
import java.util.function.Predicate;

public class World {

    private final Map<Vec3i, Chunk> chunks = new HashMap<>();
    private final WorldGenerator    generator;

    private int   radius = 2;
    private Vec3i lastChunk;

    public World(WorldGenerator generator) {
        this.generator = generator;
    }

    public void loadChunk(Vec3i chunkPosition) {
        var chunk = chunks.computeIfAbsent(chunkPosition, generator::generate);
        new ChunkLoadEvent(chunk).fire();
    }

    public void unloadChunk(Vec3i chunkPosition) {
        var chunk = chunks.remove(chunkPosition);
        if (chunk != null) new ChunkUnloadEvent(chunk).fire();
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

    public void setChunkRadius(int radius) {
        this.radius = radius;
    }

    public void tick() {
        final Vec3f cameraPos = Main.INSTANCE.getRenderer().getCamera().getPosition();
        final Vec3i currentChunk = Chunk.toChunkPosition(new Vec3i(cameraPos));

        if (!currentChunk.equals(lastChunk)) {
            final Set<Vec3i> toLoad = new HashSet<>();
            final Set<Vec3i> toUpdate = new HashSet<>();

            //Add all expected chunks
            for (int x = -radius; x < radius; x++) {
                for (int z = -radius; z < radius; z++) {
                    toLoad.add(new Vec3i(currentChunk.x() + x, 0, currentChunk.z() + z));
                }
            }

            var main = Main.INSTANCE;

            //loadedChunks \ expected
            var toUnload = getChunks()
                    .stream()
                    .map(Chunk::getPosition)
                    .filter(Predicate.not(toLoad::contains))
                    .toList();

            //Unload chunks
            for (Vec3i v : toUnload) {
                unloadChunk(v);

                //Update all chunks neighbouring unloading chunks
                for (Direction direction : Direction.values()) {
                    toUpdate.add(v.add(direction.getAxis()));
                }
            }

            //Remove loaded chunks from toLoad
            getChunks()
                    .stream()
                    .map(Chunk::getPosition)
                    .forEach(toLoad::remove);


            //Load chunks
            for (Vec3i v : toLoad) {
                loadChunk(v);

                //Update all chunks neighbouring loading chunks
                for (Direction direction : Direction.values()) {
                    toUpdate.add(v.add(direction.getAxis()));
                }
            }

            //Update chunks
            toUpdate.addAll(toLoad);
            toUpdate
                    .stream()
                    .map(vec3i -> main.getRenderer().getWorldRenderer().getRenderChunk(vec3i))
                    .filter(Objects::nonNull)
                    .forEach(renderChunk -> renderChunk.updateMesh(cameraPos));
        }

        lastChunk = currentChunk;
    }
}
