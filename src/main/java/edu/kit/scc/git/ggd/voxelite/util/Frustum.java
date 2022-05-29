package edu.kit.scc.git.ggd.voxelite.util;

import net.durchholz.beacon.math.*;

import java.util.Arrays;

public record Frustum(Vec3f position, Vec4f[] normals, AABB boundingBox) {

    private static final AABB UNIT = new AABB(new Vec3i(-1), new Vec3i(1));

    public Frustum(Vec3f position, Matrix4f viewProjection) {
        this(position, extractNormals(viewProjection), extractBoundingBox(viewProjection));
    }

    public boolean intersects(AABB aabb) {
        //TODO Check if aabb intersects with frustum aabb?
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
    
    private static AABB extractBoundingBox(Matrix4f viewProjection) {
        float minX = Float.POSITIVE_INFINITY,
                minY = Float.POSITIVE_INFINITY,
                minZ = Float.POSITIVE_INFINITY,
                maxX = Float.NEGATIVE_INFINITY,
                maxY = Float.NEGATIVE_INFINITY,
                maxZ = Float.NEGATIVE_INFINITY;

        var inverseViewProjection = viewProjection.clone();
        inverseViewProjection.invert();

        //TODO Optimize
        for (Vec4f corner : Arrays.stream(UNIT.corners()).map(vec3f -> {
            Vec4f transform = vec3f.extend(1).transform(inverseViewProjection);
            transform = transform.divide(transform.w());
            return transform;
        }).toArray(Vec4f[]::new)) {
            if(corner.x() < minX) minX = corner.x();
            if(corner.y() < minY) minY = corner.y();
            if(corner.z() < minZ) minZ = corner.z();

            if(corner.x() > maxX) maxX = corner.x();
            if(corner.y() > maxY) maxY = corner.y();
            if(corner.z() > maxZ) maxZ = corner.z();
        }

        return new AABB(new Vec3f(minX, minY, minZ), new Vec3f(maxX, maxY, maxZ));
    }
}
