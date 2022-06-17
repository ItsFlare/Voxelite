package edu.kit.scc.git.ggd.voxelite.render;

import edu.kit.scc.git.ggd.voxelite.Main;
import edu.kit.scc.git.ggd.voxelite.texture.TextureAtlas;
import edu.kit.scc.git.ggd.voxelite.util.Direction;
import edu.kit.scc.git.ggd.voxelite.util.Frustum;
import edu.kit.scc.git.ggd.voxelite.world.*;
import edu.kit.scc.git.ggd.voxelite.world.event.ChunkLoadEvent;
import edu.kit.scc.git.ggd.voxelite.world.event.ChunkUnloadEvent;
import net.durchholz.beacon.event.EventType;
import net.durchholz.beacon.event.Listener;
import net.durchholz.beacon.math.Matrix4f;
import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.math.Vec3i;
import net.durchholz.beacon.math.Vec4f;
import net.durchholz.beacon.render.opengl.OpenGL;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.stream.Collectors;

import static org.lwjgl.opengl.GL30.*;

/*
    Draw ShadowMap
        Construct PPLL OIT
            Sort GPU or CPU? One IBO + slow transparency sort for shadow?

    Draw Opaque Geometry Deferred FTB to OPAQUE
        Write depth to alpha
        Apply lighting + shadows

    Draw Occlusion Culling (needs only opaque depth)

    Draw Transparent Forward BTF to TRANSPARENT
        Write to depth
        Apply VL from current to behind
        Apply SSR

    Draw SSR + Blend to OPAQUE
        Use depth from alpha
        Apply SSR while blending with TRANSPARENT after reflect
        Blend TRANSPARENT

    Draw Volumetric Lighting
        Optionally sample Shadow OIT buffer
        Optionally sample Grid (Density & Block Light, Frustel vs. Voxel)

    See transparent in SSR:
        Reflect in last frame
        or Treat reflectors as transparent (bad overdraw)
        ==> or Split opaque and transparent framebuffers and draw transparent before deferred lighting
 */
public class WorldRenderer {

    private static final Comparator<RenderChunk> DISTANCE_COMPARATOR = Comparator.comparingInt(rc -> Chunk.toWorldPosition(rc.getChunk().getPosition()).subtract(new Vec3i(Main.INSTANCE.getRenderer().getCamera().getPosition())).magnitudeSq()); //Breaks general contract
    private static final int                     SHADOW_MAP_SIZE     = 1 << 13;
    public static        int                     frustumNumber       = 0;

    private final Map<Vec3i, RenderChunk> renderChunks = new HashMap<>();
    private final TextureAtlas            atlas;

    private final BlockingQueue<RenderChunk> buildQueue        = new PriorityBlockingQueue<>(128, DISTANCE_COMPARATOR);
    private final BlockingQueue<RenderChunk> uploadQueue       = new PriorityBlockingQueue<>(128, DISTANCE_COMPARATOR);
    private final AsyncChunkBuilder          asyncChunkBuilder = new AsyncChunkBuilder(buildQueue, uploadQueue, ForkJoinPool.getCommonPoolParallelism() / 4 + 1);
    private final ShadowMapRenderer          shadowMapRenderer = new ShadowMapRenderer(SHADOW_MAP_SIZE, 4);
    private final OcclusionRenderer          occlusionRenderer = new OcclusionRenderer();
    private final LineRenderer               lineRenderer      = new LineRenderer();
    private final GeometryBuffer             gBuffer           = new GeometryBuffer(1, 1);
    private final CompositeRenderer          compositeRenderer = new CompositeRenderer();
    private final QuadRenderer quadRenderer = new QuadRenderer();


    private RenderChunk[] lastSorted = new RenderChunk[0];

