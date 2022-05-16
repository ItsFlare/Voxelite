package edu.kit.scc.git.ggd.voxelite.world.generator.pass;

import edu.kit.scc.git.ggd.voxelite.world.Block;
import edu.kit.scc.git.ggd.voxelite.world.Chunk;
import edu.kit.scc.git.ggd.voxelite.world.Voxel;
import edu.kit.scc.git.ggd.voxelite.world.generator.noise.Noise;
import edu.kit.scc.git.ggd.voxelite.world.generator.noise.SimplexNoise;
import net.durchholz.beacon.math.Vec2f;
import net.durchholz.beacon.math.Vec3f;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

public class TerrainPass implements GeneratorPass {
    private final Noise noise;
    private float startFrequency;
    private double startAmplitude;
    private PolynomialSplineFunction function;

    public TerrainPass(long seed) {
        this.noise = new SimplexNoise(seed);
        setSplineFunction();
    }

    @Override
    public void apply(Chunk chunk) {
        double squashFactor = 0.3;
        int dirtRange = 3;

        for (Voxel voxel : chunk) {
            Vec2f pos1 = new Vec2f(voxel.position().x() , voxel.position().z());
            Vec3f pos2 = new Vec3f(voxel.position().x(),voxel.position().y(), voxel.position().z());

            double height = octave(4, pos1);
            double density = noise.sample(pos2.scale(0.05f));
            if (voxel.position().y() < 0) {
                density = density + 0.05 * (-voxel.position().y());
            } else {
                density = density - (0.02 * (voxel.position().y() / 10));
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

        }
    }

    //TODO how does octave wokr with spline
    //linear spline interpolation
    private double octave(int n, Vec2f pos) {
        /*float startFrequency = 0.02f;
        double startAmplitude = 20;*/
        double noiseValue = 0;
        for(int i = 0; i < n; i++) {
            float amplitude = (float) (1/Math.pow(2, i));
            float frequency = 1 / amplitude;
            noiseValue += noise.sample(pos.scale(startFrequency * frequency)) * amplitude * startAmplitude;
        }
        return noiseValue;
    }

    //TODO alternative height with splines
    private void setSplineFunction() {
        SplineInterpolator splineInterpolator = new SplineInterpolator();
        double[] x = {-1 , -0.3, 0.4, 1};
        double[] y = {1, 0, 1, 2};
        function = splineInterpolator.interpolate(x, y);
       /* for (double i = -1; i < 1; i = i + 0.1) {
            System.out.println(i + ";"+ function.value(i));
        }*/
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
