package edu.kit.scc.git.ggd.voxelite.render;

import edu.kit.scc.git.ggd.voxelite.Main;
import edu.kit.scc.git.ggd.voxelite.util.Util;
import net.durchholz.beacon.render.opengl.OpenGL;
import net.durchholz.beacon.render.opengl.shader.Shader;

public enum RenderType {
    OPAQUE(new OpaqueChunkProgram(Util.loadShaders("chunk_opaque")), () -> {
        OpenGL.depthMask(true);
        OpenGL.blend(false);
        OpenGL.cull(Main.INSTANCE.getRenderer().getWorldRenderer().backfaceCull);
    }),
    TRANSPARENT(new TransparentChunkProgram(
            Shader.vertex(Util.readShaderResource("chunk_transparent.vs")),
            Shader.geometry(Util.readShaderResource("chunk_transparent.gs")),
            Shader.fragment(Util.readShaderResource("chunk_opaque.fs"))
    ), () -> {
        OpenGL.depthMask(false);
        OpenGL.blend(true);
        OpenGL.blendEquation(OpenGL.BlendEquation.ADD);
        OpenGL.blendFunction(OpenGL.BlendFunction.SOURCE_ALPHA, OpenGL.BlendFunction.ONE_MINUS_SOURCE_ALPHA);
        OpenGL.cull(Main.INSTANCE.getRenderer().getWorldRenderer().backfaceCull);
    });

    private final ChunkProgram program;
    private final Runnable     state;

    RenderType(ChunkProgram program, Runnable state) {
        this.program = program;
        this.state = state;
    }

    public ChunkProgram getProgram() {
        return this.program;
    }

    public void setPipelineState() {
        state.run();
    }
}
