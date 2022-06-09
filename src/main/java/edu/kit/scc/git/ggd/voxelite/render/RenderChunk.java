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
    private final Chunk                chunk;
    private final ChunkProgram.Slice[] slices = new ChunkProgram.Slice[RenderType.values().length];

    private volatile boolean valid = true, dirty;
    private HullSet hullSet = new HullSet();
    private boolean occluded;
    private int occlusionFrame;

    public RenderChunk(Chunk chunk) {
        this.chunk = chunk;

        RenderType[] renderTypes = RenderType.values();
        for (int i = 0; i < renderTypes.length; i++) {
            if (i > 0) continue; //TODO Remove with transparency
            RenderType renderType = renderTypes[i];
            slices[i] = new ChunkProgram.Slice(chunk.getPosition(), renderType);
        }
    }

    public synchronized void build() {
        if (!valid || !dirty) return;
        dirty = false;

        this.hullSet = chunk.getVisibilityStorage().calculate();

        //TODO Ensure chunk memory visible
        for (Voxel voxel : chunk) {
            final Block block = voxel.getBlock();
            if (block == Block.AIR) continue;

            final ChunkProgram.Slice slice = slices[block.getRenderType().ordinal()];

            for (Direction direction : Direction.values()) {
                final Voxel neighbor = voxel.getNeighbor(direction);
                if (neighbor == null || !neighbor.isOpaque()) {

                    Vec2i texture = block.getTexture(direction);
                    Vec3i light = neighbor == null ? new Vec3i() : neighbor.chunk().getLightStorage().getLight(neighbor.position());

                    synchronized (slice) {
                        slice.queue.add(new ChunkProgram.Slice.QueuedQuad(direction, Chunk.toChunkSpace(voxel.position()), texture, light));
                    }
                }
            }
        }

        for (ChunkProgram.Slice s : slices) {
            if (s != null) s.build();
        }

        Main.INSTANCE.getRenderer().getWorldRenderer().queueUpload(this);
    }

    public void upload() {
        assert valid;

        for (ChunkProgram.Slice slice : slices) {
            if (slice == null) continue; //TODO Remove with transparency
            slice.upload();
        }
    }

    public void render(RenderType renderType, Vec3f cameraPosition) {
        assert valid;

        slices[renderType.ordinal()].render(cameraPosition);
    }

    public void renderShadow(RenderType renderType, int visibilityBitset) {
        assert valid;

        slices[renderType.ordinal()].renderShadow(visibilityBitset);
    }

    public void delete() {
        valid = false;
        for (ChunkProgram.Slice slice : slices) {
            if (slice == null) return; //TODO Remove with transparency
            slice.vertexArray.delete();
            slice.instanceBuffer.delete();
        }
    }

    public int getQuadCount() {
        return Arrays.stream(slices).filter(Objects::nonNull).mapToInt(ChunkProgram.Slice::getQuadCount).sum(); //TODO Remove filter with transparency
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

    public void setOccluded(boolean occluded, int frame) {
        this.occluded = occluded;
        this.occlusionFrame = frame;
    }

    public boolean isOccluded() {
        return occluded && occlusionFrame == Main.INSTANCE.getRenderer().getFrame() - 1;
    }
}
