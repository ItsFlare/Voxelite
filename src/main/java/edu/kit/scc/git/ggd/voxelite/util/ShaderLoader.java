package edu.kit.scc.git.ggd.voxelite.util;

import net.durchholz.beacon.render.opengl.shader.Shader;
import net.durchholz.beacon.render.opengl.shader.ShaderGraph;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ShaderLoader {
    private static final Map<String, Shader> RESOLVED = loadShaders();

    public static Shader get(String name) {
        return RESOLVED.get(name);
    }

    public static Shader[] getSuite(String name) {
        var shaders = new ArrayList<Shader>(Shader.Type.values().length);

        var vs = RESOLVED.get(name + ".vs");
        if(vs != null) shaders.add(vs);
        var gs = RESOLVED.get(name + ".gs");
        if(gs != null) shaders.add(gs);
        var fs = RESOLVED.get(name + ".fs");
        if(fs != null) shaders.add(fs);
        var cs = RESOLVED.get(name + ".cs");
        if(cs != null) shaders.add(cs);

        return shaders.toArray(new Shader[0]);
    }

    private static Map<String, Shader> loadShaders() {
        var graph = new ShaderGraph();

        for (ShaderGraph.Node node : loadNodes()) {
            graph.add(node);
        }

        return graph.resolve();
    }

    private static ShaderGraph.Node[] loadNodes() {
        final List<ShaderGraph.Node> shaders = new ArrayList<>();
        try {
            Path root = Util.getResourcePath("/");
            final Path shaderPath = root.resolve("shaders");
            for (Path path : Util.listResourceFolder(shaderPath, Integer.MAX_VALUE)) {
                if (Files.isDirectory(path)) continue; //Exclude directories
                final String fileName = path.getFileName().toString();
                final String[] split = fileName.split("\\.");

                if (split.length != 2) {
                    LoggerFactory.getLogger(Util.class).warn("Malformed shader file name: %s".formatted(fileName));
                    continue;
                }

                String extension = split[1];
                Shader.Type type = switch (extension) {
                    case "vs" -> Shader.Type.VERTEX;
                    case "gs" -> Shader.Type.GEOMETRY;
                    case "fs" -> Shader.Type.FRAGMENT;
                    case "cs" -> Shader.Type.COMPUTE;
                    case "glsl" -> null;
                    default -> throw new IllegalStateException("Unexpected value: " + extension);
                };

                final String name = shaderPath.relativize(path).toString();
                final String source = Util.readStringResource(root.relativize(path).toString());
                shaders.add(new ShaderGraph.Node(type, name, new StringBuffer(source)));

            }
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }

        return shaders.toArray(ShaderGraph.Node[]::new);
    }

}
