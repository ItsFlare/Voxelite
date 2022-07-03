package edu.kit.scc.git.ggd.voxelite.world.generator.noise;

public record Spline(LinearSpline spline, Noise noise) implements SimpleNoise {
    @Override
    public float sample(float noise) {
        return spline.sample(noise);
    }
}
