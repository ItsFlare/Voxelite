package edu.kit.scc.git.ggd.voxelite.util;

import edu.kit.scc.git.ggd.voxelite.Main;
import net.durchholz.beacon.math.Vec3i;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Util {

    public static String readShaderResource(String name) {
        return readStringResource("shaders/" + name);
    }

    public static String readStringResource(String resource) {
        try {
            return new String(readResource(resource).readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static InputStream readResource(String resource) {
        return Main.class.getClassLoader().getResourceAsStream(resource);
    }


    public static Collection<Path> listResourceFolder(String folder) throws URISyntaxException, IOException {
        return listResourceFolder(folder, 1);
    }

    public static Collection<Path> listResourceFolder(String folder, int depth) throws URISyntaxException, IOException {
        final URL url = Util.class.getResource(folder);
        if (url == null) throw new FileNotFoundException(folder + " not found");

        final URI uri = url.toURI();
        Path path;

        try {
            path = Paths.get(uri);
        } catch (FileSystemNotFoundException e) {
            var fs = FileSystems.newFileSystem(uri, Collections.emptyMap());
            path = fs.getPath(folder);
        }

        try (Stream<Path> walk = Files.walk(path, depth)) {
            return walk.collect(Collectors.toList());
        }
    }

    public static int log2(int n) {
        return (int) Math.ceil(Math.log(n) / Math.log(2));
    }

    public static float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }

    public static String toBinaryString(int i) {
        return String.format("%32s", Integer.toBinaryString(i)).replace(' ', '0');
    }

    public static double frac(float f) {
        return f - Math.floor(f);
    }

    public static <T> T init(T t, Consumer<T> consumer) {
        consumer.accept(t);
        return t;
    }

    public static <T> T init(IntFunction<T> constructor, IntSupplier supplier, Consumer<T> consumer) {
        final T t = constructor.apply(supplier.getAsInt());
        consumer.accept(t);
        return t;
    }

    public static Iterable<Vec3i> cuboid(Vec3i a, Vec3i b) {
        final Vec3i origin = Vec3i.min(a, b);
        final Vec3i target = Vec3i.max(a, b);
        final Vec3i diff = target.subtract(origin).add(1);
        final int maxIndex = diff.x() * diff.y() * diff.z();

        return () -> new Iterator<>() {
            private int index = 0;
            private int x = origin.x(), y = origin.y(), z = origin.y();

            @Override
            public boolean hasNext() {
                return index < maxIndex;
            }

            @Override
            public Vec3i next() {
                Vec3i next = new Vec3i(x, y, z);

                if (y++ == target.y()) {
                    y = origin.y();

                    if (z++ == target.z()) {
                        z = origin.z();

                        if (x++ == target.x()) {
                            x = origin.x();
                        }
                    }
                }

                index++;

                return next;
            }
        };
    }
}