    public List<RenderChunk> renderList      = new ArrayList<>();
    public Vec4f             lightColor      = new Vec4f(1);
    public float             ambientStrength = 0.4f, diffuseStrength = 0.7f, specularStrength = 0.2f;
    public int phongExponent = 32, uploadRate = 5, sortRate = 5;
    public boolean directionCull = true, backfaceCull = true, dotCull = true, frustumCull = true, caveCull = true, occlusionCull = true, shadows = true, shadowTransform = false, transparentSort = true, captureFrustum = false, debugFrustum = false;
    public int emptyCount, frustumCullCount, dotCullCount, caveCullCount, occlusionCullCount, totalCullCount;
    public int occlusionCullThreshold;

    private Frustum   capturedFrustum;
    private Frustum[] capturedShadowFrustums;
    private Frustum[] capturedSplitFrustums;

    public WorldRenderer() {
        EventType.addListener(this);

        try {
            atlas = new TextureAtlas("/textures/blocks");
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }

        asyncChunkBuilder.start();
    }

    @Listener
    private void onChunkLoad(ChunkLoadEvent event) {
        final RenderChunk renderChunk = new RenderChunk(event.chunk());
        final Vec3i position = event.chunk().getPosition();
        renderChunks.put(position, renderChunk);

        renderChunk.setDirty();
        buildQueue.add(renderChunk);
        queueNeighbors(position);
    }

    @Listener
    private void onChunkUnload(ChunkUnloadEvent event) {
        final Vec3i position = event.chunk().getPosition();
        final RenderChunk renderChunk = renderChunks.remove(position);
        buildQueue.remove(renderChunk);
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
        renderChunks.values().forEach(RenderChunk::setDirty);
        buildQueue.addAll(renderChunks.values());
    }

    public void queueRebuild(RenderChunk renderChunk) {
        renderChunk.setDirty();
        buildQueue.add(renderChunk);
    }

