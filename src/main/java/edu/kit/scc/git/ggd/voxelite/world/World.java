package edu.kit.scc.git.ggd.voxelite.world;

import edu.kit.scc.git.ggd.voxelite.Main;
import edu.kit.scc.git.ggd.voxelite.util.Direction;
import edu.kit.scc.git.ggd.voxelite.util.Util;
import edu.kit.scc.git.ggd.voxelite.world.event.ChunkLoadEvent;
import edu.kit.scc.git.ggd.voxelite.world.event.ChunkUnloadEvent;
import edu.kit.scc.git.ggd.voxelite.world.generator.natural.NaturalWorldGenerator;
import net.durchholz.beacon.math.Quaternion;
import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.math.Vec3i;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;

import static java.lang.Math.sin;

public class World implements ChunkDomain {
    private static final Comparator<Vec3i> DISTANCE_COMPARATOR = Comparator.comparingInt(position -> Chunk.toWorldPosition(position).subtract(new Vec3i(Main.INSTANCE.getRenderer().getCamera().getPosition())).magnitudeSq());

    private final NaturalWorldGenerator  generator;
    private final Map<Vec3i, WorldChunk> chunks = new ConcurrentHashMap<>();

    private final BlockingQueue<Vec3i> loadQueue = new LinkedBlockingQueue<>();
    private final AsyncChunkLoader<NaturalWorldGenerator>     chunkLoader;

    private boolean loadChunks = true;
    private int     radius     = 2;
    public  int     buildRate  = 5;

    private volatile Vec3i           lastChunk;
    private volatile ForkJoinTask<?> chunkMapTask;

    public World(NaturalWorldGenerator generator) {
        this.generator = generator;
        this.chunkLoader = new AsyncChunkLoader<>(generator, loadQueue, 128, 1);
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

    public WorldChunk getChunk(Vec3i chunkPosition) {
        return chunks.get(chunkPosition);
    }

    public NaturalWorldGenerator getGenerator() {
        return generator;
    }

    public Collection<WorldChunk> getChunks() {
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
        final List<Vec3i> list = getChunks().stream().map(WorldChunk::getPosition).sorted(DISTANCE_COMPARATOR).toList();
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
                for (int y = Math.max(-radius, -4); y <= Math.min(radius, 4); y++) {
                    for (int z = -radius; z <= radius; z++) {
                        expected.add(currentChunk.add(new Vec3i(x, y, z)));
                    }
                }
            }

            //Unload loadedChunks \ expected
            var toUnload = getChunks()
                    .stream()
                    .map(WorldChunk::getPosition)
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
                    .map(WorldChunk::getPosition)
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
        final WorldChunk chunk = getChunk(Chunk.toChunkPosition(position));
        if (chunk == null) return null;
        return new Voxel(chunk, position);
    }

    public int getLoadQueueSize() {
        return loadQueue.size();
    }

    public final Intersection traverse(Vec3f origin, Vec3f direction, float range) {
        return traverse(origin, origin.add(direction.normalized().scale(range)));
    }

    public final Intersection traverse(Vec3f origin, Vec3f direction, float range, Predicate<Voxel> hitPredicate) {
        return traverse(origin, origin.add(direction.normalized().scale(range)), hitPredicate);
    }

    public final Intersection traverse(Vec3f origin, Vec3f target) {
        return traverse(origin, target, voxel -> voxel.getBlock() != Block.AIR);
    }

    public final Intersection traverse(Vec3f origin, Vec3f target, Predicate<Voxel> hitPredicate) {
        /*
        Amanatides and Woo. A Fast Voxel Traversal Algorithm for Ray Tracing. 1987.
         */

        //Move slightly inwards in case coordinates are flat
        final float alpha = 1.0E-6f;
        origin = origin.interpolate(target, alpha);
        target = target.interpolate(origin, alpha);

        final double lenX = target.x() - origin.x();
        final double lenY = target.y() - origin.y();
        final double lenZ = target.z() - origin.z();

        final int signX = Double.compare(lenX, 0);
        final int signY = Double.compare(lenY, 0);
        final int signZ = Double.compare(lenZ, 0);

        final Direction dirX = signX == 1 ? Direction.NEG_X : Direction.POS_X;
        final Direction dirY = signY == 1 ? Direction.NEG_Y : Direction.POS_Y;
        final Direction dirZ = signZ == 1 ? Direction.NEG_Z : Direction.POS_Z;

        final double tDeltaX = signX == 0 ? Double.MAX_VALUE : (double) signX / lenX;
        final double tDeltaY = signY == 0 ? Double.MAX_VALUE : (double) signY / lenY;
        final double tDeltaZ = signZ == 0 ? Double.MAX_VALUE : (double) signZ / lenZ;

        double tMaxX = tDeltaX * (signX > 0 ? 1 - Util.frac(origin.x()) : Util.frac(origin.x()));
        double tMaxY = tDeltaY * (signY > 0 ? 1 - Util.frac(origin.y()) : Util.frac(origin.y()));
        double tMaxZ = tDeltaZ * (signZ > 0 ? 1 - Util.frac(origin.z()) : Util.frac(origin.z()));

        int x = (int) Math.floor(origin.x());
        int y = (int) Math.floor(origin.y());
        int z = (int) Math.floor(origin.z());

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
        } while (v == null || !hitPredicate.test(v));

        return new Intersection(v, normal);
    }

    public Vec3f getSunlightDirection() {
        return new Vec3f(Direction.NEG_Z.getAxis()).rotate(Quaternion.ofAxisAngle(new Vec3f(Direction.NEG_X.getAxis()), Main.getDayPercentage() * 360)).normalized();
    }

    public Vec3f getPhongParameters() {
        Vec3f night = new Vec3f(0.05f, 0, 0);
        Vec3f day = new Vec3f(0.4f, 0.7f, 0.1f);

        //TODO Improve curve
        float dayPercentage = Util.clamp((float) sin(2 * Math.PI * Main.getDayPercentage()) + 0.5f, 0.1f, 1);

        return night.interpolate(day, dayPercentage);
    }
}
