package edu.kit.scc.git.ggd.voxelite.world.generator.noise;

import net.durchholz.beacon.math.Mth;
import net.durchholz.beacon.util.ToFloatFunction;

import java.util.Arrays;

public class IndirectSpline<T> {
    private final ToFloatFunction<T> function;
    private final float[]            x;
    private final Point<T>[]         y;

    @SafeVarargs
    public IndirectSpline(ToFloatFunction<T> function, Point<T>... points) {
        this(function, new float[points.length], new Point[points.length]);
        for (int i = 0; i < points.length; i++) {
            x[i] = points[i].x;
            y[i] = points[i];
        }
    }

    public IndirectSpline(ToFloatFunction<T> function, float[] x, Point<T>[] y) {
        this.function = function;
        this.x = x;
        this.y = y;
    }

    public float sample(T t) {
        float x = function.applyAsFloat(t);
        int bs = Arrays.binarySearch(this.x, x);

        if (bs < 0) {
            bs = -bs - 1;
            if (bs < 1 || bs == y.length) throw new IllegalArgumentException();

            float a = this.x[bs - 1];
            float b = this.x[bs];
            final float alpha = (x - a) / (b - a);

            return Mth.lerp(this.y[bs - 1].sample(t), this.y[bs].sample(t), alpha);
        } else {
            return this.y[bs].sample(t);
        }
    }

    public record Point<T>(float x, float y, ToFloatFunction<T> function) {
        public Point(float x, float y) {
            this(x, y, t -> 0);
        }

        public float sample(T t) {
            return y + function.applyAsFloat(t);
        }
    }
}
