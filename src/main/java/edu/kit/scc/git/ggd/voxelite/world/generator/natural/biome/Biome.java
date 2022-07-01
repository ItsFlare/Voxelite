package edu.kit.scc.git.ggd.voxelite.world.generator.natural.biome;

import edu.kit.scc.git.ggd.voxelite.Main;
import edu.kit.scc.git.ggd.voxelite.world.Block;
import edu.kit.scc.git.ggd.voxelite.world.Voxel;
import edu.kit.scc.git.ggd.voxelite.world.generator.natural.NaturalWorldGenerator;
import net.durchholz.beacon.math.Vec3i;

public enum Biome {
    OCEAN(voxel -> true, 0f),
    BEACH(voxel -> {
        float temperature = Main.INSTANCE.getWorld().getGenerator().getTemperature().sample(voxel.position());
        Block block = temperature < 0 ? Block.STONE : Block.SAND;
        for (int y = 0; y < 3; y++) {
            final Voxel relative = voxel.getRelative(new Vec3i(0, -y, 0));
            if(relative != null) relative.setBlock(block);
        }
        return true;
    }, 0.5f),
    PLAINS(voxel -> {
        voxel.setBlock(Block.GRASS);
        for (int y = 1; y < 3; y++) {
            final Voxel relative = voxel.getRelative(new Vec3i(0, -y, 0));
            if(relative != null) relative.setBlock(Block.DIRT);
        }
        return true;
    }, 0.25f, new OakTreeFeature()),
    FOREST(voxel -> {
        voxel.setBlock(Block.GRASS);
        for (int y = 1; y < 3; y++) {
            final Voxel relative = voxel.getRelative(new Vec3i(0, -y, 0));
            if(relative != null) relative.setBlock(Block.DIRT);
        }
        return true;
    }, 0.1f, new OakTreeFeature()),
    MOUNTAINS(voxel -> {
        voxel.setBlock(Block.STONE);
        return true;
    }, 0.25f),
    DESERT(voxel -> {
        voxel.setBlock(Block.SAND);
        return true;
    }, 0.5f, new CactusFeature()),
    SNOW(voxel -> {
        voxel.setBlock(Block.WHITE_GLASS);
        return true;
    }, 0.5f);

    private final TerrainFeature surfaceLayer;
    private final TerrainFeature[] features;
    private final float featureDensity;

    Biome(TerrainFeature surfaceLayer, float featureDensity, TerrainFeature... features) {
        this.surfaceLayer = surfaceLayer;
        this.features = features;
        this.featureDensity = featureDensity;
    }

    public TerrainFeature getSurfaceLayer() {
        return surfaceLayer;
    }

    public TerrainFeature[] getFeatures() {
        return features;
    }

    public float getFeatureDensity() {
        return featureDensity;
    }

    public static Biome select(NaturalWorldGenerator.NoisePoint noisePoint) {
        //TODO Replace with automatic bracket selection
        final float temperature = noisePoint.temperature();
        final float humidity = noisePoint.humidity();
        if(noisePoint.continentalness() <= 0) return OCEAN;
        if(noisePoint.continentalness() < 0.1) return BEACH;

        if(noisePoint.continentalness() > 0.75f) {
            return MOUNTAINS;
        } else {
            if(temperature < -0.5f) {
                return SNOW;
            } else if (temperature < 0.5f) {
                if(humidity < 0) return PLAINS;
                else return FOREST;
            } else if (temperature < 1f) {
                return DESERT;
            }
        }

        throw new IllegalStateException();
    }
}
