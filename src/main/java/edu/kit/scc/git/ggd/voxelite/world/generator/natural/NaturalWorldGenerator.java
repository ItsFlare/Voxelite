package edu.kit.scc.git.ggd.voxelite.world.generator.natural;

import edu.kit.scc.git.ggd.voxelite.util.Util;
import edu.kit.scc.git.ggd.voxelite.world.World;
import edu.kit.scc.git.ggd.voxelite.world.generator.GeneratorChunk;
import edu.kit.scc.git.ggd.voxelite.world.generator.MultiPassGenerator;
import edu.kit.scc.git.ggd.voxelite.world.generator.natural.pass.GeneratorPass;
import edu.kit.scc.git.ggd.voxelite.world.generator.natural.pass.GeneratorPassInstance;
import edu.kit.scc.git.ggd.voxelite.world.generator.natural.pass.SurfacePass;
import edu.kit.scc.git.ggd.voxelite.world.generator.natural.pass.TerrainPass;
import edu.kit.scc.git.ggd.voxelite.world.generator.noise.*;
import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.math.Vec3i;

import java.util.function.Function;

public class NaturalWorldGenerator implements MultiPassGenerator<NaturalWorldGenerator> {

    private final Noise continentalnessNoise;
    private final Noise erosionNoise;
    private final Noise ridgeNoise;
    private final Noise surfaceNoise;

    private final long        seed;
    private final TerrainPass terrainPass;
    private final SurfacePass surfacePass;
    private       World               world;
    public        float               frequency = 0.001f;

    public final LinearSpline ridgeCoast;
    public final LinearSpline ridgeInlandSteep;
    public final LinearSpline ridgeInlandEroded;

    public final IndirectSpline<NoisePoint> erosionCoast;
    public final IndirectSpline<NoisePoint> erosionInland;

    public final IndirectSpline<NoisePoint> baseHeightSpline;
    private      float                      masterScale = 1;

    public record NoisePoint(float continentalness, float erosion, float ridge) {
        public NoisePoint clamp(float min, float max) {
            return new NoisePoint(Util.clamp(continentalness, min, max), Util.clamp(erosion, min, max), Util.clamp(ridge, min, max));
        }
    }

    public NaturalWorldGenerator(long seed) {
        this.seed = seed;
        this.continentalnessNoise = new Noise2D(new FBM(new SimplexNoise(seed + 1), 4, 4.3f, 0.3f, 0.001f, 1.1f));
        this.erosionNoise = new Noise2D(new FBM(new SimplexNoise(seed + 2), 2, 2f, 0.5f, 0.001f, 1.1f));
        this.ridgeNoise = new Noise2D(new Spline(createRidgeSpline(0.8f, -0.2f), new FBM(new SimplexNoise(seed + 3), 1, 2f, 0.25f, 0.005f, 1f)));
        this.surfaceNoise = new FBM(new SimplexNoise(seed + 4), 4, 1.75f, 0.3f, 0.02f, 1f);

        //Coast
        ridgeCoast = new LinearSpline(
                new LinearSpline.Point(-1, -30),
                new LinearSpline.Point(0, 0),
                new LinearSpline.Point(1, 10)
        );

        erosionCoast = new IndirectSpline<>(NoisePoint::erosion,
                new IndirectSpline.Point<>(-1, 10, point -> ridgeCoast.sample(point.ridge)),
                new IndirectSpline.Point<>(1, 0)
        );

        //Inland
        ridgeInlandSteep = new LinearSpline(
                new LinearSpline.Point(-1, -10),
                new LinearSpline.Point(-0.75f, -10),
                new LinearSpline.Point(-0.5f, 0),
                new LinearSpline.Point(0, 0),
                new LinearSpline.Point(0.25f, 0),
                new LinearSpline.Point(1, 20)
        );

        ridgeInlandEroded = new LinearSpline(
                new LinearSpline.Point(-1, -10),
                new LinearSpline.Point(0, 0),
                new LinearSpline.Point(1, 20)
        );

        erosionInland = new IndirectSpline<>(NoisePoint::erosion,
                new IndirectSpline.Point<>(-1, 25, point -> ridgeInlandSteep.sample(point.ridge)),
                new IndirectSpline.Point<>(1, 0, point -> ridgeInlandEroded.sample(point.ridge))
        );


        baseHeightSpline = new IndirectSpline<>(NoisePoint::continentalness,
                new IndirectSpline.Point<>(-1, 0),
                new IndirectSpline.Point<>(-0.85f, 0),
                new IndirectSpline.Point<>(-0.8f, 64),
                new IndirectSpline.Point<>(-0.3f, 64),
                new IndirectSpline.Point<>(-0.2f, TerrainPass.SEA_LEVEL - 16),
                new IndirectSpline.Point<>(0, TerrainPass.SEA_LEVEL, erosionCoast::sample),
                new IndirectSpline.Point<>(0.08f, TerrainPass.SEA_LEVEL + 10, erosionCoast::sample),
                new IndirectSpline.Point<>(0.2f, TerrainPass.SEA_LEVEL + 50, erosionInland::sample),
                new IndirectSpline.Point<>(1, TerrainPass.SEA_LEVEL + 128, erosionInland::sample)
        );

        terrainPass = new TerrainPass(this);
        surfacePass = new SurfacePass(this);
    }

