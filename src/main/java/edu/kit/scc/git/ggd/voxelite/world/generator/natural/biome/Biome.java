package edu.kit.scc.git.ggd.voxelite.world.generator.natural.biome;

import edu.kit.scc.git.ggd.voxelite.Main;
import edu.kit.scc.git.ggd.voxelite.world.Block;
import edu.kit.scc.git.ggd.voxelite.world.Voxel;
import net.durchholz.beacon.math.Vec3i;

public enum Biome {
    OCEAN(voxel -> true),
    BEACH(voxel -> {
        float temperature = Main.INSTANCE.getWorld().getGenerator().getTemperature().sample(voxel.position());
        Block block = temperature < 0 ? Block.STONE : Block.SAND;
        for (int y = 0; y < 3; y++) {
            final Voxel relative = voxel.getRelative(new Vec3i(0, -y, 0));
            if(relative != null) relative.setBlock(block);
        }
        return true;
    }),
    PLAINS(voxel -> {
        voxel.setBlock(Block.GRASS);
        for (int y = 1; y < 3; y++) {
            final Voxel relative = voxel.getRelative(new Vec3i(0, -y, 0));
            if(relative != null) relative.setBlock(Block.DIRT);
        }
        return true;
    }, new Structure(3, 5, new BirchTreeFeature())),
    FOREST(voxel -> {
        voxel.setBlock(Block.GRASS);
        for (int y = 1; y < 3; y++) {
            final Voxel relative = voxel.getRelative(new Vec3i(0, -y, 0));
            if(relative != null) relative.setBlock(Block.DIRT);
        }
        return true;
    },  new Structure(1, 3, new OakTreeFeature())),
    MOUNTAINS(voxel -> {
        voxel.setBlock(Block.STONE);
        return true;
    }),
    DESERT(voxel -> {
        voxel.setBlock(Block.SAND);
        return true;
    },  new Structure(0, 2, new CactusFeature())),
    SNOW(voxel -> {
        voxel.setBlock(Block.WHITE_GLASS);
        return true;
    },  new Structure(0, 3, new AcaciaTreeFeature()));

    private final TerrainFeature surfaceLayer;

    private final Structure[] structures;

    Biome(TerrainFeature surfaceLayer, Structure... structures) {
        this.surfaceLayer = surfaceLayer;
        this.structures = structures;

    }

    public TerrainFeature getSurfaceLayer() {
        return surfaceLayer;
    }

    public Structure[] getStructures() {
        return structures;
    }

}
