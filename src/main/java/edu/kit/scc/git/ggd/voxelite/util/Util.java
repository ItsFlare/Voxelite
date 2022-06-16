package edu.kit.scc.git.ggd.voxelite.util;

import edu.kit.scc.git.ggd.voxelite.Main;
import net.durchholz.beacon.math.*;

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

    public static Matrix3f quatToMatrix(Quaternion quaternion) {
        float q0 = quaternion.r();
        float q1 = quaternion.i();
        float q2 = quaternion.j();
        float q3 = quaternion.k();


        float  r00 = 2 * (q0 * q0 + q1 * q1) - 1;
        float  r01 = 2 * (q1 * q2 - q0 * q3);
        float r02 = 2 * (q1 * q3 + q0 * q2);

        float r10 = 2 * (q1 * q2 + q0 * q3);
        float r11 = 2 * (q0 * q0 + q2 * q2) - 1;
        float r12 = 2 * (q2 * q3 - q0 * q1);

        float r20 = 2 * (q1 * q3 - q0 * q2);
        float  r21 = 2 * (q2 * q3 + q0 * q1);
        float r22 = 2 * (q0 * q0 + q3 * q3) - 1;

        Matrix3f rot_matrix = new Matrix3f(
                r00, r01, r02,
                r10, r11, r12,
                r20, r21, r22
        );
        return rot_matrix;
    }

    //Sets the tangent and bitangent for the given tangent and bitangent parameter
    public static Vec3f getTangent(Vec3f edge1, Vec3f edge2, Vec2i deltaUV1, Vec2i deltaUV2) {
        float f = 1.0f / (deltaUV1.x() * deltaUV2.y() - deltaUV2.x() * deltaUV1.y());

        float tangentX = f * (deltaUV2.y() * edge1.x() - deltaUV1.y() * edge2.x());
        float tangentY = f * (deltaUV2.y() * edge1.y() - deltaUV1.y() * edge2.y());
        float tangentZ = f * (deltaUV2.y() * edge1.z() - deltaUV1.y() * edge2.z());
        return new Vec3f(tangentX, tangentY, tangentZ);
    }

    public static Vec3f getBitangent(Vec3f edge1, Vec3f edge2, Vec2i deltaUV1, Vec2i deltaUV2) {
        float f = 1.0f / (deltaUV1.x() * deltaUV2.y() - deltaUV2.x() * deltaUV1.y());

        float bitangentX = f * (-deltaUV2.x() * edge1.x() + deltaUV1.x() * edge2.x());
        float bitangentY = f * (-deltaUV2.x() * edge1.y() + deltaUV1.x() * edge2.y());
        float bitangentZ = f * (-deltaUV2.x() * edge1.z() + deltaUV1.x() * edge2.z());

        return new Vec3f(bitangentX, bitangentY, bitangentZ);
    }
}
