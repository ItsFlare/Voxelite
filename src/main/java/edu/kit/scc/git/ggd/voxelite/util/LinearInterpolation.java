package edu.kit.scc.git.ggd.voxelite.util;

import net.durchholz.beacon.math.Vec2f;

public class LinearInterpolation {

    public static float value(Vec2f[] vecList, float x) {
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
