package edu.kit.scc.git.ggd.voxelite.util;

import edu.kit.scc.git.ggd.voxelite.Main;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Util {

    public static String readStringResource(String resource) throws IOException {
        return new String(readResource(resource).readAllBytes());
    }

    public static InputStream readResource(String resource) {
        return Main.class.getClassLoader().getResourceAsStream(resource);
    }


    public static Collection<Path> listResourceFolder(String folder) throws URISyntaxException, IOException {
        return listResourceFolder(folder, 0);
    }

    public static Collection<Path> listResourceFolder(String folder, int depth) throws URISyntaxException, IOException {
        URL url = Util.class.getResource(folder);
        if(url == null) throw new FileNotFoundException(folder + " not found");
        Path path = Paths.get(url.toURI());

        try(Stream<Path> walk = Files.walk(path, depth)) {
            return walk.collect(Collectors.toList());
        }
    }
}
