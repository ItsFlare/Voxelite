package edu.kit.scc.git.ggd.voxelite.util;

import net.durchholz.beacon.util.BeaconExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

public class VoxeliteExecutor implements BeaconExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(VoxeliteExecutor.class);

    private final Queue<Job<?>> queue = new ConcurrentLinkedQueue<>();

    @Override
    public <T> CompletableFuture<T> execute(Supplier<T> supplier) {
        CompletableFuture<T> future = new CompletableFuture<>();
        queue.add(new Job<>(future, supplier));
        return future;
    }

    public void process() {
        Job<?> job;
        while ((job = queue.poll()) != null) {
            try {
                job.run();
            } catch (RuntimeException e) {
                LOGGER.error("Failed to process queued job %s".formatted(job.toString()), e);
            }
        }
    }

    public void process(long untilNanos) {
        Job<?> job;
        while (System.nanoTime() < untilNanos && (job = queue.poll()) != null) {
            try {
                job.run();
            } catch (RuntimeException e) {
                LOGGER.error("Failed to process queued job %s".formatted(job.toString()), e);
            }
        }
    }

    private record Job<T>(CompletableFuture<T> future, Supplier<T> supplier) implements Runnable {
        public void run() {
            future.complete(supplier.get());
        }
    }
}
