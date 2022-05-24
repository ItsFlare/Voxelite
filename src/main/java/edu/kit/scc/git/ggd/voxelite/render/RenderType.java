package edu.kit.scc.git.ggd.voxelite.render;

import edu.kit.scc.git.ggd.voxelite.util.Util;
import net.durchholz.beacon.render.opengl.shader.Shader;

public enum RenderType {
    OPAQUE(loadProgram("chunk_opaque")),
    CUTOUT(null),
    TRANSPARENT(null);

    private final ChunkProgram program;

    RenderType(ChunkProgram program) {
        this.program = program;
    }

    public ChunkProgram getProgram() {
        return this.program;
    }

    private static ChunkProgram loadProgram(String name) {
        return new ChunkProgram(Shader.vertex(Util.readShaderResource(name + ".vs")), Shader.fragment(Util.readShaderResource(name + ".fs")));
    }
}
