package edu.kit.scc.git.ggd.voxelite.world.generator.noise;

import edu.kit.scc.git.ggd.voxelite.util.Util;
import net.durchholz.beacon.math.Vec2f;
import net.durchholz.beacon.math.Vec3f;

public record FBM(Noise noise, int octaves, float lacunarity, float gain, float frequency, float amplitude, float max) implements Noise {

    /* 1/f noise */
    public FBM(Noise noise, int octaves) {
        this(noise, octaves, 2, 0.5f);
    }

    public FBM(Noise noise, int octaves, float lacunarity, float gain) {
        this(noise, octaves, lacunarity, gain, 1, 1);
    }

    public FBM(Noise noise, int octaves, float lacunarity, float gain, float frequency, float amplitude) {
        this(noise, octaves, lacunarity, gain, frequency, amplitude, calculateMax(octaves, gain));
    }

    public float sample(Vec2f position) {
        float result = 0;

        float f = 1;
        float a = 1;

        for (int i = 0; i < octaves; i++) {
            result += noise.sample(position.scale(frequency * f)) * a;
            f *= lacunarity;
            a *= gain;
        }

        return Util.clamp(result / max, -1, 1) * amplitude;
    }

    public float sample(Vec3f position) {
        float result = 0;

        float f = 1;
        float a = 1;

        for (int i = 0; i < octaves; i++) {
            result += noise.sample(position.scale(frequency * f)) * a;
            f *= lacunarity;
            a *= gain;
        }

        return Util.clamp(result / max, -1, 1) * amplitude;
    }

    //TODO Find analytical solution
    private static float calculateMax(int octaves, float gain) {
        float max = 0;

        float a = 1;

        for (int i = 0; i < octaves; i++) {
            max += a;
            a *= gain;
        }

        return max;
    }
}
