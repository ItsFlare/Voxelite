package edu.kit.scc.git.ggd.voxelite.world;

import edu.kit.scc.git.ggd.voxelite.Main;
import edu.kit.scc.git.ggd.voxelite.render.RenderChunk;
import edu.kit.scc.git.ggd.voxelite.util.Direction;
import net.durchholz.beacon.math.AABB;
import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.math.Vec3i;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;

public class Chunk implements Iterable<Voxel> {

    public static final int   WIDTH_EXP    = 5;
    public static final int   HEIGHT_EXP   = 5;
    public static final int   AREA_EXP     = WIDTH_EXP << 1;
    public static final int   WIDTH        = 1 << WIDTH_EXP;
    public static final int   HEIGHT       = 1 << HEIGHT_EXP;
    public static final int   AREA         = 1 << AREA_EXP;
    public static final int   VOLUME       = AREA << HEIGHT_EXP;
    public static final Vec3i CENTER       = new Vec3i(WIDTH >> 1, HEIGHT >> 1, WIDTH >> 1);
    public static final AABB  BOUNDING_BOX = new AABB(new Vec3f(), WIDTH, HEIGHT, WIDTH);


    private final World                  world;
    private final Vec3i                  position;
    private final AABB                   boundingBox;
    private final BlockStorage           blockStorage = new CompressedBlockStorage();
    private final CompressedLightStorage lightStorage = new CompressedLightStorage();

    private int blockCount = 0;


    public Chunk(World world, Vec3i position) {
        this.world = world;
        this.position = position;
        this.boundingBox = BOUNDING_BOX.translate(position);
    }

    public Voxel getVoxel(Vec3i position) {
        return new Voxel(this, position);
    }

    public Block getBlock(Vec3i position) {
        return blockStorage.getBlock(toLinearSpace(position));
    }

    public void setBlock(final Vec3i position, final Block block) {
        final Voxel voxel = getVoxel(position);
        final int linear = toLinearSpace(position);
        final Block previous = blockStorage.getBlock(linear);

        blockStorage.setBlock(linear, block);
        ForkJoinPool.commonPool().submit(() -> {
            lightStorage.calculate(voxel, previous);
        });

        //TODO Optimize?
        if (previous == Block.AIR && block != Block.AIR) blockCount += 1;
        else if (previous != Block.AIR && block == Block.AIR) blockCount -= 1;

        //Store chunks to update
        final Set<Vec3i> chunks = new HashSet<>();

        //Add current chunk
        chunks.add(this.position);

        //Add neighboring chunks
        for (Direction direction : Direction.values()) {
            final Voxel neighbor = voxel.getNeighbor(direction);
            if (neighbor != null) chunks.add(neighbor.chunk().position);
        }

        //TODO Causes slight flicker because of rebuild during light update
        //Update all affected chunks
        for (Vec3i chunkPosition : chunks) {
            final RenderChunk renderChunk = Main.INSTANCE.getRenderer().getWorldRenderer().getRenderChunk(chunkPosition);
            if (renderChunk != null) Main.INSTANCE.getRenderer().getWorldRenderer().queueRebuild(renderChunk); //TODO Make neater
        }
    }

    public CompressedLightStorage getLightStorage() {
        return lightStorage;
    }

    public World getWorld() {
        return world;
    }

    public Vec3i getPosition() {
        return position;
    }

    public AABB getBoundingBox() {
        return boundingBox;
    }

    public int getBlockCount() {
        return blockCount;
    }

    @Override
    public Iterator<Voxel> iterator() {
        return new Iterator<>() {
            private int position;

            @Override
            public boolean hasNext() {
                return position < VOLUME;
            }

            @Override
            public Voxel next() {
                return getVoxel(fromLinearSpace(position++).add(toWorldPosition(Chunk.this.position)));
            }
        };
    }

    public static int toLinearSpace(Vec3i position) {
        position = toChunkSpace(position);
        return (position.x() << AREA_EXP) + (position.z() << WIDTH_EXP) + position.y(); //TODO Replace with bitwise or
    }

    public static Vec3i fromLinearSpace(int index) {
        //TODO Optimize
        int x = index / AREA;
        int y = (index % AREA) % HEIGHT;
        int z = (index % AREA) / WIDTH;
        return new Vec3i(x, y, z);
    }

    public static Vec3i toChunkSpace(Vec3i position) {
        return new Vec3i(
                position.x() & (WIDTH - 1),
                position.y() & (HEIGHT - 1),
                position.z() & (WIDTH - 1)
        );
    }

    public static Vec3i toChunkPosition(Vec3i worldPosition) {
        return new Vec3i(
                worldPosition.x() >> WIDTH_EXP,
                worldPosition.y() >> HEIGHT_EXP,
                worldPosition.z() >> WIDTH_EXP
        );
    }

    public static Vec3i toChunkPosition(Vec3f worldPosition) {
        return toChunkPosition(toBlockPosition(worldPosition));
    }

    public static Vec3i toBlockPosition(Vec3f worldPosition) {
        int x = (int) (worldPosition.x() < 0 ? Math.floor(worldPosition.x()) : worldPosition.x());
        int y = (int) (worldPosition.y() < 0 ? Math.floor(worldPosition.y()) : worldPosition.y());
        int z = (int) (worldPosition.z() < 0 ? Math.floor(worldPosition.z()) : worldPosition.z());
        return new Vec3i(x, y, z);
    }

    public static Vec3i toWorldPosition(Vec3i chunkPosition) {
        return new Vec3i(
                chunkPosition.x() << WIDTH_EXP,
                chunkPosition.y() << HEIGHT_EXP,
                chunkPosition.z() << WIDTH_EXP
        );
    }
}
