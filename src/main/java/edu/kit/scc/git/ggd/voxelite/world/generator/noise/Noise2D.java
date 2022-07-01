package edu.kit.scc.git.ggd.voxelite.world.generator.noise;

import net.durchholz.beacon.math.Vec2f;
import net.durchholz.beacon.math.Vec3f;

public record Noise2D(Noise noise) implements Noise {
    @Override
    public float sample(Vec3f position) {
        return noise.sample(position.xz());
    }

    @Override
    public float sample(Vec2f position) {
        return noise.sample(position);
    }
}
