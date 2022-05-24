package edu.kit.scc.git.ggd.voxelite.world;

import edu.kit.scc.git.ggd.voxelite.Main;
import edu.kit.scc.git.ggd.voxelite.util.Direction;
import edu.kit.scc.git.ggd.voxelite.util.Util;
import edu.kit.scc.git.ggd.voxelite.world.event.ChunkLoadEvent;
import edu.kit.scc.git.ggd.voxelite.world.event.ChunkUnloadEvent;
import edu.kit.scc.git.ggd.voxelite.world.generator.WorldGenerator;
import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.math.Vec3i;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;

public class World {
    private static final Comparator<Vec3i> DISTANCE_COMPARATOR = Comparator.comparingInt(position -> Chunk.toWorldPosition(position).subtract(new Vec3i(Main.INSTANCE.getRenderer().getCamera().getPosition())).magnitudeSq());

    private final WorldGenerator    generator;
    private final Map<Vec3i, Chunk> chunks = new ConcurrentHashMap<>();

    private final BlockingQueue<Vec3i> loadQueue = new LinkedBlockingQueue<>();
    private final AsyncChunkLoader     chunkLoader;

    private boolean loadChunks = true;
    private int     radius     = 2;
    public  int     buildRate  = 5;

    private volatile Vec3i           lastChunk;
    private volatile ForkJoinTask<?> chunkMapTask;

    public World(WorldGenerator generator) {
        this.generator = generator;
        this.chunkLoader = new AsyncChunkLoader(generator, loadQueue, 128, ForkJoinPool.getCommonPoolParallelism() / 4 + 1);
        this.chunkLoader.start();
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
    }

    public void frame() {
        chunkLoader.consume(chunk -> {
            if (chunks.putIfAbsent(chunk.getPosition(), chunk) == null) new ChunkLoadEvent(chunk).fire();
        }, buildRate);
    }

    private void tickChunkLoading() {
        if (!loadChunks || (chunkMapTask != null && !chunkMapTask.isDone())) return;

        final Vec3f cameraPos = Main.INSTANCE.getRenderer().getCamera().getPosition();
        final Vec3i currentChunk = Chunk.toChunkPosition(cameraPos);

        if (currentChunk.equals(lastChunk)) return;
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
            expected.sort(DISTANCE_COMPARATOR);
            loadQueue.clear();
            loadQueue.addAll(expected);
        });
    }

    public Voxel getVoxel(Vec3f position) {
        return getVoxel(Chunk.toBlockPosition(position));
    }

    public Voxel getVoxel(Vec3i position) {
        final Chunk chunk = getChunk(Chunk.toChunkPosition(position));
        if (chunk == null) return null;
        return new Voxel(chunk, position);
    }

    public int getLoadQueueSize() {
        return loadQueue.size();
    }

    public Intersection traverse(Vec3f origin, Vec3f direction, float range) {
        return traverse(origin, origin.add(direction.normalized().scale(range)), block -> block != Block.AIR);
    }

    public Intersection traverse(Vec3f origin, Vec3f direction, float range, Predicate<Block> hitPredicate) {
        return traverse(origin, origin.add(direction.normalized().scale(range)), hitPredicate);
    }
    public Intersection traverse(Vec3f origin, Vec3f target) {
        return traverse(origin, target, block -> block != Block.AIR);
    }

    public Intersection traverse(Vec3f origin, Vec3f target, Predicate<Block> hitPredicate) {
        /*
        Amanatides and Woo. A Fast Voxel Traversal Algorithm for Ray Tracing. 1987.
         */

        final double d0 = Util.lerp(-1.0E-7D, target.x(), origin.x());
        final double d1 = Util.lerp(-1.0E-7D, target.y(), origin.y());
        final double d2 = Util.lerp(-1.0E-7D, target.z(), origin.z());
        final double d3 = Util.lerp(-1.0E-7D, origin.x(), target.x());
        final double d4 = Util.lerp(-1.0E-7D, origin.y(), target.y());
        final double d5 = Util.lerp(-1.0E-7D, origin.z(), target.z());

        final double lenX = d0 - d3;
        final double lenY = d1 - d4;
        final double lenZ = d2 - d5;
        final int signX = Double.compare(lenX, 0);
        final int signY = Double.compare(lenY, 0);
        final int signZ = Double.compare(lenZ, 0);
        final Direction dirX = signX == 1 ? Direction.NEG_X : Direction.POS_X;
        final Direction dirY = signY == 1 ? Direction.NEG_Y : Direction.POS_Y;
        final Direction dirZ = signZ == 1 ? Direction.NEG_Z : Direction.POS_Z;
        final double tDeltaX = signX == 0 ? Double.MAX_VALUE : (double) signX / lenX;
        final double tDeltaY = signY == 0 ? Double.MAX_VALUE : (double) signY / lenY;
        final double tDeltaZ = signZ == 0 ? Double.MAX_VALUE : (double) signZ / lenZ;

        double tMaxX = tDeltaX * (signX > 0 ? 1 - Util.frac(d3) : Util.frac(d3));
        double tMaxY = tDeltaY * (signY > 0 ? 1 - Util.frac(d4) : Util.frac(d4));
        double tMaxZ = tDeltaZ * (signZ > 0 ? 1 - Util.frac(d5) : Util.frac(d5));
        int x = Util.floor(d3);
        int y = Util.floor(d4);
        int z = Util.floor(d5);

        Voxel v;
        Direction normal;

        do {
            if (tMaxX > 1 && tMaxY > 1 && tMaxZ > 1) {
                return null;
            }

            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    x += signX;
                    tMaxX += tDeltaX;
                    normal = dirX;
                } else {
                    z += signZ;
                    tMaxZ += tDeltaZ;
                    normal = dirZ;
                }
            } else if (tMaxY < tMaxZ) {
                y += signY;
                tMaxY += tDeltaY;
                normal = dirY;
            } else {
                z += signZ;
                tMaxZ += tDeltaZ;
                normal = dirZ;
            }

            v = getVoxel(new Vec3i(x, y, z));
        } while (v == null || !hitPredicate.test(v.getBlock()));

        return new Intersection(v, normal);
    }

}
