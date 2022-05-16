package edu.kit.scc.git.ggd.voxelite.world;

import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.math.Vec3i;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ChunkTest {

    @Test
    void toChunkSpace() {
        assertEquals(new Vec3i(1), Chunk.toChunkSpace(new Vec3i(1)));
        assertEquals(new Vec3i(1), Chunk.toChunkSpace(new Vec3i(Chunk.WIDTH + 1)));
        assertEquals(new Vec3i(Chunk.WIDTH - 1), Chunk.toChunkSpace(new Vec3i(-1)));
        assertEquals(new Vec3i(Chunk.WIDTH - 1), Chunk.toChunkSpace(new Vec3i(-Chunk.WIDTH - 1)));
    }

    @Test
    void toChunkPosition() {
        assertEquals(new Vec3i(0), Chunk.toChunkPosition(new Vec3i(0)));
        assertEquals(new Vec3i(0), Chunk.toChunkPosition(new Vec3i(1)));
        assertEquals(new Vec3i(0), Chunk.toChunkPosition(new Vec3i(Chunk.WIDTH - 1)));
        assertEquals(new Vec3i(1), Chunk.toChunkPosition(new Vec3i(Chunk.WIDTH)));
        assertEquals(new Vec3i(-1), Chunk.toChunkPosition(new Vec3i(-1)));
        assertEquals(new Vec3i(-1), Chunk.toChunkPosition(new Vec3i(-Chunk.WIDTH)));

        //Wrong way to calculate chunk position of a float vector
        assertNotEquals(new Vec3i(-1), Chunk.toChunkPosition(new Vec3i(new Vec3f(-0.1f))));
    }

    @Test
    void toChunkPositionFloat() {
        assertEquals(new Vec3i(-1), Chunk.toChunkPosition(new Vec3f(-0.1f)));
        assertEquals(new Vec3i(0), Chunk.toChunkPosition(new Vec3f(0)));
        assertEquals(new Vec3i(0), Chunk.toChunkPosition(new Vec3f(0.1f)));
    }

    @Test
    void toWorldPosition() {
        assertEquals(new Vec3i(0), Chunk.toWorldPosition(new Vec3i(0)));
        assertEquals(new Vec3i(Chunk.WIDTH), Chunk.toWorldPosition(new Vec3i(1)));
        assertEquals(new Vec3i(-Chunk.WIDTH), Chunk.toWorldPosition(new Vec3i(-1)));
    }

    @Test
    void toBlockPosition() {
        assertEquals(new Vec3i(0), Chunk.toBlockPosition(new Vec3f(0.1f)));
        assertEquals(new Vec3i(-1), Chunk.toBlockPosition(new Vec3f(-0.1f)));
        assertEquals(new Vec3i(-Chunk.WIDTH), Chunk.toBlockPosition(new Vec3f(-Chunk.WIDTH)));
        assertEquals(new Vec3i(-Chunk.WIDTH - 1), Chunk.toBlockPosition(new Vec3f(-Chunk.WIDTH - 0.1f)));
    }
}