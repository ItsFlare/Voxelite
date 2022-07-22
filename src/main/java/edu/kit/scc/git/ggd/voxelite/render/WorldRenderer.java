package edu.kit.scc.git.ggd.voxelite.render;

import edu.kit.scc.git.ggd.voxelite.Main;
import edu.kit.scc.git.ggd.voxelite.texture.TextureAtlas;
import edu.kit.scc.git.ggd.voxelite.util.Direction;
import edu.kit.scc.git.ggd.voxelite.util.Frustum;
import edu.kit.scc.git.ggd.voxelite.util.Util;
import edu.kit.scc.git.ggd.voxelite.world.*;
import edu.kit.scc.git.ggd.voxelite.world.event.ChunkLoadEvent;
import edu.kit.scc.git.ggd.voxelite.world.event.ChunkUnloadEvent;
import net.durchholz.beacon.event.EventType;
import net.durchholz.beacon.event.Listener;
import net.durchholz.beacon.math.*;
import net.durchholz.beacon.render.opengl.OpenGL;
import org.lwjgl.opengl.GL40;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.stream.Collectors;

import static java.lang.Math.sin;
import static net.durchholz.beacon.render.opengl.OpenGL.*;
import static org.lwjgl.opengl.GL30.*;

public class WorldRenderer {

    private static final Comparator<RenderChunk> DISTANCE_COMPARATOR = Comparator.comparingDouble(rc -> Main.INSTANCE.getRenderer().getCamera().getPosition().subtract(rc.getChunk().getCenter()).magnitudeSq()); //Breaks general contract
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
    private final CompositeRenderer          compositeRenderer = new CompositeRenderer();
    private final PostRenderer               postRenderer      = new PostRenderer();

    private RenderChunk[] lastSorted = new RenderChunk[0];

    public List<RenderChunk> renderList      = new ArrayList<>();
    public Vec4f             lightColor      = new Vec4f(1);
    public float             ambientStrength = 0.4f, diffuseStrength = 0.7f, specularStrength = 0.2f, debugRoughness;
    public int phongExponent = 32, uploadRate = 5, sortRate = 5;
    public boolean directionCull = true, backfaceCull = true, dotCull = true, frustumCull = true, caveCull = true, occlusionCull = false, shadows = true, shadowTransform = false, transparentSort = true, captureFrustum = false, debugFrustum = false, normalMap = true, fog = true, ao = true, aliasingOn = true, reflections = true, coneTracing = false;
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


        var gBuffer = Main.INSTANCE.getRenderer().getGeometryBuffer();

