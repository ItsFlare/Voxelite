package edu.kit.scc.git.ggd.voxelite.world.generator.noisemap;

import edu.kit.scc.git.ggd.voxelite.util.LinearInterpolation;
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
        return LinearInterpolation.value(vecList, x);
    }

}
