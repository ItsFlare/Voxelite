package edu.kit.scc.git.ggd.voxelite.world;

import net.durchholz.beacon.math.Vec3i;

public interface LightStorage {
    int RANGE_EXP    = 5;
    int CHANNELS_EXP = 1;

    int RANGE    = 1 << RANGE_EXP;
    int CHANNELS = 1 << CHANNELS_EXP;

    int MAX_COMPONENT_VALUE = RANGE - 1;
    int MAX_TOTAL_VALUE = MAX_COMPONENT_VALUE << CHANNELS_EXP;

    Vec3i getLight(Vec3i position);
}