        use(STATE, gBuffer, () -> {
            resetState();
            cull(backfaceCull);

            //Draw opaque
            {
                setDrawBuffers(GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT1, GL_COLOR_ATTACHMENT2);
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
                    System.out.println(Arrays.toString(lastSorted));
                    lineRenderer.render(matrix, new Vec4f(1, 1, 0, 1), renderChunk.getChunk().getBoundingBox());
                }
            }

            //Generate mipmaps for cone tracing
            gBuffer.opaque().use(() -> gBuffer.opaque().generateMipmap());

            //Generate mipmaps for volumetric lighting
            gBuffer.depth().use(() -> gBuffer.depth().generateMipmap());

            //Draw composite
            compositeRenderer.render(gBuffer);

            //Draw transparent
            {
                final TransparentChunkProgram program = (TransparentChunkProgram) RenderType.TRANSPARENT.getProgram(); //TODO Remove cast
                use(STATE, program, gBuffer, () -> {
                    OpenGL.setDrawBuffers(GL_COLOR_ATTACHMENT3, GL_COLOR_ATTACHMENT4);
                    resetState();
                    RenderType.TRANSPARENT.setPipelineState();
                    GL40.glBlendFunci(1, GL_SRC_ALPHA, GL_SRC_COLOR);
                    GL40.glBlendEquationi(1, GL_FUNC_ADD);

                    setCommonUniforms(program, mvp, cameraPosition, lightDirection);
                    program.opaque.bind(2, gBuffer.opaque());
                    program.depth.bind(3, gBuffer.depth());
                    program.debugRoughness.set(debugRoughness);
                    program.reflections.set(reflections ? 1 : 0);
                    program.coneTracing.set(coneTracing ? 1 : 0);
                    program.projection.set(camera.projection());

                    for (int i = frameRenderInfo.size() - 1; i >= 0; i--) {
                        RenderInfo info = frameRenderInfo.get(i);
                        final RenderChunk renderChunk = info.chunk();
                        program.chunk.set(renderChunk.getChunk().getWorldPosition());

                        renderChunk.render(RenderType.TRANSPARENT, info.visibility());
                    }
                });
            }

        });

        //Generate mipmap for VL sampling
        gBuffer.composite().use(() -> gBuffer.composite().generateMipmap());

        postRenderer.render(gBuffer);
    }

    private void setCommonUniforms(ChunkProgram program, Matrix4f mvp, Vec3f cameraPosition, Vec3f lightDirection) {
        //General
        program.mvp.set(shadowTransform ? shadowMapRenderer.lightTransform(frustumNumber, lightDirection) : mvp);
        program.view.set(Main.INSTANCE.getRenderer().getCamera().view(true, true));
        program.camera.set(cameraPosition);
        program.maxLightValue.set(LightStorage.MAX_TOTAL_VALUE);

        //Textures
        program.atlas.bind(0, atlas);
        program.normalizedSpriteSize.set(atlas.getNormalizedSpriteSize());

        //Lighting
        Vec3f phongParameters = Main.INSTANCE.getWorld().getPhongParameters();
        program.ambientStrength.set(phongParameters.x());
        program.diffuseStrength.set(phongParameters.y());
        program.specularStrength.set(phongParameters.z());
        program.phongExponent.set(phongExponent);
        program.lightColor.set(new Vec3f(lightColor.x(), lightColor.y(), lightColor.z()));
        program.lightDirection.set(lightDirection);
        program.lightView.set(shadowMapRenderer.lightView(lightDirection));

        //Toggles
        program.normalMap.set(normalMap ? 1 : 0);
        program.fogSet.set(fog ? 1 : 0);
        program.aoSet.set(ao ? 1 : 0);

        //Fog
        program.fogRange.set(Main.INSTANCE.getWorld().getChunkRadius() * Chunk.WIDTH);
        program.fogColor.set(new Vec3f(0).interpolate(new Vec3f(0.55f, 0.73f, 0.91f), Util.clamp((float) sin(2 * Math.PI * Main.getDayPercentage()) + 0.3f, 0, 1)));
        //program.fogColor.set(getHorizonColor());

        //Shadow mapping
        program.shadowMap.bind(1, shadowMapRenderer.getTexture());
        program.shadows.set(shadows && !shadowTransform ? 1 : 0);
        program.constantBias.set(shadowMapRenderer.constantBias);
        program.cascadeScales.set(Arrays.stream(shadowMapRenderer.c).map(ShadowMapRenderer.Cascade::scale).toArray(Vec3f[]::new));
        program.cascadeTranslations.set(Arrays.stream(shadowMapRenderer.c).map(ShadowMapRenderer.Cascade::translation).toArray(Vec3f[]::new));
        program.cascadeFar.set(Arrays.stream(shadowMapRenderer.c).map(ShadowMapRenderer.Cascade::far).toArray(Float[]::new));
    }

    private Vec3f getHorizonColor() {
        Camera camera = Main.INSTANCE.getRenderer().getCamera();
        Quaternion quaternion = Quaternion.ofAxisAngle(new Vec3f(Direction.NEG_X.getAxis()), Main.getDayPercentage() * 360).normalized();
        Vec3f quadNormal = new Vec3f(Direction.POS_Z.getAxis());
        Vec3f sunPosition = quadNormal.rotate(quaternion);
        Vec3f skyColor = new Vec3f(0).interpolate(new Vec3f(0.55f, 0.73f, 0.91f), Util.clamp((float) sin(2 * Math.PI * Main.getDayPercentage()) + 0.2f, 0, 1));
        Vec3f sunsetColor = skyColor;
        double directionToSunDeg = Util.clamp(camera.getDirection().dot(sunPosition), 0, 1);
        if ((sunPosition.z() < -0.9 || sunPosition.z() > 0.9)) {
            float num = Math.abs(sunPosition.z());
            Vec3f a = new Vec3f(0.82f, 0.57f, 0.02f); //yellow-orange
            Vec3f b = new Vec3f(0.82f, 0.37f, 0.02f); //orange-red
            Vec3f c = new Vec3f(1.00f, 0.00f, 0.00f); //red
            Vec3f d = new Vec3f(0.70f, 0.00f, 0.30f); //purple
            if (sunPosition.y() >= 0) {
                if (num < 0.95f) {
                    sunsetColor = a.interpolate(skyColor, (0.95f - num) / 0.05f);
                } else if (num < 0.98) {
                    sunsetColor = b.interpolate(a, (0.98f - num) / 0.03f);
                } else {
                    sunsetColor = c.interpolate(b, (1 - num) / 0.02f);
                }
            } else {
                if (num > 0.95f) {
                    sunsetColor = c.interpolate(d, (1 - num) / 0.05f);
                } else {
                    sunsetColor = d.interpolate(new Vec3f(0), (0.95f - num) / 0.05f);
                }
            }
        }
        return skyColor.interpolate(sunsetColor, (float) directionToSunDeg - 0.3f);
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
        final WorldChunk chunk = Main.INSTANCE
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

    public CompositeRenderer getCompositeRenderer() {
        return compositeRenderer;
    }

    public PostRenderer getPostRenderer() {
        return postRenderer;
    }
}
