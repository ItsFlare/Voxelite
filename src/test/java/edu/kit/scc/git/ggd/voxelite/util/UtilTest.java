package edu.kit.scc.git.ggd.voxelite.util;

import edu.kit.scc.git.ggd.voxelite.world.Chunk;
import net.durchholz.beacon.math.Vec3i;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class UtilTest {

    @Test
    void cuboid() {
        testCuboid(new Vec3i(), new Vec3i());

        testCuboid(new Vec3i(), Chunk.EXTENT);
        testCuboid(Chunk.EXTENT, new Vec3i());

        testCuboid(new Vec3i(-Chunk.MAX_WIDTH), new Vec3i(0));
        testCuboid(new Vec3i(0), new Vec3i(-Chunk.MAX_WIDTH));
    }

    private static void testCuboid(Vec3i a, Vec3i b) {
        final Vec3i origin = Vec3i.min(a, b);
        final Vec3i target = Vec3i.max(a, b);

        List<Vec3i> visited = new ArrayList<>();

        for (Vec3i vec3i : Util.cuboid(a, b)) {
            visited.add(vec3i);
        }

        int i = 0;
        for (int x = origin.x(); x <= target.x(); x++) {
            for (int z = origin.z(); z <= target.z(); z++) {
                for (int y = origin.y(); y <= target.y(); y++) {
                    assertEquals(new Vec3i(x, y, z), visited.get(i++));
                }
            }
        }

        final Vec3i diff = target.subtract(origin).add(1);
        assertEquals(diff.x() * diff.y() * diff.z(), visited.size());
    }
}