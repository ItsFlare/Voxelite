package edu.kit.scc.git.ggd.voxelite.world.generator.noisemap;

import edu.kit.scc.git.ggd.voxelite.util.LinearInterpolation;
import net.durchholz.beacon.math.Vec2f;

import java.util.List;

abstract class NoiseMap {


    abstract float value(float x);
}
