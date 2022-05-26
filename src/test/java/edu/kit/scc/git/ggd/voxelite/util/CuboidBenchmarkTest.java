package edu.kit.scc.git.ggd.voxelite.util;

import edu.kit.scc.git.ggd.voxelite.test.AbstractBenchmarkTest;
import edu.kit.scc.git.ggd.voxelite.world.Chunk;
import net.durchholz.beacon.math.Vec3i;
import org.junit.jupiter.api.Disabled;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

@Warmup(iterations = 1)
@Measurement(iterations = 1)
@Fork(value = 1, warmups = 1)
@Disabled
public class CuboidBenchmarkTest extends AbstractBenchmarkTest {
    @Override
    public void run(Blackhole blackhole) {
        for (Vec3i vec3i : Util.cuboid(new Vec3i(), Chunk.EXTENT)) {
            blackhole.consume(vec3i);
        }
    }
}
