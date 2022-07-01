package edu.kit.scc.git.ggd.voxelite.world.generator.noise;

import net.durchholz.beacon.math.Mth;

import java.util.Arrays;

public class LinearSpline {
    private final float[] x, y;

    public LinearSpline(Point... points) {
        this(new float[points.length], new float[points.length]);
        for (int i = 0; i < points.length; i++) {
            x[i] = points[i].x;
            y[i] = points[i].y;
        }
    }

    public LinearSpline(float[] x, float[] y) {
        this.x = x;
        this.y = y;
    }

    public float sample(float x) {
        int bs = Arrays.binarySearch(this.x, x);

        if(bs < 0) {
            bs = -bs - 1;
            if (bs < 1 || bs == y.length) throw new IllegalArgumentException();

            float a = this.x[bs - 1];
            float b = this.x[bs];
            final float alpha = (x - a) / (b - a);

            return Mth.lerp(this.y[bs - 1], this.y[bs], alpha);
        } else {
            return this.y[bs];
        }
    }

    public float min() {
        return y[0];
    }

    public float max() {
        return y[y.length - 1];
    }

    public float[] getX() {
        return x;
    }

    public float[] getY() {
        return y;
    }

    public record Point(float x, float y) {}
}
