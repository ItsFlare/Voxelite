package edu.kit.scc.git.ggd.voxelite.util;

import net.durchholz.beacon.math.AABB;
import net.durchholz.beacon.math.Matrix4f;
import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.math.Vec4f;

public record Frustum(Vec3f position, Vec4f[] normals) {

    public Frustum(Vec3f position, Matrix4f viewProjection) {
        this(position, extractNormals(viewProjection));
    }

    public boolean intersects(AABB aabb) {
        final AABB relative = aabb.translate(position.scale(-1));

        final float minX = relative.min().x();
        final float minY = relative.min().y();
        final float minZ = relative.min().z();

        final float maxX = relative.max().x();
        final float maxY = relative.max().y();
        final float maxZ = relative.max().z();

        for (Vec4f normal : normals) {
            //If all corners are behind the plane it can't intersect
            if (
                    normal.dot(new Vec4f(minX, minY, minZ, 1)) <= 0 &&
                    normal.dot(new Vec4f(maxX, minY, minZ, 1)) <= 0 &&
                    normal.dot(new Vec4f(minX, maxY, minZ, 1)) <= 0 &&
                    normal.dot(new Vec4f(minX, minY, maxZ, 1)) <= 0 &&

                    normal.dot(new Vec4f(maxX, maxY, maxZ, 1)) <= 0 &&
                    normal.dot(new Vec4f(minX, maxY, maxZ, 1)) <= 0 &&
                    normal.dot(new Vec4f(maxX, minY, maxZ, 1)) <= 0 &&
                    normal.dot(new Vec4f(maxX, maxY, minZ, 1)) <= 0
            ) {
                return false;
            }
        }

        return true;
    }

    private static Vec4f[] extractNormals(Matrix4f viewProjection) {
        /*
        Gribb and Hartmann. Fast Extraction of Viewing Frustum Planes from the WorldView-Projection Matrix. 2001.
         */

        final var normals = new Vec4f[6];

        normals[0] = viewProjection.row(3).add(viewProjection.row(0));
        normals[1] = viewProjection.row(3).subtract(viewProjection.row(0));
        normals[2] = viewProjection.row(3).subtract(viewProjection.row(1));
        normals[3] = viewProjection.row(3).add(viewProjection.row(1));
        normals[4] = viewProjection.row(3).add(viewProjection.row(2));
        normals[5] = viewProjection.row(3).subtract(viewProjection.row(2));

        return normals;
    }
}
