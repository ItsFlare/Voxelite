package edu.kit.scc.git.ggd.voxelite.render;

import edu.kit.scc.git.ggd.voxelite.Main;
import edu.kit.scc.git.ggd.voxelite.util.Util;

import static net.durchholz.beacon.render.opengl.OpenGL.*;

public enum RenderType {
    OPAQUE(new OpaqueChunkProgram(Util.loadShaders("chunk_opaque")), () -> {
        depthMask(true);
        depthTest(true);
        colorMask(true);
        blend(false);
        cull(Main.INSTANCE.getRenderer().getWorldRenderer().backfaceCull);
    }),
    TRANSPARENT(new TransparentChunkProgram(Util.loadShaders("chunk_transparent")), () -> {
        depthMask(false);
        depthTest(true);
        colorMask(true);
        blend(true);
        blendEquation(BlendEquation.ADD);
        blendFunction(BlendFunction.SOURCE_ALPHA, BlendFunction.ONE_MINUS_SOURCE_ALPHA);
        cull(Main.INSTANCE.getRenderer().getWorldRenderer().backfaceCull);
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
