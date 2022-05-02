package edu.kit.scc.git.ggd.voxelite.util;

import edu.kit.scc.git.ggd.voxelite.Main;

import java.io.IOException;
import java.io.InputStream;

public class Util {

    public static String readStringResource(String resource) throws IOException {
        return new String(readResource(resource).readAllBytes());
    }

    public static InputStream readResource(String resource) {
        return Main.class.getClassLoader().getResourceAsStream(resource);
    }
}
