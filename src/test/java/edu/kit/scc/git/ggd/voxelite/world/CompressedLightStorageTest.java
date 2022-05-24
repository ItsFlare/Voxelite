package edu.kit.scc.git.ggd.voxelite.world;

import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.math.Vec3i;
import org.junit.jupiter.api.Test;

import static edu.kit.scc.git.ggd.voxelite.world.CompressedLightStorage.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CompressedLightStorageTest {

    @Test
    void max() {
        assertEquals(15, CompressedLightStorage.max(0xf));
        assertEquals(15, CompressedLightStorage.max(0x1e7));
        assertEquals(7, CompressedLightStorage.max(0x300007));
        assertEquals(7, CompressedLightStorage.max(0x700003));

        final Vec3f color = new Vec3f(1, 0.5f, 0);
        for (int i = 0; i < RANGE; i++) {
            assertEquals(i, CompressedLightStorage.max(encode(color, i)));
        }
    }

    @Test
    void decrement() {
        for (int i = 0; i < LightStorage.RANGE; i++) {
            int a = encode(i, i, i);
            int dec = SIMD.decrement(a);

            assertEquals(new Vec3i(i), decode(a));
            assertEquals(new Vec3i(Math.max(0, i - 1)), decode(dec));
        }
    }

}