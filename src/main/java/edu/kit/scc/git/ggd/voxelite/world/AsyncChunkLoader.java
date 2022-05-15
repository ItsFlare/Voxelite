package edu.kit.scc.git.ggd.voxelite.world;

import edu.kit.scc.git.ggd.voxelite.world.generator.WorldGenerator;
import net.durchholz.beacon.math.Vec3i;

import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

//TODO Check thread safety
public class AsyncChunkLoader {
    private final WorldGenerator generator;
    private final Queue<Chunk>   finished = new ConcurrentLinkedQueue<>();

    public AsyncChunkLoader(WorldGenerator generator, BlockingQueue<Vec3i> expected) {
        this.generator = generator;

        for (int i = 0; i < 4; i++) {
            final Thread thread = new Thread(() -> {
                try {
                    while (!Thread.interrupted()) {
                        finished.add(generator.generate(expected.take()));
                    }
                } catch (InterruptedException ignored) {

                }
            });
            thread.setDaemon(true);
            thread.start();
        }
    }

    public void consume(Consumer<Chunk> consumer) {
        Chunk c;
        while ((c = finished.poll()) != null) {
            consumer.accept(c);
        }
    }
}
