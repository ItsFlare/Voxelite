package edu.kit.scc.git.ggd.voxelite.render;

import edu.kit.scc.git.ggd.voxelite.Main;
import edu.kit.scc.git.ggd.voxelite.texture.TextureAtlas;
import edu.kit.scc.git.ggd.voxelite.util.Direction;
import edu.kit.scc.git.ggd.voxelite.world.Chunk;
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
import java.util.stream.Collectors;

public class WorldRenderer {

    public static final Comparator<RenderChunk> DISTANCE_COMPARATOR = Comparator.comparingInt(rc -> Chunk.toWorldPosition(rc.getChunk().getPosition()).subtract(new Vec3i(Main.INSTANCE.getRenderer().getCamera().getPosition())).magnitudeSq());

    private final Map<Vec3i, RenderChunk> renderChunks    = new HashMap<>();
    private final TextureAtlas            atlas;
    private final Set<RenderChunk>        toBuild         = ConcurrentHashMap.newKeySet();
    public final  Queue<RenderChunk>      toUpload        = new PriorityBlockingQueue<>(1000, DISTANCE_COMPARATOR);

    public List<RenderChunk> renderList = new ArrayList<>();
    public Vec3f lightColor      = new Vec3f(1);
    public float ambientStrength = 0.4f, diffuseStrength = 0.7f, specularStrength = 0.2f;
    public int phongExponent = 32;

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
        toBuild.add(renderChunk);
        queueNeighbors(position);
    }

    @Listener
    private void onChunkUnload(ChunkUnloadEvent event) {
        final Vec3i position = event.chunk().getPosition();
        final RenderChunk renderChunk = renderChunks.remove(position);
        renderList.remove(renderChunk);
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

    public void render() {
        upload(5);
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
                program.phongExponent.set(phongExponent);
                program.normalizedSpriteSize.set(atlas.getNormalizedSpriteSize());

                for (RenderChunk renderChunk : renderList) {
                    renderChunk.render(renderType);
                }
            });
        }
    }

    public void tick() {
        buildChunksAsync();
        renderList = renderChunks.values().stream().filter(renderChunk -> renderChunk.getQuadCount() > 0).collect(Collectors.toList());
    }

    public Collection<RenderChunk> getRenderChunks() {
        return Collections.unmodifiableCollection(renderChunks.values());
    }

    public RenderChunk getRenderChunk(Vec3i position) {
        return renderChunks.get(position);
    }

    public TextureAtlas getAtlas() {
        return atlas;
    }

    private void buildChunksAsync() {
        CompletableFuture.runAsync(() -> {
            toBuild.forEach(renderChunk -> {
                ForkJoinPool.commonPool().submit(renderChunk::build); //TODO Error handling
                toBuild.remove(renderChunk);
            });
        }).exceptionally(throwable -> {
            System.out.println(throwable);
            return null;
        });
    }

    private void upload(int limit) {
        RenderChunk r;
        int i = 0;
        while (i++ < limit && (r = toUpload.poll()) != null) {
            r.upload();
        }
    }
}
