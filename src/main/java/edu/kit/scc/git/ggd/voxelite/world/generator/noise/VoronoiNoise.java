package edu.kit.scc.git.ggd.voxelite.world.generator.noise;

import net.durchholz.beacon.math.Vec2f;
import net.durchholz.beacon.math.Vec3f;

public class VoronoiNoise implements Noise {
    int blockToSizeRatio = 4; // x blocks form a tile (must be of 2^n)

    int tileSize = 4; // entire noise consist of nxn tiles
    Vec2f[][] randNumArray = new Vec2f[tileSize][tileSize]; // save the rand num of a tile

    public VoronoiNoise() {
        init();
    }

    private void init() {
        for (int i = 0; i < (tileSize * tileSize); i++) {
            randNumArray[i / tileSize][i % tileSize] = new Vec2f((float) Math.random(), (float) Math.random());
        }
    }

    @Override
    public float sample(Vec2f position) {
        Vec2f randomNum = randNumArray[(int) position.x()][(int) position.y()];
        Vec2f pos = new Vec2f((float) Math.floor(position.x() / blockToSizeRatio), (float) Math.floor(position.y() / blockToSizeRatio));
        Vec2f point = pos.add(randomNum);
        if (point.x() >= pos.x() / blockToSizeRatio && point.x() <= (pos.x() / blockToSizeRatio + 1 / (float) blockToSizeRatio)) {
            if (point.y() >= pos.y() / blockToSizeRatio && point.y() <= (pos.y() / blockToSizeRatio + 1 / (float) blockToSizeRatio)) {
                return 1;
            }
        }
        return 0;
    }

    @Override
    public float sample(Vec3f position) {
        return 0;
    }
}
