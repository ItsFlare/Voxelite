package edu.kit.scc.git.ggd.voxelite.render;

import edu.kit.scc.git.ggd.voxelite.Main;
import edu.kit.scc.git.ggd.voxelite.util.Direction;
import edu.kit.scc.git.ggd.voxelite.world.Block;
import edu.kit.scc.git.ggd.voxelite.world.Chunk;
import edu.kit.scc.git.ggd.voxelite.world.Voxel;
import net.durchholz.beacon.math.Vec2i;

import java.util.Arrays;
import java.util.Objects;

public class RenderChunk {
    private final Chunk                chunk;
    private final ChunkProgram.Slice[] slices = new ChunkProgram.Slice[RenderType.values().length];
    private boolean valid = true;

    public RenderChunk(Chunk chunk) {
        this.chunk = chunk;

        RenderType[] renderTypes = RenderType.values();
        for (int i = 0; i < renderTypes.length; i++) {
            if (i > 0) continue; //TODO Remove with transparency
            RenderType renderType = renderTypes[i];
            slices[i] = renderType.getProgram().new Slice(chunk.getPosition(), renderType);
        }
    }

    public void build() {
        synchronized (this) {
            if(!valid) return;

            for (Voxel voxel : chunk) {
                final Block block = voxel.getBlock();
                if (block == null) continue;

                final ChunkProgram.Slice slice = slices[block.getRenderType().ordinal()];

                for (Direction direction : Direction.values()) {
                    final Voxel neighbor = voxel.getNeighbor(direction);
                    if (neighbor == null || neighbor.getBlock() == null) {

                        Vec2i texture = block.getTexture(direction);

                        synchronized (slice.queue) {
                            slice.queue.add(new ChunkProgram.Slice.QueuedQuad(direction, Chunk.toChunkSpace(voxel.position()), texture));
                        }
                    }
                }
            }
        }

        Main.INSTANCE.getRenderer().getWorldRenderer().toUpload.add(this);
    }

    public synchronized void upload() {
        if(!valid) return;
        for (ChunkProgram.Slice slice : slices) {
            if (slice == null) continue; //TODO Remove with transparency
            slice.upload();
        }
    }

    public synchronized void render(RenderType renderType) {
        if(!valid) return;
        slices[renderType.ordinal()].render();
    }

    public synchronized void delete() {
        valid = false;
        for (ChunkProgram.Slice slice : slices) {
            if(slice == null) return; //TODO Remove with transparency
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
}
