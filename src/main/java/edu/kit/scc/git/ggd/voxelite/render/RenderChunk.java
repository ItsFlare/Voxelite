package edu.kit.scc.git.ggd.voxelite.render;

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

    public RenderChunk(Chunk chunk) {
        this.chunk = chunk;

        RenderType[] renderTypes = RenderType.values();
        for (int i = 0; i < renderTypes.length; i++) {
            if (i > 0) continue; //TODO Remove with transparency
            RenderType renderType = renderTypes[i];
            slices[i] = renderType.getProgram().new Slice(chunk, renderType);
        }
    }

    public void build() {
        //TODO Make async

        for (Voxel voxel : chunk) {
            final Block block = voxel.getBlock();
            if (block == null) continue;

            final ChunkProgram.Slice slice = slices[block.getRenderType().ordinal()];

            for (Direction direction : Direction.values()) {
                final Voxel neighbor = voxel.getNeighbor(direction);
                if (neighbor == null || neighbor.getBlock() == null) {

                    Vec2i texture = block.getTexture(direction);

                    slice.queue.add(new ChunkProgram.Slice.QueuedQuad(direction, Chunk.toChunkSpace(voxel.position()), texture));
                }
            }
        }

        for (ChunkProgram.Slice slice : slices) {
            if (slice == null) continue; //TODO Remove with transparency
            slice.upload();
        }
    }

    public void render(RenderType renderType) {
        slices[renderType.ordinal()].render();
    }

    public void delete() {
        for (ChunkProgram.Slice slice : slices) {
            if(slice == null) return; //TODO Remove with transparency
            slice.vertexArray.delete();
            slice.instanceBuffer.delete();
        }
    }

    public int getQuadCount() {
        return Arrays.stream(slices).filter(Objects::nonNull).mapToInt(ChunkProgram.Slice::getQuadCount).sum(); //TODO Remove filter with transparency
    }
}
