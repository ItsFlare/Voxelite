package edu.kit.scc.git.ggd.voxelite.world.generator.noise;

import net.durchholz.beacon.math.Vec2f;
import net.durchholz.beacon.math.Vec2i;
import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.math.Vec3i;

public interface Noise {
    float sample(Vec2f position);
    float sample(Vec3f position);

    default float sample(Vec2i position) {
        return sample(new Vec2f(position));
    }

    default float sample(Vec3i position) {
        return sample(new Vec3f(position));
    }
}