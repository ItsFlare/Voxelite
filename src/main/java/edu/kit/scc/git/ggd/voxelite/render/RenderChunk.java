package edu.kit.scc.git.ggd.voxelite.render;

import edu.kit.scc.git.ggd.voxelite.Main;
import edu.kit.scc.git.ggd.voxelite.util.Direction;
import edu.kit.scc.git.ggd.voxelite.world.*;
import net.durchholz.beacon.math.Vec2i;
import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.math.Vec3i;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Objects;

public class RenderChunk {
    public static final int    FULL_VISIBILITY = (1 << Direction.values().length) - 1;
    public static final Logger LOGGER          = LoggerFactory.getLogger(RenderChunk.class);

    private final WorldChunk chunk;
    private final Slice[]    slices = new Slice[RenderType.values().length];
    private final int     occlusionQueryId;

    private volatile boolean valid = true, dirty;
    private HullSet hullSet = new HullSet();

    private int occlusionFrame;

    public RenderChunk(WorldChunk chunk) {
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
                        slice.queue(new OpaqueSlice.QueuedQuad(direction, Chunk.toChunkSpace(voxel.position()), texture, light, aoByte(voxel, direction)));
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

    public WorldChunk getChunk() {
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


    public static byte aoByte(Voxel voxel, Direction direction) {
        Direction sideDirection;
        Direction upDirection;

        if(direction == Direction.POS_X || direction == Direction.NEG_X) {
            sideDirection = direction == Direction.POS_X ? Direction.NEG_Z : Direction.POS_Z;
        } else if(direction == Direction.POS_Z || direction == Direction.NEG_Z) {
            sideDirection = direction == Direction.POS_Z ? Direction.POS_X : Direction.NEG_X;
        } else {
            sideDirection = Direction.POS_X;
        }

        if(direction == Direction.POS_Y || direction == Direction.NEG_Y) {
            upDirection = direction == Direction.POS_Y ? Direction.NEG_Z : Direction.POS_Z;
        } else {
            upDirection = Direction.POS_Y;
        }

        Voxel bottomSide;
        Voxel topSide;
        Voxel rightSide;
        Voxel leftSide;
        Voxel bottomRightCorner;
        Voxel bottomLeftCorner;
        Voxel topRightCorner;
        Voxel topLeftCorner;
        Voxel neighbor1;
        Voxel neighbor2;

        neighbor1 = voxel.getNeighbor(upDirection.getOpposite());
        bottomSide = neighbor1 == null ? null : neighbor1.getNeighbor(direction);

        neighbor1 = voxel.getNeighbor(upDirection);
        topSide = neighbor1 == null ? null : neighbor1.getNeighbor(direction);

        neighbor1 = voxel.getNeighbor(sideDirection);
        rightSide = neighbor1 == null ? null : neighbor1.getNeighbor(direction);

        neighbor1 = voxel.getNeighbor(sideDirection.getOpposite());
        leftSide = neighbor1 == null ? null : neighbor1.getNeighbor(direction);

        neighbor1 = voxel.getNeighbor(sideDirection);
        neighbor2 = neighbor1 == null ? null : neighbor1.getNeighbor(direction);
        bottomRightCorner = neighbor2 == null ? null : neighbor2.getNeighbor(upDirection.getOpposite());

        neighbor1 = voxel.getNeighbor(sideDirection.getOpposite());
        neighbor2 = neighbor1 == null ? null : neighbor1.getNeighbor(direction);
        bottomLeftCorner = neighbor2 == null ? null : neighbor2.getNeighbor(upDirection.getOpposite());

        neighbor1 = voxel.getNeighbor(sideDirection);
        neighbor2 = neighbor1 == null ? null : neighbor1.getNeighbor(direction);
        topRightCorner = neighbor2 == null ? null : neighbor2.getNeighbor(upDirection);

        neighbor1 = voxel.getNeighbor(sideDirection.getOpposite());
        neighbor2 = neighbor1 == null ? null : neighbor1.getNeighbor(direction);
        topLeftCorner = neighbor2 == null ? null : neighbor2.getNeighbor(upDirection);

        int v0 = vertexAO(
                (rightSide == null || rightSide.getBlock().equals(Block.AIR)) ? 0 : 1,
                (bottomSide == null || bottomSide.getBlock().equals(Block.AIR)) ? 0 : 1,
                (bottomRightCorner == null || bottomRightCorner.getBlock().equals(Block.AIR)) ? 0 : 1);
        int v1 = vertexAO(
                leftSide == null || leftSide.getBlock().equals(Block.AIR) ? 0 : 1,
                bottomSide == null || bottomSide.getBlock().equals(Block.AIR) ? 0 : 1,
                bottomLeftCorner == null || bottomLeftCorner.getBlock().equals(Block.AIR) ? 0 : 1);
        int v2 = vertexAO(
                rightSide == null || rightSide.getBlock().equals(Block.AIR) ? 0 : 1,
                topSide == null || topSide.getBlock().equals(Block.AIR) ? 0 : 1,
                topRightCorner == null || topRightCorner.getBlock().equals(Block.AIR) ? 0 : 1);
        int v3 = vertexAO(
                leftSide == null || leftSide.getBlock().equals(Block.AIR) ? 0 : 1,
                topSide == null || topSide.getBlock().equals(Block.AIR) ? 0 : 1,
                topLeftCorner == null || topLeftCorner.getBlock().equals(Block.AIR) ? 0 : 1);

        byte result = 0;

        result |= v1;
        result |= v0 << 2;
        result |= v3 << 4;
        result |= v2 << 6;

        return result;
    }

    private static int vertexAO(int side1, int side2, int corner) {
        if(side1 == 1 && side2 == 1) {
            return 0;
        }
        return 3 - (side1 + side2 + corner);
    }

}
