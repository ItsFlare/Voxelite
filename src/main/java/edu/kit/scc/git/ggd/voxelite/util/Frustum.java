package edu.kit.scc.git.ggd.voxelite.util;

import net.durchholz.beacon.math.*;

import java.util.Arrays;

public record Frustum(Vec3f position, Vec4f[] normals, Vec3f[] corners) {

    private static final AABB UNIT = new AABB(new Vec3i(-1), new Vec3i(1));

    public Frustum(Vec3f position, Matrix4f viewProjection) {
        this(position, extractNormals(viewProjection), extractCorners(viewProjection));
    }

    public boolean intersects(AABB aabb) {
        //TODO Check if aabb intersects with frustum aabb?

        final float minX = aabb.min().x();
        final float minY = aabb.min().y();
        final float minZ = aabb.min().z();

        final float maxX = aabb.max().x();
        final float maxY = aabb.max().y();
        final float maxZ = aabb.max().z();

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

    public static Vec3f[] extractCorners(Matrix4f viewProjection) {
        var clone = viewProjection.clone();
        clone.invert();
        return Arrays.stream(UNIT.corners()).map(vec3f -> {
            Vec4f transform = vec3f.extend(1).transform(clone);
            transform = transform.divide(transform.w());
            return new Vec3f(transform.x(), transform.y(), transform.z());
        }).toArray(Vec3f[]::new);
    }
}
