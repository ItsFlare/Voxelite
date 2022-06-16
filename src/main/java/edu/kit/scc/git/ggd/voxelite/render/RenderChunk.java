package edu.kit.scc.git.ggd.voxelite.render;

import edu.kit.scc.git.ggd.voxelite.Main;
import edu.kit.scc.git.ggd.voxelite.util.Direction;
import edu.kit.scc.git.ggd.voxelite.world.Block;
import edu.kit.scc.git.ggd.voxelite.world.Chunk;
import edu.kit.scc.git.ggd.voxelite.world.HullSet;
import edu.kit.scc.git.ggd.voxelite.world.Voxel;
import net.durchholz.beacon.math.Vec2i;
import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.math.Vec3i;

import java.util.Arrays;
import java.util.Objects;

public class RenderChunk {
    public static final int FULL_VISIBILITY = (1 << Direction.values().length) - 1;

    private final Chunk   chunk;
    private final Slice[] slices = new Slice[RenderType.values().length];
    private final int     occlusionQueryId;

    private volatile boolean valid = true, dirty;
    private HullSet hullSet = new HullSet();

    private int occlusionFrame;

    public RenderChunk(Chunk chunk) {
        this.chunk = chunk;

        RenderType[] renderTypes = RenderType.values();
        for (int i = 0; i < renderTypes.length; i++) {
            RenderType renderType = renderTypes[i];
            slices[i] = switch (renderType) {
                case OPAQUE -> new OpaqueSlice(renderType);
                case TRANSPARENT -> new TransparentSlice(chunk.getWorldPosition(), renderType);
            };
        }

        final WorldRenderer worldRenderer = Main.INSTANCE.getRenderer().getWorldRenderer();
        final OcclusionRenderer.Query query = new OcclusionRenderer.Query(
                () -> getQuadCount() >= worldRenderer.occlusionCullThreshold,
                () -> getChunk().getBoundingBox(),
                () -> setOccluded(Main.INSTANCE.getRenderer().getFrame())
        );
        occlusionQueryId = worldRenderer.getOcclusionRenderer().getQueries().add(query);
    }

    public OpaqueSlice opaqueSlice() {
        return (OpaqueSlice) slices[RenderType.OPAQUE.ordinal()];
    }

    public TransparentSlice transparentSlice() {
        return (TransparentSlice) slices[RenderType.TRANSPARENT.ordinal()];
    }

    public static int directionCull(Vec3f cameraPosition, Vec3i chunkWorldPosition) {
        final int visibilityBitset;

        /*
        Direction culling (geometry partitioned by face direction)
        TODO Unroll loop and replace dot product with comparison?
        */

        final Vec3i chunkCenter = chunkWorldPosition.add(Chunk.CENTER);
        final Direction[] directions = Direction.values();

        //Calculate visibility bitset
        int bitset = 0;
        for (int i = 0; i < directions.length; i++) {

            final var direction = directions[i];
            final var planePos = chunkCenter.subtract(direction.getAxis().scale(Chunk.WIDTH >> 1));
            final var planeToCam = cameraPosition.subtract(planePos);

            if (planeToCam.dot(direction.getAxis()) > 0f) {
                bitset |= (1 << i);
            }
        }

        visibilityBitset = bitset;
        return visibilityBitset;
    }

    public synchronized void build() {
        if (!valid || !dirty) return;
        dirty = false;

        this.hullSet = chunk.getVisibilityStorage().calculate();

        //TODO Ensure chunk memory visible
        for (Voxel voxel : chunk) {
            final Block block = voxel.getBlock();
            if (block == Block.AIR) continue;

            final Slice slice = slices[block.getRenderType().ordinal()];

            for (Direction direction : Direction.values()) {
                final Voxel neighbor = voxel.getNeighbor(direction);
                if (neighbor == null || (!neighbor.isOpaque() && block != neighbor.getBlock())) {

                    Vec2i texture = block.getTexture(direction);
                    Vec3i light = neighbor == null ? new Vec3i() : neighbor.chunk().getLightStorage().getLight(neighbor.position());

                    synchronized (slice) {
                        slice.queue(new OpaqueSlice.QueuedQuad(direction, Chunk.toChunkSpace(voxel.position()), texture, light));
                    }
                }
            }
        }

        for (Slice s : slices) {
            s.build();
        }

        Main.INSTANCE.getRenderer().getWorldRenderer().queueUpload(this);
    }

    public void upload() {
        assert valid;

        for (Slice slice : slices) {
            slice.upload();
        }
    }

    public void render(RenderType renderType, int visibility) {
        assert valid;

        slices[renderType.ordinal()].render(visibility);
    }

    public void renderShadow(RenderType renderType, int visibility) {
        assert valid;

        slices[renderType.ordinal()].renderShadow(visibility);
    }

    public void delete() {
        assert valid;

        valid = false;
        for (Slice slice : slices) {
            slice.delete();
        }
        Main.INSTANCE.getRenderer().getWorldRenderer().getOcclusionRenderer().getQueries().remove(occlusionQueryId);
    }

    public void sortTransparent() {
        assert valid;
        transparentSlice().sort();
    }

    public int getQuadCount() {
        return Arrays.stream(slices).filter(Objects::nonNull).mapToInt(Slice::getQuadCount).sum(); //TODO Remove filter with transparency
    }

    public Chunk getChunk() {
        return chunk;
    }

    public boolean isValid() {
        return valid;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty() {
        this.dirty = true;
    }

    public HullSet getHullSet() {
        return hullSet;
    }

    private void setOccluded(int frame) {
        this.occlusionFrame = frame;
    }

    public boolean isOccluded() {
        return getQuadCount() >= Main.INSTANCE.getRenderer().getWorldRenderer().occlusionCullThreshold
                && occlusionFrame == Main.INSTANCE.getRenderer().getFrame();
    }
}
