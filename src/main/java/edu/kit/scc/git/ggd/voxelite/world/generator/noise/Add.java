package edu.kit.scc.git.ggd.voxelite.world.generator.noise;

import net.durchholz.beacon.math.Vec2f;
import net.durchholz.beacon.math.Vec3f;

public record Add(Noise a, Noise b) implements Noise {

    @Override
    public float sample(Vec3f position) {
        return a.sample(position) * b.sample(position);
    }

    @Override
    public float sample(Vec2f position) {
        return a.sample(position) * b.sample(position);
    }
}
