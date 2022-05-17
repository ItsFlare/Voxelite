package edu.kit.scc.git.ggd.voxelite.world;

import edu.kit.scc.git.ggd.voxelite.render.RenderChunk;
import edu.kit.scc.git.ggd.voxelite.util.ParallelRunner;

import java.util.Queue;
import java.util.concurrent.BlockingQueue;

public class AsyncChunkBuilder extends ParallelRunner {

    private final BlockingQueue<RenderChunk> pending;
    private final Queue<RenderChunk>         upload;

    public AsyncChunkBuilder(BlockingQueue<RenderChunk> pending, Queue<RenderChunk> upload, int parallelism) {
        super("AsyncChunkBuilder", parallelism);
        this.pending = pending;
        this.upload = upload;
    }

    @Override
    public void run() throws Exception {
        final RenderChunk renderChunk = pending.take();
        if(renderChunk.isDirty() && renderChunk.isValid()) {
            renderChunk.build();
            upload.add(renderChunk);
        }
    }

}
