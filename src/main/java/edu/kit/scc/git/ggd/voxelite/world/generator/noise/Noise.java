package edu.kit.scc.git.ggd.voxelite.world.generator.noise;

import net.durchholz.beacon.math.Vec2f;
import net.durchholz.beacon.math.Vec3f;

public interface Noise {
    double sample(Vec3f position);
    double sample(Vec2f position);
}
