package edu.kit.scc.git.ggd.voxelite.render;

import edu.kit.scc.git.ggd.voxelite.Main;
import edu.kit.scc.git.ggd.voxelite.util.Memory;
import net.durchholz.beacon.data.StructArray;
import net.durchholz.beacon.math.AABB;
import net.durchholz.beacon.math.Matrix4f;
import net.durchholz.beacon.math.Vec1i;
import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.render.opengl.buffers.BufferLayout;
import net.durchholz.beacon.render.opengl.buffers.SSBO;
import net.durchholz.beacon.render.opengl.buffers.VertexArray;
import net.durchholz.beacon.render.opengl.buffers.VertexBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.IntBuffer;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import static edu.kit.scc.git.ggd.voxelite.util.Util.debug;
import static net.durchholz.beacon.render.opengl.OpenGL.*;
import static org.lwjgl.opengl.GL44.*;

public class OcclusionRenderer {

    private static final OcclusionProgram PROGRAM = new OcclusionProgram();
    private static final Logger           LOGGER  = LoggerFactory.getLogger(OcclusionRenderer.class);

    private static final OcclusionProgram.CubeVertex[] CUBE_VERTICES = new OcclusionProgram.CubeVertex[]{
            new OcclusionProgram.CubeVertex(new Vec3f(1, 1, 1)),
            new OcclusionProgram.CubeVertex(new Vec3f(0, 1, 1)),
            new OcclusionProgram.CubeVertex(new Vec3f(1, 0, 1)),
            new OcclusionProgram.CubeVertex(new Vec3f(0, 0, 1)),
            new OcclusionProgram.CubeVertex(new Vec3f(0, 0, 0)),
            new OcclusionProgram.CubeVertex(new Vec3f(0, 1, 1)),
            new OcclusionProgram.CubeVertex(new Vec3f(0, 1, 0)),
            new OcclusionProgram.CubeVertex(new Vec3f(1, 1, 1)),
            new OcclusionProgram.CubeVertex(new Vec3f(1, 1, 0)),
            new OcclusionProgram.CubeVertex(new Vec3f(1, 0, 1)),
            new OcclusionProgram.CubeVertex(new Vec3f(1, 0, 0)),
            new OcclusionProgram.CubeVertex(new Vec3f(0, 0, 0)),
            new OcclusionProgram.CubeVertex(new Vec3f(1, 1, 0)),
            new OcclusionProgram.CubeVertex(new Vec3f(0, 1, 0))
    };

    private static final VertexBuffer<OcclusionProgram.CubeVertex> CUBE_VB = new VertexBuffer<>(OcclusionProgram.CubeVertex.LAYOUT, BufferLayout.INTERLEAVED, Usage.STATIC_DRAW);

    static {
        CUBE_VB.use(() -> CUBE_VB.data(CUBE_VERTICES));
    }

    /**
     * Grace time controls how long something has to be occluded in order to be culled, and therefore also the time we offer for memory to sync.
     */
    public static final long GRACE_TIME_NS = TimeUnit.MILLISECONDS.toNanos(50);

    /**
     * Constant inflation of bounding boxes to compensate movement vs. latency of last frame's depth.
     * TODO Make this adapt based on frame time and actual movement.
     */
    public static final float INFLATE = 0.5f;

    public static final int MAX_OCCLUDEES = 100_000;

    private final SSBO<StructArray<Vec1i>>                      ssbo      = new SSBO<>();
    private final IntBuffer                                     mappedBuffer;
    private final VertexArray                                   va        = new VertexArray();
    private final VertexBuffer<OcclusionProgram.InstanceVertex> instances = new VertexBuffer<>(OcclusionProgram.InstanceVertex.LAYOUT, BufferLayout.INTERLEAVED, Usage.STREAM_DRAW);
    private final Memory<Query>                                 queries   = new Memory<>();

    public OcclusionRenderer() {
        this.mappedBuffer = ssbo.use(() -> {
            final int bytes = MAX_OCCLUDEES << 2;
            final int flags = GL_MAP_READ_BIT | GL_MAP_PERSISTENT_BIT;

            glBufferStorage(ssbo.type().target(), bytes, flags);
            return glMapBufferRange(ssbo.type().target(), 0, bytes, flags).asIntBuffer();
        });

        va.use(() -> {
            use(CUBE_VB, () -> {
                va.set(PROGRAM.pos, OcclusionProgram.CubeVertex.POSITION, CUBE_VB, 0);
            });

            instances.use(() -> {
                va.set(PROGRAM.min, OcclusionProgram.InstanceVertex.MIN, instances, 1);
                va.set(PROGRAM.max, OcclusionProgram.InstanceVertex.MAX, instances, 1);
                va.set(PROGRAM.id, OcclusionProgram.InstanceVertex.ID, instances, 1);
            });
        });
    }

    public Memory<Query> getQueries() {
        return queries;
    }

    public void render(Matrix4f mvp) {
        final int frame = Main.INSTANCE.getRenderer().getFrame();

        var vertices = queries
                .stream()
                .filter(entry -> entry.value().active.getAsBoolean())
                .map(entry -> {
                    final AABB aabb = entry.value().aabb.get().inflate(INFLATE);
                    return new OcclusionProgram.InstanceVertex(entry.address(), aabb.min(), aabb.max().subtract(aabb.min()));
                }).toArray(OcclusionProgram.InstanceVertex[]::new);

        use(STATE, PROGRAM, va, ssbo, instances, () -> {
            depthTest(true);
            depthFunction(CompareFunction.LESS_EQUAL);
            depthMask(false);
            colorMask(false);
            cull(false);

            instances.data(vertices);

            PROGRAM.occlusion.set(ssbo);
            PROGRAM.frame.set(frame);
            PROGRAM.mvp.set(mvp);

            LOGGER.trace("Dispatching %d occlusion queries in frame %d".formatted(vertices.length, frame));
            drawArraysInstanced(Mode.TRIANGLE_STRIP, 0, CUBE_VERTICES.length, vertices.length);
            glMemoryBarrier(GL_CLIENT_MAPPED_BUFFER_BARRIER_BIT);
        });
    }

    public void read() {
        final int frame = Main.INSTANCE.getRenderer().getFrame();
        debug(() -> LOGGER.trace("Reading %d occlusion results in frame %d".formatted(queries.size(), frame)));

        //These mapped buffer reads are unsynchronized and may return stale or incoherent values.

        final int grace = (int) (GRACE_TIME_NS / Main.INSTANCE.getFrameTime());
        final int occlusionFrame = mappedBuffer.get(0);
        final int latency = frame - occlusionFrame;

        if (latency > grace) {
            LOGGER.warn("Occlusion latency exceeded (%d>%d frames)".formatted(latency, grace));
            return;
        }

        ssbo.use(() -> {
            for (Memory.Entry<Query> entry : queries) {
                int lastVisibleFrame = mappedBuffer.get(entry.address() + 1);
                if (lastVisibleFrame + grace < frame) entry.value().occluded.run();
            }
        });
    }

    public record Query(BooleanSupplier active, Supplier<AABB> aabb, Runnable occluded) {}
}
