package edu.kit.scc.git.ggd.voxelite.render;

import edu.kit.scc.git.ggd.voxelite.util.Direction;
import edu.kit.scc.git.ggd.voxelite.world.Block;
import edu.kit.scc.git.ggd.voxelite.world.Chunk;
import edu.kit.scc.git.ggd.voxelite.world.Voxel;
import net.durchholz.beacon.math.Vec3f;

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

    public void updateMesh(Vec3f cameraPosition) {
        for (Voxel voxel : chunk) {
            final Block block = voxel.getBlock();
            if (block == null) continue;

            final ChunkProgram.Slice slice = slices[block.getRenderType().ordinal()];

            for (Direction direction : Direction.values()) {
                final Voxel neighbor = voxel.getNeighbor(direction);
                if (neighbor == null || neighbor.getBlock() == null) {

                    QuadMesh quadMesh = direction.getUnitQuad().translate(Chunk.toChunkSpace(voxel.position()));
                    QuadTexture quadTexture = block.getTexture(direction);
                    short quadPosition = (short) Chunk.toLinearSpace(voxel.position().add(direction.getAxis()));

                    slice.queue.add(new ChunkProgram.Slice.QueuedQuad(direction, quadMesh, quadTexture, quadPosition));
                }
            }
        }

        for (ChunkProgram.Slice slice : slices) {
            if (slice == null) continue; //TODO Remove with transparency
            slice.upload(cameraPosition);
        }
    }

    public void render(RenderType renderType, Vec3f cameraPosition) {
        slices[renderType.ordinal()].render(cameraPosition);
    }

    public int getQuadCount() {
        int count = 0;
        for (ChunkProgram.Slice slice : slices) {
            if (slice != null) count += slice.quadPositions.length; //TODO Remove null check with transparency
        }

        return count;
    }

    public void delete() {
        for (ChunkProgram.Slice slice : slices) {
            if(slice == null) return; //TODO Remove with transparency
            slice.vertexArray.delete();
            slice.meshBuffer.delete();
            slice.ibo.delete();
        }
    }
}
