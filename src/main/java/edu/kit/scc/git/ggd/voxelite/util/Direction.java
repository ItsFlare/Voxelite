package edu.kit.scc.git.ggd.voxelite.util;

import edu.kit.scc.git.ggd.voxelite.render.QuadMesh;
import net.durchholz.beacon.math.Vec3i;

public enum Direction {
    POS_X(new Vec3i(1, 0, 0),   new QuadMesh(new Vec3i(1, 0, 1), new Vec3i(1, 1, 1), new Vec3i(1, 1, 0), new Vec3i(1, 0, 0))),
    NEG_X(new Vec3i(-1, 0, 0),  new QuadMesh(new Vec3i(0, 0, 0), new Vec3i(0, 1, 0), new Vec3i(0, 1, 1), new Vec3i(0, 0, 1))),
    POS_Y(new Vec3i(0, 1, 0),   new QuadMesh(new Vec3i(0, 1, 1), new Vec3i(0, 1, 0), new Vec3i(1, 1, 0), new Vec3i(1, 1 ,1))),
    NEG_Y(new Vec3i(0, -1, 0),  new QuadMesh(new Vec3i(1, 0, 1), new Vec3i(1, 0, 0), new Vec3i(0, 0, 0), new Vec3i(0, 0, 1))),
    POS_Z(new Vec3i(0, 0, 1),   new QuadMesh(new Vec3i(0, 0, 1), new Vec3i(0, 1, 1), new Vec3i(1, 1, 1), new Vec3i(1, 0, 1))),
    NEG_Z(new Vec3i(0, 0, -1),  new QuadMesh(new Vec3i(1, 0, 0), new Vec3i(1, 1, 0), new Vec3i(0, 1, 0), new Vec3i(0, 0, 0)));

    private final Vec3i    axis;
    private final QuadMesh unitQuad;

    Direction(Vec3i axis, QuadMesh unitQuad) {
        this.axis = axis;
        this.unitQuad = unitQuad;
    }

    public Vec3i getAxis() {
        return axis;
    }

    public QuadMesh getUnitQuad() {
        return unitQuad;
    }
}