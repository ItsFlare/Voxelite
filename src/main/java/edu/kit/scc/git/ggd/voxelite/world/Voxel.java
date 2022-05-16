package edu.kit.scc.git.ggd.voxelite.world;

import edu.kit.scc.git.ggd.voxelite.util.Direction;
import net.durchholz.beacon.math.Vec3i;
import org.jetbrains.annotations.Nullable;

public record Voxel(Chunk chunk, Vec3i position) {

    public Block getBlock() {
        return chunk.getBlock(position);
    }

    public void setBlock(Block block) {
        chunk.setBlock(position, block);
    }

    @Nullable
    public Voxel getRelative(Vec3i relative) {
        final Vec3i relativePosition = position.add(relative);
        final Vec3i chunkPosition = Chunk.toChunkPosition(relativePosition);

        if(chunkPosition.equals(chunk.getPosition())) {
            return new Voxel(chunk, relativePosition);
        } else {
            final Chunk c = chunk.getWorld().getChunk(chunkPosition);
            return c == null ? null : new Voxel(c, relativePosition);
        }
    }

    public Voxel getNeighbor(Direction direction) {
        return getRelative(direction.getAxis());
    }
}
