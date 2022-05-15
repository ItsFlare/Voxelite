package edu.kit.scc.git.ggd.voxelite.render;

import edu.kit.scc.git.ggd.voxelite.Main;
import edu.kit.scc.git.ggd.voxelite.texture.TextureAtlas;
import edu.kit.scc.git.ggd.voxelite.util.Direction;
import edu.kit.scc.git.ggd.voxelite.world.event.ChunkLoadEvent;
import edu.kit.scc.git.ggd.voxelite.world.event.ChunkUnloadEvent;
import net.durchholz.beacon.event.EventType;
import net.durchholz.beacon.event.Listener;
import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.math.Vec3i;
import net.durchholz.beacon.render.opengl.OpenGL;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.PriorityBlockingQueue;

public class WorldRenderer {

    public static final Comparator<RenderChunk> DISTANCE_COMPARATOR = Comparator.comparingInt(rc -> rc.getChunk().getPosition().subtract(new Vec3i(Main.INSTANCE.getRenderer().getCamera().getPosition())).magnitudeSq());

    private final Map<Vec3i, RenderChunk> renderChunks    = new HashMap<>();
    private final List<RenderChunk>       renderChunkList = new ArrayList<>();
    private final TextureAtlas            atlas;
    private final Set<RenderChunk>        toBuild         = ConcurrentHashMap.newKeySet();
    public final  Queue<RenderChunk>      toUpload        = new PriorityBlockingQueue<>(1000, DISTANCE_COMPARATOR);

    public Vec3f lightColor      = new Vec3f(1);
    public float ambientStrength = 0.2f, diffuseStrength = 0.5f, specularStrength = 0.5f;

    public WorldRenderer() {
        EventType.addListener(this);

        try {
            atlas = new TextureAtlas("/textures/blocks");
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Listener
    private void onChunkLoad(ChunkLoadEvent event) {
        final RenderChunk renderChunk = new RenderChunk(event.chunk());
        final Vec3i position = event.chunk().getPosition();
        renderChunks.put(position, renderChunk);
        renderChunkList.add(renderChunk);
        toBuild.add(renderChunk);
        queueNeighbors(position);
    }

    @Listener
    private void onChunkUnload(ChunkUnloadEvent event) {
        final Vec3i position = event.chunk().getPosition();
        final RenderChunk renderChunk = renderChunks.remove(position);
        renderChunkList.remove(renderChunk);
        queueNeighbors(position);
        renderChunk.delete();
    }

    public void queueNeighbors(Vec3i position) {
        for (Direction direction : Direction.values()) {
            var neighbor = renderChunks.get(position.add(direction.getAxis()));
            if (neighbor != null) queueRebuild(neighbor);
        }
    }

    public void queueAll() {
        toBuild.addAll(renderChunks.values());
    }

    public void queueRebuild(RenderChunk renderChunk) {
        toBuild.add(renderChunk);
    }

    public void buildChunksAsync() {
        CompletableFuture.runAsync(() -> {
            toBuild.forEach(renderChunk -> {
                ForkJoinPool.commonPool().submit(() -> {
                    try {
                        renderChunk.build();
                    } catch (RuntimeException e) {
                        System.out.println(e);
                    }
                });
                toBuild.remove(renderChunk);
            });
        }).exceptionally(throwable -> {
            System.out.println(throwable);
            return null;
        });
    }

    public void uploadFor(long nanos) {
        long limit = System.nanoTime() + nanos;
        RenderChunk r;
        while (System.nanoTime() < limit && (r = toUpload.poll()) != null) {
            r.upload();
        }
    }

    public void render() {
        OpenGL.depthTest(true);
        OpenGL.depthMask(true);

        final RenderType[] renderTypes = RenderType.values();
        for (int i = 0; i < renderTypes.length; i++) {
            if (i > 0) return; //TODO Remove with transparency

            final RenderType renderType = renderTypes[i];
            final ChunkProgram program = renderType.getProgram();

            program.use(() -> {
                OpenGL.primitiveRestart(true);
                OpenGL.primitiveRestartIndex(ChunkProgram.PRIMITIVE_RESET_INDEX);

                program.mvp.set(Main.INSTANCE.getRenderer().getCamera().transform());
                program.atlas.bind(0, atlas);
                program.camera.set(Main.INSTANCE.getRenderer().getCamera().getPosition());
                program.lightColor.set(lightColor);
                program.lightDirection.set(new Vec3f(0, -1, 0));
                program.ambientStrength.set(ambientStrength);
                program.diffuseStrength.set(diffuseStrength);
                program.specularStrength.set(specularStrength);
                program.normalizedSpriteSize.set(atlas.getNormalizedSpriteSize());

                for (RenderChunk renderChunk : renderChunkList) {
                    renderChunk.render(renderType);
                }
            });
        }
    }

    public Collection<RenderChunk> getRenderChunks() {
        return Collections.unmodifiableList(renderChunkList);
    }

    public RenderChunk getRenderChunk(Vec3i position) {
        return renderChunks.get(position);
    }

    public TextureAtlas getAtlas() {
        return atlas;
    }
}
