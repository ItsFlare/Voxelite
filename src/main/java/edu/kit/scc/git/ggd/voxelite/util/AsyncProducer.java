package edu.kit.scc.git.ggd.voxelite.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

public class AsyncProducer<T> implements Supplier<T> {
    private final Supplier<T> task;
    private final Executor    executor;

    private volatile T result;

    private CompletableFuture<Void> updateTask;

    public AsyncProducer(Supplier<T> task, Executor executor) {
        this.task = task;
        this.executor = executor;
    }

    @Override
    public T get() {
        if (updateTask == null || updateTask.isDone()) {
            if (updateTask == null) result = task.get();
            updateTask = CompletableFuture.runAsync(() -> this.result = task.get(), executor);
        }

        return result;
    }
}
