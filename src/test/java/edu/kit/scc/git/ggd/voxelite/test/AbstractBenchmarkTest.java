package edu.kit.scc.git.ggd.voxelite.test;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 2)
@Fork(value = 1, warmups = 1)
@Tag("benchmark")
public abstract class AbstractBenchmarkTest {
    @Benchmark
    public final void benchmark(Blackhole blackhole) throws Exception {
        run(blackhole);
    }

    public abstract void run(Blackhole blackhole) throws Exception;

    @Test
    public void test() throws Exception {
        BenchmarkManager.run(getClass(), threshold());
    }

    public float threshold() {
        return 1.1f;
    }
}
