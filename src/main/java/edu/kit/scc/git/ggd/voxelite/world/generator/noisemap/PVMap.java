package edu.kit.scc.git.ggd.voxelite.world.generator.noisemap;

import net.durchholz.beacon.math.Vec2f;

public class PVMap {

    private Vec2f[] vecList = {
            new Vec2f(-1,1),
            new Vec2f(-0.7f,-2),
            new Vec2f(-0.5f,4),
            new Vec2f(-0.4f,6),
            new Vec2f(-0.3f,9),
            new Vec2f(0.1f,13),
            new Vec2f(0.5f,15),
            new Vec2f(1,16),
    };

    public float value(float x) {
        if (x < -1 && x > 1) {
            throw new IllegalArgumentException("x must be in range of -1 and 1");
        }
        for(int i = 0; i < vecList.length - 1; i++) {
            if (x >= vecList[i].x() && x <= vecList[i + 1].x()) {
                float result = vecList[i].y() + ((vecList[i + 1].y() - vecList[i].y()) / (vecList[i + 1].x() - vecList[i].x())) * (x - vecList[i].x());
                return result;
            }

        }
        throw new IllegalArgumentException("Error");
    }

}
