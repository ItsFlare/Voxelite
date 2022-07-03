package edu.kit.scc.git.ggd.voxelite.world;

import net.durchholz.beacon.math.AABB;
import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.math.Vec3i;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

public interface Chunk extends Iterable<Voxel> {
    int WIDTH_EXP = 5;
    int AREA_EXP  = WIDTH_EXP << 1;

    int WIDTH = 1 << WIDTH_EXP;
    int AREA  = 1 << AREA_EXP;
    int VOLUME = AREA << WIDTH_EXP;

    int MAX_WIDTH = WorldChunk.WIDTH - 1;
    int MAX_AREA  = WorldChunk.AREA - 1;
    int MAX_VOLUME = WorldChunk.VOLUME - 1;

    Vec3i EXTENT = new Vec3i(MAX_WIDTH);
    Vec3i CENTER = new Vec3i(WIDTH >> 1);
    AABB  BOUNDING_BOX = new AABB(new Vec3f(), new Vec3f(WIDTH));

    int RADIUS_SQUARED = WorldChunk.CENTER.magnitudeSq();
    float RADIUS         = (float) Math.sqrt(RADIUS_SQUARED);

    Vec3i getPosition();

    ChunkDomain getDomain();

    Block getBlock(Vec3i position);

    void setBlock(Vec3i position, Block block);

    default Voxel getVoxel(Vec3i relative) {
        return new Voxel(this, getWorldPosition()).getRelative(relative);
    }

    default boolean isOpaque(Vec3i position) {
        return getBlock(position).isOpaque();
    }

    default Vec3i getWorldPosition() {
        return toWorldPosition(getPosition());
    }

    default Vec3i getCenter() {
        return getWorldPosition().add(CENTER);
    }

    default AABB getBoundingBox() {
        return BOUNDING_BOX.translate(getWorldPosition());
    }

    CompressedLightStorage getLightStorage();

    @NotNull
    @Override
    default Iterator<Voxel> iterator() {
        return new Iterator<>() {
            private int position;

            @Override
            public boolean hasNext() {
                return position < VOLUME;
            }

            @Override
            public Voxel next() {
                return getVoxel(fromLinearSpace(position++));
            }
        };
    }

    static int toLinearSpace(Vec3i position) {
        position = toChunkSpace(position);
        return (position.x() << AREA_EXP) | (position.z() << WIDTH_EXP) | position.y();
    }

    static Vec3i fromLinearSpace(int linear) {
        return new Vec3i(x(linear), y(linear), z(linear));
    }

    static int x(int linear) {
        return linear >>> AREA_EXP;
    }

    static int y(int linear) {
        return linear & MAX_WIDTH;
    }

    static int z(int linear) {
        return (linear >>> WIDTH_EXP) & MAX_WIDTH;
    }

    static Vec3i toChunkSpace(Vec3i position) {
        return new Vec3i(
                position.x() & MAX_WIDTH,
                position.y() & MAX_WIDTH,
                position.z() & MAX_WIDTH
        );
    }

    static Vec3i toChunkPosition(Vec3i worldPosition) {
        return new Vec3i(
                worldPosition.x() >> WIDTH_EXP,
                worldPosition.y() >> WIDTH_EXP,
                worldPosition.z() >> WIDTH_EXP
        );
    }

    static Vec3i toChunkPosition(Vec3f worldPosition) {
        return toChunkPosition(toBlockPosition(worldPosition));
    }

    static Vec3i toBlockPosition(Vec3f worldPosition) {
        int x = (int) (worldPosition.x() < 0 ? Math.floor(worldPosition.x()) : worldPosition.x());
        int y = (int) (worldPosition.y() < 0 ? Math.floor(worldPosition.y()) : worldPosition.y());
        int z = (int) (worldPosition.z() < 0 ? Math.floor(worldPosition.z()) : worldPosition.z());
        return new Vec3i(x, y, z);
    }

    static Vec3i toWorldPosition(Vec3i chunkPosition) {
        return new Vec3i(
                chunkPosition.x() << WIDTH_EXP,
                chunkPosition.y() << WIDTH_EXP,
                chunkPosition.z() << WIDTH_EXP
        );
    }
}
