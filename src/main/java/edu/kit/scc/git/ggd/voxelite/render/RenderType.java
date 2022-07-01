package edu.kit.scc.git.ggd.voxelite.render;

import edu.kit.scc.git.ggd.voxelite.Main;
import edu.kit.scc.git.ggd.voxelite.util.Util;
import net.durchholz.beacon.render.opengl.OpenGL;

public enum RenderType {
    OPAQUE(new OpaqueChunkProgram(Util.loadShaders("chunk_opaque")), () -> {
        OpenGL.depthMask(true);
        OpenGL.depthTest(true);
        OpenGL.colorMask(true);
        OpenGL.blend(false);
        OpenGL.cull(Main.INSTANCE.getRenderer().getWorldRenderer().backfaceCull);
    }),
    TRANSPARENT(new TransparentChunkProgram(Util.loadShaders("chunk_transparent")), () -> {
        OpenGL.depthMask(false);
        OpenGL.depthTest(true);
        OpenGL.colorMask(true);
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