    public void render() {
        upload(uploadRate);
        if (transparentSort) sort(sortRate);

        final Camera camera = Main.INSTANCE.getRenderer().getCamera();
        final Vec3f cameraPosition = camera.getPosition();
        final Vec3f cameraDirection = camera.getDirection();

        final Matrix4f mvp = camera.transform();
        final Frustum frustum = new Frustum(mvp);

        final Vec3f lightDirection = Main.INSTANCE.getWorld().getSunlightDirection();
        if (shadows) shadowMapRenderer.render(lightDirection);

        if (!captureFrustum) {
            capturedFrustum = frustum;
            capturedShadowFrustums = new Frustum[shadowMapRenderer.cascades];

            for (int i = 0; i < shadowMapRenderer.cascades; i++) {
                capturedShadowFrustums[i] = new Frustum(shadowMapRenderer.lightTransform(i, lightDirection));
            }

            capturedSplitFrustums = shadowMapRenderer.split(camera);
        }

        dotCullCount = 0;
        frustumCullCount = 0;
        final var frameRenderList = renderList
                .stream()
                .filter(RenderChunk::isValid)
                .filter(dotCull ? renderChunk -> {
                    //Filter if behind camera (biased towards view direction to compensate for chunk volume) - basically frustum culling ordered on Wish.
                    final Vec3f camToChunk = renderChunk.getChunk().getCenter().subtract(cameraPosition);
                    final boolean visible = camToChunk.add(cameraDirection.scale(Chunk.RADIUS)).dot(cameraDirection) >= 0;
                    if (!visible) dotCullCount++;
                    return visible;
                } : renderChunk -> true)
                .filter(frustumCull ? renderChunk -> {
                    boolean visible = frustum.intersects(renderChunk.getChunk().getBoundingBox());
                    if (!visible) frustumCullCount++;
                    return visible;
                } : renderChunk -> true)
                .collect(Collectors.toList());

        occlusionCullCount = 0;
        if (occlusionCull && !shadowTransform) {
            occlusionRenderer.read(); //TODO Read only whats necessary

            final int previous = frameRenderList.size();
            frameRenderList.removeIf(renderChunk -> renderChunk.isOccluded() && renderChunk.getChunk().getCenter().subtract(cameraPosition).magnitudeSq() > Chunk.RADIUS_SQUARED);
            occlusionCullCount = previous - frameRenderList.size();
        }

        totalCullCount = emptyCount + dotCullCount + frustumCullCount + caveCullCount + occlusionCullCount;

        record RenderInfo(RenderChunk chunk, int visibility) {}
        final var frameRenderInfo = frameRenderList
                .stream()
                .map(directionCull ?
                        renderChunk -> new RenderInfo(renderChunk, RenderChunk.directionCull(cameraPosition, renderChunk.getChunk().getWorldPosition())) :
                        renderChunk -> new RenderInfo(renderChunk, RenderChunk.FULL_VISIBILITY))
                .toList();

        OpenGL.colorMask(true);
        OpenGL.depthMask(true);
        OpenGL.depthTest(true);
        OpenGL.depthFunction(OpenGL.CompareFunction.LESS);
        OpenGL.cull(backfaceCull);

        gBuffer.allocate(Main.INSTANCE.getWindow().getWidth(), Main.INSTANCE.getWindow().getHeight());

        OpenGL.use(gBuffer, () -> {
            OpenGL.setDrawBuffers(GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT1, GL_COLOR_ATTACHMENT2, GL_COLOR_ATTACHMENT3);
            OpenGL.clearAll();

            //Draw opaque
            {
                OpenGL.setDrawBuffers(GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT2, GL_COLOR_ATTACHMENT3);
                RenderType.OPAQUE.setPipelineState();

                final ChunkProgram program = RenderType.OPAQUE.getProgram();
                program.use(() -> {
                    setCommonUniforms(program, mvp, cameraPosition, lightDirection);

                    for (RenderInfo info : frameRenderInfo) {
                        final RenderChunk renderChunk = info.chunk();
                        program.chunk.set(renderChunk.getChunk().getWorldPosition());

                        renderChunk.render(RenderType.OPAQUE, info.visibility());
                    }
                });
            }

            //Draw occlusion culling
            {
                if (occlusionCull) occlusionRenderer.render(mvp);
            }

            //Draw transparent
            {
                OpenGL.setDrawBuffers(GL_COLOR_ATTACHMENT1);
                RenderType.TRANSPARENT.setPipelineState();

                final ChunkProgram program = RenderType.TRANSPARENT.getProgram();
                program.use(() -> {
                    setCommonUniforms(program, mvp, cameraPosition, lightDirection);

                    for (int i = frameRenderInfo.size() - 1; i >= 0; i--) {
                        RenderInfo info = frameRenderInfo.get(i);
                        final RenderChunk renderChunk = info.chunk();
                        program.chunk.set(renderChunk.getChunk().getWorldPosition());

                        renderChunk.render(RenderType.TRANSPARENT, info.visibility());
                    }
                });
            }

            OpenGL.setDrawBuffers(GL_COLOR_ATTACHMENT0);
            if (debugFrustum) {
                final Matrix4f matrix;
                if (shadowTransform) {
                    matrix = shadowMapRenderer.lightTransform(frustumNumber, lightDirection);
                } else {
                    //Extend far plane to ensure lines are visible
                    var f = camera.getFar();
                    camera.setFar(10_000);
                    matrix = camera.transform();
                    camera.setFar(f);
                }

                //If red is visible the split is bad (split should draw over)
                lineRenderer.render(matrix, new Vec4f(1, 0, 0, 1), capturedFrustum);

                for (int i = 0; i < shadowMapRenderer.cascades; i++) {
                    final float ratio = (shadowMapRenderer.cascades - i) / (float) shadowMapRenderer.cascades;
                    final Vec4f color = new Vec4f(ratio, ratio, 1f, 1f);

                    lineRenderer.render(matrix, color, capturedShadowFrustums[i]);
                    lineRenderer.render(matrix, color, capturedSplitFrustums[i]);
                }

                final Chunk chunk = Main.INSTANCE.getWorld().getChunk(Chunk.toChunkPosition(cameraPosition));
                if (chunk != null) lineRenderer.render(matrix, new Vec4f(0, 1, 0, 1), chunk.getBoundingBox());

                for (RenderChunk renderChunk : lastSorted) {
                    lineRenderer.render(matrix, new Vec4f(1, 1, 0, 1), renderChunk.getChunk().getBoundingBox());
                }
            }
        });

        //Draw composite
        compositeRenderer.render(gBuffer, shadowMapRenderer.getTexture());

        //quadRenderer.render(Matrix4f.identity(), gBuffer.normal(), new Vec2f(0), new Vec2f(1));
    }

