package edu.kit.scc.git.ggd.voxelite.world.generator.noisemap;

import edu.kit.scc.git.ggd.voxelite.util.LinearInterpolation;
import net.durchholz.beacon.math.Vec2f;

public class ContinentalMap extends NoiseMap {
    private Vec2f[] vecList = {
            new Vec2f(-1,-10),
            new Vec2f(-0.8f,-100),
            new Vec2f(-0.6f,10),
            new Vec2f(-0.5f,-40),
            new Vec2f(-0.1f,-10),
            new Vec2f(0.1f,40),
            new Vec2f(0.2f,40),
            new Vec2f(0.3f,50),
            new Vec2f(0.4f,70),
            new Vec2f(1,150),
    };


    public ContinentalMap() {}

    public float value(float x) {
        return LinearInterpolation.value(vecList, x);
    }
}
