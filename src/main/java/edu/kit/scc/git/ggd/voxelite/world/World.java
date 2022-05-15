package edu.kit.scc.git.ggd.voxelite.world;

import edu.kit.scc.git.ggd.voxelite.Main;
import edu.kit.scc.git.ggd.voxelite.render.event.CameraMoveEvent;
import edu.kit.scc.git.ggd.voxelite.world.event.ChunkLoadEvent;
import edu.kit.scc.git.ggd.voxelite.world.event.ChunkUnloadEvent;
import edu.kit.scc.git.ggd.voxelite.world.generator.WorldGenerator;
import net.durchholz.beacon.event.EventType;
import net.durchholz.beacon.event.Listener;
import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.math.Vec3i;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public class World {

    private final Map<Vec3i, Chunk> chunks = new ConcurrentHashMap<>();
    private final WorldGenerator generator;
    private final AsyncChunkLoader chunkLoader;

    private int     radius     = 2;
    private Vec3i   lastChunk;
    private boolean loadChunks = true;

    public World(WorldGenerator generator) {
        this.generator = generator;
        this.chunkLoader = new AsyncChunkLoader(generator);
        EventType.addListener(this);
    }

    public void loadChunk(Vec3i chunkPosition) {
        chunkLoader.load(chunkPosition);
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

    public int getChunkRadius() {
        return radius;
    }

    public void setChunkRadius(int radius) {
        this.radius = radius;
    }

    public void setLoadChunks(boolean loadChunks) {
        this.loadChunks = loadChunks;
    }

    public void regenerate() {
        getChunks().stream().map(Chunk::getPosition).toList().forEach(vec3i -> {
            unloadChunk(vec3i);
            loadChunk(vec3i);
        });
    }

    public void tick() {
        chunkLoader.consume(chunk -> {
            chunks.put(chunk.getPosition(), chunk);
            new ChunkLoadEvent(chunk).fire();
        });
    }

    @Listener
    private void onCameraMove(CameraMoveEvent event) {
        if (!loadChunks) return;

        final Vec3f cameraPos = Main.INSTANCE.getRenderer().getCamera().getPosition();
        final Vec3i currentChunk = Chunk.toChunkPosition(cameraPos);

        if (!currentChunk.equals(lastChunk)) {
            final Set<Vec3i> expected = new HashSet<>((int) Math.ceil(Math.pow(radius + 1, 3)));

            //Add all expected chunks
            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        expected.add(currentChunk.add(new Vec3i(x, y, z)));
                    }
                }
            }

            //Unload loadedChunks \ expected
            getChunks()
                    .stream()
                    .map(Chunk::getPosition)
                    .filter(Predicate.not(expected::contains))
                    .forEach(this::unloadChunk);

            //Remove loaded chunks from expected
            getChunks()
                    .stream()
                    .map(Chunk::getPosition)
                    .forEach(expected::remove);

            //Load chunks
            expected.forEach(this::loadChunk);
        }

        lastChunk = currentChunk;
    }
}
