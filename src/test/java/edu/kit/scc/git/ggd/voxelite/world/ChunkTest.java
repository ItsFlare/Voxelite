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
        assertEquals(new Vec3i(1), Chunk.toChunkSpace(new Vec3i(17)));
        assertEquals(new Vec3i(15), Chunk.toChunkSpace(new Vec3i(-1)));
        assertEquals(new Vec3i(15), Chunk.toChunkSpace(new Vec3i(-17)));
    }

    @Test
    void toChunkPosition() {
        assertEquals(new Vec3i(0), Chunk.toChunkPosition(new Vec3i(0)));
        assertEquals(new Vec3i(0), Chunk.toChunkPosition(new Vec3i(1)));
        assertEquals(new Vec3i(0), Chunk.toChunkPosition(new Vec3i(15)));
        assertEquals(new Vec3i(1), Chunk.toChunkPosition(new Vec3i(16)));
        assertEquals(new Vec3i(-1), Chunk.toChunkPosition(new Vec3i(-1)));
        assertEquals(new Vec3i(-1), Chunk.toChunkPosition(new Vec3i(-16)));

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
        assertEquals(new Vec3i(16), Chunk.toWorldPosition(new Vec3i(1)));
        assertEquals(new Vec3i(-16), Chunk.toWorldPosition(new Vec3i(-1)));
    }
}