package edu.kit.scc.git.ggd.voxelite.render;

import edu.kit.scc.git.ggd.voxelite.util.Util;

public enum RenderType {
    OPAQUE("chunk_opaque");

    private final ChunkProgram program;

    RenderType(ChunkProgram program) {
        this.program = program;
    }

    RenderType(String shaderName) {
        this.program = new ChunkProgram(Util.loadShaders(shaderName));
    }

    public ChunkProgram getProgram() {
        return this.program;
    }
}
