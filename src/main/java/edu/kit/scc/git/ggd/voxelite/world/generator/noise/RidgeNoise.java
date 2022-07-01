package edu.kit.scc.git.ggd.voxelite.world.generator.noise;

public record RidgeNoise(boolean invert, Noise noise) implements SimpleNoise {

    public float sample(float noise) {
        return (invert ? -1 : 1) * (Math.abs(noise) * -2 + 1);
    }
}
