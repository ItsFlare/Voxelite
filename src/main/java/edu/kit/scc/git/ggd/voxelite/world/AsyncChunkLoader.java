package edu.kit.scc.git.ggd.voxelite.world;

import edu.kit.scc.git.ggd.voxelite.Main;
import edu.kit.scc.git.ggd.voxelite.util.ParallelRunner;
import edu.kit.scc.git.ggd.voxelite.world.generator.GeneratorChunk;
import edu.kit.scc.git.ggd.voxelite.world.generator.MultiPassGenerator;
import edu.kit.scc.git.ggd.voxelite.world.generator.natural.pass.GeneratorPass;
import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.math.Vec3i;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

public class AsyncChunkLoader<G extends MultiPassGenerator<G>> extends ParallelRunner implements ChunkDomain {

    private final G                             generator;
    private final BlockingQueue<Vec3i>          pending;
    private final BlockingQueue<WorldChunk>     finished;
    private final Map<Vec3i, GeneratorChunk<G>> chunks = Collections.synchronizedMap(new HashMap<>());

    public AsyncChunkLoader(G generator, BlockingQueue<Vec3i> pending, int bufferLimit, int parallelism) {
        super("AsyncChunkLoader", parallelism);
        this.generator = generator;
        this.pending = pending;
        this.finished = new LinkedBlockingQueue<>(bufferLimit);
    }

    @Override
    public GeneratorChunk<G> getChunk(Vec3i position) {
        return chunks.get(position);
    }

    @Override
    protected void run() throws Exception {
        Vec3i position;
        while ((position = pending.poll()) != null) {
            final GeneratorChunk<G> chunk = new GeneratorChunk<>(generator, this, position);
            assert chunk.getPass().isFirst();
            chunks.putIfAbsent(position, chunk);
        }

        Vec3f cameraPosition = Main.INSTANCE.getRenderer().getCamera().getPosition();
        Vec3f cameraDirection = Main.INSTANCE.getRenderer().getCamera().getDirection();
        Vec3i cameraChunkPos = Chunk.toChunkPosition(cameraPosition);

        List<GeneratorChunk<G>> sorted;
        synchronized (chunks) {
            sorted = chunks
                    .values()
                    .stream()
                    .sorted(
                            Comparator.<GeneratorChunk<G>>comparingInt(c -> ringDistance(c.getPosition(), cameraChunkPos))
                                    .thenComparingDouble(chunk -> chunk.getCenter().subtract(cameraPosition).normalized().dot(cameraDirection))
                    )
                    .toList();
        }


        for (GeneratorChunk<G> chunk : sorted) {
            if (chunk.getLock().tryLock()) {
                try {
                    if (isReady(chunk)) chunk.generate();

                    if (chunk.getPass() == null) {
                        //TODO Assert neighbors are not dependent anymore
                        if (finished.offer(new WorldChunk(chunk))) chunks.remove(chunk.getPosition());
                    }
                } finally {
                    chunk.getLock().unlock();
                }
            }
        }
    }

    public void consume(Consumer<WorldChunk> consumer, int limit) {
        WorldChunk c;
        int i = 0;
        while (i++ < limit && (c = finished.poll()) != null) {
            consumer.accept(c);
        }
    }

    private boolean isReady(GeneratorChunk<G> chunk) {
        final var pass = chunk.getPass();
        if (pass == null) return false;
        if (pass.isFirst()) return true;

        //Check if all neighbors are in parent pass
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    final Vec3i position = chunk.getPosition().add(new Vec3i(x, y, z));
                    if (Main.INSTANCE.getWorld().getChunk(position) != null) continue;

                    final var neighbor = getChunk(position);
                    if (neighbor == null) return false;
                    final GeneratorPass<G> neighborPass = neighbor.getPass();
                    if (neighborPass != null && neighborPass.ordinal() < pass.ordinal()) return false;
                }
            }
        }
        return true;
    }

    public static int ringDistance(Vec3i a, Vec3i b) {
        return a.subtract(b).abs().max();
    }
}