    @Override
    public World getWorld() {
        return world;
    }

    @Override
    public void setWorld(World world) {
        this.world = world;
    }

    public long getSeed() {
        return seed;
    }

    public NoisePoint sampleNoises(Vec3f position) {
        float continentalness = continentalnessNoise.sample(position);
        float erosion = erosionNoise.sample(position);
        float ridge = ridgeNoise.sample(position);

        return new NoisePoint(continentalness, erosion, ridge).clamp(-1, 1);
    }

    @Override
    public GeneratorPass<NaturalWorldGenerator>[] getPasses() {
        return Pass.values();
    }

    @Override
    public GeneratorPass<NaturalWorldGenerator> getFirstPass() {
        return getPasses()[0];
    }

    public TerrainPass getTerrainPass() {
        return terrainPass;
    }

    public SurfacePass getSurfacePass() {
        return surfacePass;
    }

    public int getBaseHeight(Vec3i position) {
        return getBaseHeight(new Vec3f(position));
    }

    public int getBaseHeight(Vec3f position) {
        return getBaseHeight(sampleNoises(position).clamp(-1, 1));
    }

    public int getBaseHeight(NoisePoint noisePoint) {
        return (int) baseHeightSpline.sample(noisePoint);
    }

    public Noise getContinentalness() {
        return continentalnessNoise;
    }

    public Noise getErosion() {
        return erosionNoise;
    }

    public Noise getRidge() {
        return ridgeNoise;
    }

    public Noise getSurfaceNoise() {
        return surfaceNoise;
    }

    public IndirectSpline<NoisePoint> getBaseHeightSpline() {
        return baseHeightSpline;
    }

    public enum Pass implements GeneratorPass<NaturalWorldGenerator> {
        TERRAIN(NaturalWorldGenerator::getTerrainPass),
        SURFACE(NaturalWorldGenerator::getSurfacePass);

        private final Function<NaturalWorldGenerator, GeneratorPassInstance<NaturalWorldGenerator>> pass;

        Pass(Function<NaturalWorldGenerator, GeneratorPassInstance<NaturalWorldGenerator>> pass) {
            this.pass = pass;
        }

        @Override
        public void apply(NaturalWorldGenerator generator, GeneratorChunk<NaturalWorldGenerator> chunk) {
            pass.apply(generator).apply(chunk);
        }

        @Override
        public GeneratorPass<NaturalWorldGenerator>[] getValues() {
            return values();
        }
    }

    private static LinearSpline createRidgeSpline(float ridgeCenter, float valleyCenter) {
        assert ridgeCenter > 0;
        assert valleyCenter < 0;

        final float ridgeWidth = Math.min(1 - ridgeCenter, ridgeCenter);
        final float valleyWidth = Math.min(1 + valleyCenter, -valleyCenter);

        return new LinearSpline(
                new LinearSpline.Point(-1, 0),
                new LinearSpline.Point(valleyCenter - valleyWidth, 0),
                new LinearSpline.Point(valleyCenter, -1),
                new LinearSpline.Point(valleyCenter + valleyWidth, 0),

                new LinearSpline.Point(ridgeCenter - ridgeWidth, 0),
                new LinearSpline.Point(ridgeCenter, 1),
                new LinearSpline.Point(ridgeCenter + ridgeWidth, 0),
                new LinearSpline.Point(1, 0)
        );
    }
}
