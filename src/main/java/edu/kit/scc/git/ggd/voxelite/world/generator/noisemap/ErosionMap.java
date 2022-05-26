package edu.kit.scc.git.ggd.voxelite.world.generator.noisemap;

import edu.kit.scc.git.ggd.voxelite.util.LinearInterpolation;
import net.durchholz.beacon.math.Vec2f;



public class ErosionMap extends NoiseMap {

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
        return LinearInterpolation.value(vecList, x);
    }

}
