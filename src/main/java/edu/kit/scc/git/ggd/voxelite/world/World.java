package edu.kit.scc.git.ggd.voxelite.world;

import edu.kit.scc.git.ggd.voxelite.Main;
import edu.kit.scc.git.ggd.voxelite.util.Direction;
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
        return traverse(origin, origin.add(direction.normalized().scale(range)));
    }

    public Intersection traverse(Vec3f origin, Vec3f target) {
        double d0 = lerp(-1.0E-7D, target.x(), origin.x());
        double d1 = lerp(-1.0E-7D, target.y(), origin.y());
        double d2 = lerp(-1.0E-7D, target.z(), origin.z());
        double d3 = lerp(-1.0E-7D, origin.x(), target.x());
        double d4 = lerp(-1.0E-7D, origin.y(), target.y());
        double d5 = lerp(-1.0E-7D, origin.z(), target.z());
        int x = floor(d3);
        int y = floor(d4);
        int z = floor(d5);

        double xLen = d0 - d3;
        double yLen = d1 - d4;
        double zLen = d2 - d5;
        int signX = sign(xLen);
        int signY = sign(yLen);
        int signZ = sign(zLen);
        double d9 = signX == 0 ? Double.MAX_VALUE : (double) signX / xLen;
        double d10 = signY == 0 ? Double.MAX_VALUE : (double) signY / yLen;
        double d11 = signZ == 0 ? Double.MAX_VALUE : (double) signZ / zLen;
        double xt = d9 * (signX > 0 ? 1.0D - frac(d3) : frac(d3));
        double yt = d10 * (signY > 0 ? 1.0D - frac(d4) : frac(d4));
        double zt = d11 * (signZ > 0 ? 1.0D - frac(d5) : frac(d5));

        Voxel v;
        int normal;

        do {
            if (xt > 1.0D && yt > 1.0D && zt > 1.0D) {
                return null;
            }

            if (xt < yt) {
                if (xt < zt) {
                    x += signX;
                    xt += d9;
                    normal = 0;
                } else {
                    z += signZ;
                    zt += d11;
                    normal = 2;
                }
            } else if (yt < zt) {
                y += signY;
                yt += d10;
                normal = 1;
            } else {
                z += signZ;
                zt += d11;
                normal = 2;
            }

            v = getVoxel(new Vec3i(x, y, z));
        } while (v == null || v.getBlock() == Block.AIR);

        Direction normalDirection = switch (normal) {
            case 0 -> signX == 1 ? Direction.NEG_X : Direction.POS_X;
            case 1 -> signY == 1 ? Direction.NEG_Y : Direction.POS_Y;
            case 2 -> signZ == 1 ? Direction.NEG_Z : Direction.POS_Z;
            default -> throw new IllegalStateException("Unexpected value: " + normal);
        };

        return new Intersection(v, normalDirection);
    }

    public static double lerp(double d0, double d1, double d2) {
        return d1 + d0 * (d2 - d1);
    }

    public static double frac(double d0) {
        return d0 - (double) lfloor(d0);
    }

    public static long lfloor(double d0) {
        long i = (long) d0;

        return d0 < (double) i ? i - 1L : i;
    }
    public static int sign(double d0) {
        return d0 == 0.0D ? 0 : (d0 > 0.0D ? 1 : -1);
    }
    public static int floor(double d0) {
        int i = (int) d0;

        return d0 < (double) i ? i - 1 : i;
    }
}