    private void setCommonUniforms(ChunkProgram program, Matrix4f mvp, Vec3f cameraPosition, Vec3f lightDirection) {
        program.mvp.set(shadowTransform ? shadowMapRenderer.lightTransform(frustumNumber, lightDirection) : mvp);
        if(program instanceof OpaqueChunkProgram cp) {
            cp.view.set(Main.INSTANCE.getRenderer().getCamera().view(false, true));
        }
        program.atlas.bind(0, atlas);
        program.camera.set(cameraPosition);
        program.lightColor.set(new Vec3f(lightColor.x(), lightColor.y(), lightColor.z()));
        program.lightDirection.set(lightDirection);

        Vec3f phongParameters = Main.INSTANCE.getWorld().getPhongParameters();
        program.ambientStrength.set(phongParameters.x());
        program.diffuseStrength.set(phongParameters.y());
        program.specularStrength.set(phongParameters.z());

        program.phongExponent.set(phongExponent);
        program.normalizedSpriteSize.set(atlas.getNormalizedSpriteSize());
        program.maxLightValue.set(LightStorage.MAX_TOTAL_VALUE);
        program.shadowMap.bind(1, shadowMapRenderer.getTexture());
        program.lightView.set(shadowMapRenderer.lightView(lightDirection));
        program.shadows.set(shadows && !shadowTransform ? 1 : 0);
        program.constantBias.set(shadowMapRenderer.constantBias);


        program.cascadeScales.set(Arrays.stream(shadowMapRenderer.c).map(ShadowMapRenderer.Cascade::scale).toArray(Vec3f[]::new));
        program.cascadeTranslations.set(Arrays.stream(shadowMapRenderer.c).map(ShadowMapRenderer.Cascade::translation).toArray(Vec3f[]::new));
        program.cascadeFar.set(Arrays.stream(shadowMapRenderer.c).map(ShadowMapRenderer.Cascade::far).toArray(Float[]::new));
    }

    public record VisibilityNode(RenderChunk renderChunk, Direction source, int directions) {
        public boolean hasDirection(Direction direction) {
            return (this.directions & (1 << direction.ordinal())) == 1;
        }
    }

    private Collection<RenderChunk> caveCull() {
        /*
        Tommaso Checchi et al. Advanced Cave Culling Algorithm. 2014.
         */

        final Set<RenderChunk> result = new HashSet<>();
        final Queue<VisibilityNode> queue = new ArrayDeque<>();

        final Camera camera = Main.INSTANCE.getRenderer().getCamera();
        final Vec3i cameraBlockPosition = Chunk.toBlockPosition(camera.getPosition());
        final RenderChunk currentChunk = renderChunks.get(Chunk.toChunkPosition(cameraBlockPosition));

        //Outside loaded area
        if (currentChunk == null) return renderChunks.values();

        //Inside opaque block
        if (currentChunk.getChunk().isOpaque(cameraBlockPosition)) return result;

        //Always render current chunk
        result.add(currentChunk);

        //Calculate which neighbors are reachable
        final Set<Direction> visibleDirections = this.floodFill(cameraBlockPosition);

        //Remove direction behind camera
        visibleDirections.remove(Direction.getNearest(camera.getDirection()).getOpposite());

        //Flood fill reached neighbors
        for (Direction direction : visibleDirections) {
            final RenderChunk neighbor = renderChunks.get(currentChunk.getChunk().getPosition().add(direction.getAxis()));
            if (neighbor != null) queue.add(new VisibilityNode(neighbor, direction, 0));
        }

        //Flood-fill chunk grid based on directional connectivity
        VisibilityNode node;
        while ((node = queue.poll()) != null) {
            if (!result.add(node.renderChunk)) continue;
            final Direction sourceDirection = node.source;

            for (Direction direction : Direction.values()) {
                final RenderChunk neighbor = renderChunks.get(node.renderChunk.getChunk().getPosition().add(direction.getAxis()));

                if (neighbor != null) {
                    boolean empty = node.renderChunk.getQuadCount() == 0;
                    boolean backwards = node.hasDirection(direction.getOpposite());
                    boolean connected = empty || sourceDirection == null || node.renderChunk.getHullSet().contains(sourceDirection.getOpposite(), direction);

                    if (!backwards && connected) {
                        VisibilityNode neighborNode = new VisibilityNode(neighbor, direction, node.directions);
                        queue.add(neighborNode);
                    }
                }
            }
        }

        return result;
    }

