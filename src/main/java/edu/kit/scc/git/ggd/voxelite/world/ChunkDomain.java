package edu.kit.scc.git.ggd.voxelite.world;

import net.durchholz.beacon.math.Vec3i;

public interface ChunkDomain {
    Chunk getChunk(Vec3i position);
}
