package edu.kit.scc.git.ggd.voxelite.world;

import edu.kit.scc.git.ggd.voxelite.world.generator.WorldGenerator;
import net.durchholz.beacon.math.Vec3i;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

//TODO Check thread safety
public class AsyncChunkLoader {
    private final WorldGenerator generator;
    private final Queue<Chunk>   finished = new ConcurrentLinkedQueue<>();

    public AsyncChunkLoader(WorldGenerator generator) {
        this.generator = generator;
    }

    public CompletableFuture<Void> load(Vec3i position) {
        return CompletableFuture.supplyAsync(() -> generator.generate(position)).thenAccept(finished::add);
    }

    public void consume(Consumer<Chunk> consumer) {
        Chunk c;
        while ((c = finished.poll()) != null) {
            consumer.accept(c);
        }
    }
}
