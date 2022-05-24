package edu.kit.scc.git.ggd.voxelite.world;

import edu.kit.scc.git.ggd.voxelite.util.ParallelRunner;
import edu.kit.scc.git.ggd.voxelite.world.generator.WorldGenerator;
import net.durchholz.beacon.math.Vec3i;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

public class AsyncChunkLoader extends ParallelRunner {

    private final WorldGenerator       generator;
    private final BlockingQueue<Vec3i> pending;
    private final BlockingQueue<Chunk> finished;

    public AsyncChunkLoader(WorldGenerator generator, BlockingQueue<Vec3i> pending, int bufferLimit, int parallelism) {
        super("AsyncChunkLoader", parallelism);
        this.generator = generator;
        this.pending = pending;
        this.finished = new LinkedBlockingQueue<>(bufferLimit);
    }

    @Override
    protected void run() throws Exception {
        final Chunk chunk = generator.generate(pending.take());
        finished.put(chunk);
    }

    public void consume(Consumer<Chunk> consumer, int limit) {
        Chunk c;
        int i = 0;
        while (i++ < limit && (c = finished.poll()) != null) {
            consumer.accept(c);
        }
    }
}
