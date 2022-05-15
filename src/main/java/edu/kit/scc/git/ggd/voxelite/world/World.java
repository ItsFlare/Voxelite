package edu.kit.scc.git.ggd.voxelite.world;

import edu.kit.scc.git.ggd.voxelite.Main;
import edu.kit.scc.git.ggd.voxelite.world.event.ChunkLoadEvent;
import edu.kit.scc.git.ggd.voxelite.world.event.ChunkUnloadEvent;
import edu.kit.scc.git.ggd.voxelite.world.generator.WorldGenerator;
import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.math.Vec3i;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;

public class World {
    public static final Comparator<Vec3i> DISTANCE_COMPARATOR = Comparator.comparingInt(position -> Chunk.toWorldPosition(position).subtract(new Vec3i(Main.INSTANCE.getRenderer().getCamera().getPosition())).magnitudeSq());

    private final Map<Vec3i, Chunk>    chunks    = new ConcurrentHashMap<>();
    private final BlockingQueue<Vec3i> loadQueue = new LinkedBlockingQueue<>();
    private final WorldGenerator       generator;
    private final AsyncChunkLoader chunkLoader;

    private int     radius     = 2;
    private volatile Vec3i   lastChunk;
    private boolean loadChunks = true;
    private volatile ForkJoinTask<?> chunkMapTask;

    public World(WorldGenerator generator) {
        this.generator = generator;
        this.chunkLoader = new AsyncChunkLoader(generator, loadQueue);
    }

    public void loadChunk(Vec3i chunkPosition) {
        loadQueue.add(chunkPosition);
    }

    public void unloadChunk(Vec3i chunkPosition) {
        loadQueue.remove(chunkPosition);
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
        final List<Vec3i> list = getChunks().stream().map(Chunk::getPosition).sorted(DISTANCE_COMPARATOR).toList();
        list.forEach(this::unloadChunk);
        list.forEach(this::loadChunk);
    }

    public void tick() {
        tickChunkLoading();

        chunkLoader.consume(chunk -> {
            chunks.put(chunk.getPosition(), chunk);
            new ChunkLoadEvent(chunk).fire();
        });
    }

    private void tickChunkLoading() {
        if (!loadChunks || (chunkMapTask != null && !chunkMapTask.isDone())) return;

        final Vec3f cameraPos = Main.INSTANCE.getRenderer().getCamera().getPosition();
        final Vec3i currentChunk = Chunk.toChunkPosition(cameraPos);

        if(currentChunk.equals(lastChunk)) return;
        lastChunk = currentChunk;

        chunkMapTask = ForkJoinPool.commonPool().submit(() -> {
            final List<Vec3i> expected = new ArrayList<>((int) Math.ceil(Math.pow(radius + 1, 3)));

            //Add all expected chunks
            for (int x = -radius; x <= radius; x++) {
                for (int y = Math.max(-radius, -3); y <= Math.min(radius, 3); y++) {
                    for (int z = -radius; z <= radius; z++) {
                        expected.add(currentChunk.add(new Vec3i(x, y, z)));
                    }
                }
            }

            //Unload loadedChunks \ expected
            var toUnload = getChunks()
                    .stream()
                    .map(Chunk::getPosition)
                    .filter(Predicate.not(expected::contains))
                    .toArray(Vec3i[]::new);

            Main.INSTANCE.getExecutor().execute(() -> {
                for (Vec3i vec3i : toUnload) {
                    unloadChunk(vec3i);
                }
            });

            //Remove loaded chunks from expected
            getChunks()
                    .stream()
                    .map(Chunk::getPosition)
                    .forEach(expected::remove);

            //Load chunks
            loadQueue.clear();
            expected.sort(DISTANCE_COMPARATOR);
            loadQueue.addAll(expected);
        });
    }

    public Voxel getVoxel(Vec3f position) {
        final Chunk chunk = getChunk(Chunk.toChunkPosition(position));
        if(chunk == null) return null;
        return new Voxel(chunk, Chunk.toBlockPosition(position));
    }
}
