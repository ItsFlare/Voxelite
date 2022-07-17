package edu.kit.scc.git.ggd.voxelite.world.generator.noise;

import net.durchholz.beacon.math.Vec2f;
import net.durchholz.beacon.math.Vec3f;

public class VoronoiNoise implements Noise {

    int samplePoints;
    private final int tileSize = 4; // entire noise consist of nxn tiles
    private final Vec2f[][] randNumArray = new Vec2f[tileSize][tileSize]; // save the rand num of a tile

    public VoronoiNoise() {
        for (int x = 0; x < tileSize; x++) {
            for (int y = 0; y < tileSize; y++) {
                randNumArray[x][y] = new Vec2f((float) Math.random(), (float) Math.random());
            }
        }
    }

    @Override
    public float sample(Vec2f position) {
        int blockWidth = 8;
        int gridWidth = 32;

        //calculates the starting position in grid
        int gridPosX = position.x() < 0 ? (int) -((position.x() / blockWidth) % tileSize) : (int) ((position.x() / blockWidth) % tileSize);
        int gridPosY = position.y() < 0 ? (int) -((position.y() / blockWidth) % tileSize) : (int) ((position.y() / blockWidth) % tileSize);

        //calculate the starting position of the block in the grid
        float blockPosX = position.x() < 0 ? -((position.x() % gridWidth) % blockWidth) : ((position.x() % gridWidth) % blockWidth);
        float blockPosY = position.y() < 0 ? -((position.y() % gridWidth) % blockWidth) : ((position.y() % gridWidth) % blockWidth);

        float lowerXLimit = gridPosX + blockPosX / (float) blockWidth;
        float upperXLimit = lowerXLimit + 1 / (float) blockWidth;
        float lowerYLimit = gridPosY + blockPosY / (float) blockWidth;
        float upperYLimit = lowerYLimit + 1 / (float) blockWidth;

        Vec2f randomNum = randNumArray[gridPosX][gridPosY];
        Vec2f pos = new Vec2f((float) Math.floor(lowerXLimit), (float) Math.floor(lowerYLimit));
        Vec2f point = pos.add(randomNum);

        if (point.x() >= lowerXLimit && point.x() <= upperXLimit) {
            if (point.y() >= lowerYLimit && point.y() <= upperYLimit) {
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
