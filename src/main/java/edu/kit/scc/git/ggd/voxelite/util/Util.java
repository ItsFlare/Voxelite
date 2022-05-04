package edu.kit.scc.git.ggd.voxelite.util;

import edu.kit.scc.git.ggd.voxelite.Main;

import java.io.IOException;
import java.io.InputStream;

public class Util {

    public static String readShaderResource(String name) throws IOException {
        return readStringResource("shaders/" + name);
    }

    public static String readStringResource(String resource) throws IOException {
        return new String(readResource(resource).readAllBytes());
    }

    public static InputStream readResource(String resource) {
        return Main.class.getClassLoader().getResourceAsStream(resource);
    }

    public static int[] incrementalArray(int size) {
        int[] result = new int[size];
        for (int i = 0; i < size; i++) {
            result[i] = i;
        }

        return result;
    }
}
