package edu.kit.scc.git.ggd.voxelite.world.generator.noise;

import net.durchholz.beacon.math.Vec2f;
import net.durchholz.beacon.math.Vec3f;

public interface SimpleNoise extends Noise {
    @Override
    default float sample(Vec2f position) {
        return sample(noise().sample(position));
    }

    @Override
    default float sample(Vec3f position) {
        return sample(noise().sample(position));
    }

    float sample(float noise);
    Noise noise();
}
