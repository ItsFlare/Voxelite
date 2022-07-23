package edu.kit.scc.git.ggd.voxelite.render;

import net.durchholz.beacon.math.Vec3i;

public record QuadMesh(Vec3i v0, Vec3i v1, Vec3i v2, Vec3i v3, Vec3i tangent, Vec3i bitangent) {
    public QuadMesh translate(Vec3i vector) {
        return new QuadMesh(v0.add(vector), v1.add(vector), v2.add(vector), v3.add(vector), tangent, bitangent);
    }
}
