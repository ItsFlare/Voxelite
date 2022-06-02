package edu.kit.scc.git.ggd.voxelite.test;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.openjdk.jmh.results.BenchmarkResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.CommandLineOptionException;
import org.openjdk.jmh.runner.options.CommandLineOptions;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class BenchmarkManager {
    private static final Path PATH = Path.of("test_baselines.json");
    private static final Gson GSON = new Gson();
    private static final Type TYPE = new TypeToken<Map<String, Baseline>>() { }.getType();

    private static final Map<String, Baseline> map = deserialize();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                serialize();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }));
    }

    public BenchmarkManager() {}

    public static Baseline getBaseline(String name) {
        return map.get(name);
    }

    public static void putBaseline(String name, Baseline baseline) {
        map.put(name, baseline);
    }

    @SuppressWarnings("ClassCanBeRecord")
    public static final class Baseline {
        private final double value;
        private final String timeUnit;

        public Baseline(double value, String timeUnit) {
            this.value = value;
            this.timeUnit = timeUnit;
        }

        public double value() {return value;}

        public String timeUnit() {return timeUnit;}

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (Baseline) obj;
            return Double.doubleToLongBits(this.value) == Double.doubleToLongBits(that.value) &&
                    Objects.equals(this.timeUnit, that.timeUnit);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value, timeUnit);
        }

        @Override
        public String toString() {
            return "Baseline[" +
                    "value=" + value + ", " +
                    "timeUnit=" + timeUnit + ']';
        }
    }

    private static Map<String, Baseline> deserialize() {
        if(!PATH.toFile().isFile()) return new HashMap<>();

        String json;
        try {
            json = Files.readString(PATH);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(json.isBlank()) return new HashMap<>();
        final Map<String, Baseline> deserialized = GSON.fromJson(json, TYPE);
        if(deserialized == null) return new HashMap<>();
        return deserialized;
    }

    private static void serialize() throws IOException {
        Files.writeString(PATH, GSON.toJson(map), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public static void run(Class<?> clazz, float threshold) throws CommandLineOptionException, RunnerException {
        run(clazz.getName().replace('$', '.'), threshold);
    }

    public static void run(String args, float threshold) throws RunnerException, CommandLineOptionException {
        var runner = new Runner(new CommandLineOptions(args));
        var runResult = runner.runSingle();

        for (BenchmarkResult result : runResult.getBenchmarkResults()) {
            final var primaryResult = result.getPrimaryResult();
            final String name = result.getParams().getBenchmark();
            final var baseline = BenchmarkManager.getBaseline(name);
            final var current = new BenchmarkManager.Baseline(primaryResult.getScore(), primaryResult.getScoreUnit());

            boolean write = false;
            if(baseline != null) {
                if (baseline.timeUnit().equals(primaryResult.getScoreUnit())) {
                    double error = Double.isNaN(primaryResult.getScoreError()) ? 0 : primaryResult.getScoreError();

                    final double ratio = (primaryResult.getScore() - error) / baseline.value();
                    if(ratio > threshold) {
                        throw new AssertionError("Performance degraded by %.2f%% (baseline %.2f %s)".formatted((ratio - 1) * 100, baseline.value(), baseline.timeUnit()));
                    } else if (ratio < 1) {
                        write = true;
                    }
                } else {
                    System.out.println(name + ": Time unit mismatch");
                    write = true;
                }
            } else {
                write = true;
            }

            if(write) BenchmarkManager.putBaseline(name, current);
        }

    }
}