    private Set<Direction> floodFill(Vec3i position) {
        final Chunk chunk = Main.INSTANCE
                .getWorld()
                .getChunk(Chunk.toChunkPosition(position));
        final VisibilityStorage visibilityStorage = new VisibilityStorage();

        for (Voxel voxel : chunk) {
            if (voxel.isOpaque()) visibilityStorage.set(voxel.position(), true);
        }

        return visibilityStorage.floodFill(position);
    }

    public void tick() {
        updateRenderList();
    }

    private void updateRenderList() {
        //TODO Make neater
        //TODO Immediately add back chunks going from empty to non-empty
        emptyCount = 0;
        if (caveCull) {
            renderList = caveCull()
                    .stream()
                    .filter(renderChunk -> {
                        boolean empty = renderChunk.getQuadCount() == 0;
                        if (empty) emptyCount++;
                        return !empty;
                    })
                    .sorted(DISTANCE_COMPARATOR)
                    .collect(Collectors.toList());
            caveCullCount = renderChunks.size() - renderList.size() - emptyCount;
        } else {
            renderList = renderChunks
                    .values()
                    .stream()
                    .filter(renderChunk -> {
                        boolean empty = renderChunk.getQuadCount() == 0;
                        if (empty) emptyCount++;
                        return !empty;
                    })
                    .sorted(DISTANCE_COMPARATOR)
                    .collect(Collectors.toList());
            caveCullCount = 0;
        }
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

    public AsyncChunkBuilder getAsyncChunkBuilder() {
        return asyncChunkBuilder;
    }

    public void queueUpload(RenderChunk renderChunk) {
        uploadQueue.add(renderChunk);
    }

    private void upload(int limit) {
        RenderChunk r;
        int i = 0;
        while (i < limit && (r = uploadQueue.poll()) != null) {
            if (r.isValid()) {
                r.upload();
                i++;
            }
        }
    }

    private void sort(int limit) {
        var cameraPosition = Chunk.toBlockPosition(Main.INSTANCE.getRenderer().getCamera().getPosition());
        long tick = Main.INSTANCE.getTick();

        lastSorted = renderList
                .stream()
                .parallel()
                .filter(renderChunk -> renderChunk.transparentSlice().getQuadCount() > 0)
                .sorted(Comparator.comparingDouble(r -> r.getChunk().getCenter().subtract(cameraPosition).magnitudeSq() / (float) (tick - r.transparentSlice().getLastSortTick() + 1)))
                .limit(limit)
                .toArray(RenderChunk[]::new);

        for (RenderChunk renderChunk : lastSorted) {
            renderChunk.transparentSlice().sort();
        }
    }

    public int getBuildQueueSize() {
        return buildQueue.size();
    }

    public int getUploadQueueSize() {
        return uploadQueue.size();
    }

    public ShadowMapRenderer getShadowMapRenderer() {
        return shadowMapRenderer;
    }

    public OcclusionRenderer getOcclusionRenderer() {
        return occlusionRenderer;
    }
}
