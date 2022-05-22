package edu.kit.scc.git.ggd.voxelite.world.generator.pass;

import edu.kit.scc.git.ggd.voxelite.world.Block;
import edu.kit.scc.git.ggd.voxelite.world.Chunk;
import edu.kit.scc.git.ggd.voxelite.world.Voxel;
import edu.kit.scc.git.ggd.voxelite.world.generator.noise.Noise;
import edu.kit.scc.git.ggd.voxelite.world.generator.noise.SimplexNoise;
import edu.kit.scc.git.ggd.voxelite.world.generator.noisemap.ContinentalMap;
import edu.kit.scc.git.ggd.voxelite.world.generator.noisemap.ErosionMap;
import edu.kit.scc.git.ggd.voxelite.world.generator.noisemap.PVMap;
import net.durchholz.beacon.math.Vec2f;
import net.durchholz.beacon.math.Vec3f;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

public class TerrainPass implements GeneratorPass {
    private final Noise noise;
    private float startFrequency;
    private double startAmplitude;

    private int waterLevel = -10;

    private ContinentalMap continentalMap;

    private ErosionMap erosionMap;

    private PVMap pvMap;

    private int octave = 4;


    public TerrainPass(long seed) {
        this.noise = new SimplexNoise(seed);
        continentalMap = new ContinentalMap();
        erosionMap = new ErosionMap();
        pvMap = new PVMap();
    }

    @Override
    public void apply(Chunk chunk) {
        int dirtRange = 3;
        double maxOctaveValue = getMaxOctaveValue(octave);

        for (Voxel voxel : chunk) {
            Vec2f pos1 = new Vec2f(voxel.position().x() , voxel.position().z());
            Vec3f pos2 = new Vec3f(voxel.position().x(),voxel.position().y(), voxel.position().z());

            //double nois = noise.sample(pos1.scale(0.02f));
            //double height = continentalMap.value((float)nois);
            //double height = octave(4, pos1);
            float pos = (float) (octave(octave, pos1) / maxOctaveValue);
            double height = erosionMap.value(pos / 2) * (continentalMap.value(pos) + pvMap.value(pos));
            double density = noise.sample(pos2.scale(0.05f));
            if (voxel.position().y() < 0) {
                density = density + 0.2 * (-voxel.position().y());
            } else {
                density = density - (voxel.position().y() / 5000);
            }
            if (density < 0) {
                continue;
            }
            if((voxel.position().y() <  height) && (voxel.position().y() > (height - dirtRange))) {
                voxel.setBlock(Block.DIRT);
                if (voxel.position().y() >  height - 1) {
                    voxel.setBlock(Block.GRASS);
                }
            }
            if (voxel.position().y() <  (height - dirtRange)) {
                voxel.setBlock(Block.STONE);
            }
            if ((voxel.position().y() > height) && (voxel.position().y() < waterLevel)) {
                voxel.setBlock(Block.COBBLESTONE);
            }

        }
    }

    private double octave(int n, Vec2f pos) {
        double noiseValue = 0;
        for(int i = 0; i < n; i++) {
            float amplitude = (float) (1/Math.pow(2, i));
            float frequency = 1 / amplitude;
            noiseValue += noise.sample(pos.scale(startFrequency * frequency)) * amplitude * startAmplitude;
        }
        return noiseValue;
    }

    private double getMaxOctaveValue(int octaves) {
        double result = 0;
        for (int i = 0; i < octaves; i++) {
            float amplitude = (float) (1/Math.pow(2, i));
            result += amplitude * startAmplitude;
        }
        return result;
    }




    @Override
    public void setFrequency(float startFrequency) {
        this.startFrequency = startFrequency;
    }

    @Override
    public void setAmplitude(int amplitude) {
        this.startAmplitude = amplitude;
    }


}
