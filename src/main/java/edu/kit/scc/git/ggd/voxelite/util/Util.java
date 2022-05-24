package edu.kit.scc.git.ggd.voxelite.util;

import edu.kit.scc.git.ggd.voxelite.Main;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.Collection;
import java.util.Collections;
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
        if(url == null) throw new FileNotFoundException(folder + " not found");

        final URI uri = url.toURI();
        Path path;

        try {
            path = Paths.get(uri);
        } catch (FileSystemNotFoundException e) {
            var fs = FileSystems.newFileSystem(uri, Collections.emptyMap());
            path = fs.getPath(folder);
        }

        try(Stream<Path> walk = Files.walk(path, depth)) {
            return walk.collect(Collectors.toList());
        }
    }

    public static int log2(int n) {
        return (int) Math.ceil(Math.log(n) / Math.log(2));
    }

    public static String toBinaryString(int i) {
        return String.format("%32s", Integer.toBinaryString(i)).replace(' ', '0');
    }

    public static double lerp(double d0, double d1, double d2) {
        return d1 + d0 * (d2 - d1);
    }

    public static double frac(double d0) {
        return d0 - (double) lfloor(d0);
    }

    public static long lfloor(double d0) {
        long i = (long) d0;

        return d0 < (double) i ? i - 1L : i;
    }

    public static int floor(double d0) {
        int i = (int) d0;

        return d0 < (double) i ? i - 1 : i;
    }
}
