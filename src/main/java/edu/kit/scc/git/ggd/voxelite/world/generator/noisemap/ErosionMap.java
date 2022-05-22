package edu.kit.scc.git.ggd.voxelite.world.generator.noisemap;

import net.durchholz.beacon.math.Vec2f;



public class ErosionMap {

    private Vec2f[] vecList = {
            new Vec2f(-1,1),
            new Vec2f(-0.5f,0.75f),
            new Vec2f(0.1f,0.5f),
            new Vec2f(0.5f,0.6f),
            new Vec2f(1,0.1f),
    };

    public ErosionMap() {
    }


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
