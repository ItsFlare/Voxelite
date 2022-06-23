package edu.kit.scc.git.ggd.voxelite.render;

import edu.kit.scc.git.ggd.voxelite.util.Direction;
import edu.kit.scc.git.ggd.voxelite.world.LightStorage;
import net.durchholz.beacon.math.Vec2i;
import net.durchholz.beacon.math.Vec3i;
import net.durchholz.beacon.render.opengl.OpenGL;
import net.durchholz.beacon.render.opengl.buffers.BufferLayout;
import net.durchholz.beacon.render.opengl.buffers.VertexArray;
import net.durchholz.beacon.render.opengl.buffers.VertexBuffer;

import java.util.ArrayList;
import java.util.List;

public abstract class Slice {
    protected final     RenderType                                     renderType;
    protected final     VertexArray                                    vertexArray       = new VertexArray();
    protected final     VertexArray                                    shadowVertexArray = new VertexArray();
    protected final     VertexBuffer<ChunkProgram.InstanceVertex>      instanceBuffer    = new VertexBuffer<>(ChunkProgram.InstanceVertex.LAYOUT, BufferLayout.INTERLEAVED, OpenGL.Usage.DYNAMIC_DRAW);

    protected final VertexBuffer<ChunkProgram.AOVertex> aoBuffer = new VertexBuffer<>(ChunkProgram.AOVertex.LAYOUT, BufferLayout.INTERLEAVED, OpenGL.Usage.DYNAMIC_DRAW);
    protected final     VertexBuffer<ChunkProgram.InstanceLightVertex> lightBuffer       = new VertexBuffer<>(ChunkProgram.InstanceLightVertex.LAYOUT, BufferLayout.INTERLEAVED, OpenGL.Usage.DYNAMIC_DRAW);

    protected List<QueuedQuad>                   queue = new ArrayList<>();
    protected ChunkProgram.InstanceVertex[]      nextVertices;

    protected ChunkProgram.AOVertex[] nextAOVertices;
    protected ChunkProgram.InstanceLightVertex[] nextLightVertices;
    protected int quadCount;

    public Slice(RenderType renderType) {
        this.renderType = renderType;
    }

    public abstract void build();

    public abstract void render(VertexArray va, int visibility);

    public void render(int visibility) {
        render(vertexArray, visibility);
    }

    public void renderShadow(int visibility) {
        render(shadowVertexArray, visibility);
    }

    public int getQuadCount() {
        return quadCount;
    }

    public synchronized void upload() {
        if (nextVertices == null) return;
        assert nextVertices.length == nextLightVertices.length;
        assert nextVertices.length == (4 * (nextAOVertices.length - 1) + (nextAOVertices.length % 4));

        instanceBuffer.use(() -> {
            instanceBuffer.data(nextVertices);
        });

        aoBuffer.use(() -> {
            aoBuffer.data(nextAOVertices);
        });

        lightBuffer.use(() -> {
            lightBuffer.data(nextLightVertices);
        });

        this.quadCount = nextVertices.length;
        this.nextVertices = null;
        this.nextAOVertices = null;
        this.nextLightVertices = null;
    }

    public void delete() {
        vertexArray.delete();
        instanceBuffer.delete();
        aoBuffer.delete();
        lightBuffer.delete();
    }

    public void queue(QueuedQuad quad) {
        queue.add(quad);
    }

    protected static ChunkProgram.InstanceLightVertex[] toLightVertices(List<QueuedQuad> queuedQuads) {
        return queuedQuads.stream()
                .map(queuedQuad -> new ChunkProgram.InstanceLightVertex(packLight(queuedQuad.light())))
                .toArray(ChunkProgram.InstanceLightVertex[]::new);
    }

    protected static ChunkProgram.InstanceVertex[] toInstanceVertices(List<QueuedQuad> queuedQuads) {
        return queuedQuads.stream()
                .map(queuedQuad -> new ChunkProgram.InstanceVertex(packInstance(queuedQuad)))
                .toArray(ChunkProgram.InstanceVertex[]::new);
    }

    protected static ChunkProgram.AOVertex[] toAOVertices(List<QueuedQuad> queuedQuads) {
        int length = (int) Math.ceil(queuedQuads.size() / 4f);
        int rest = queuedQuads.size() % 4;
        //System.out.println("size:" + queuedQuads.size() + " rest:" + rest + " length:" + length);
        ChunkProgram.AOVertex[] aoVertices = new ChunkProgram.AOVertex[length];
        for (int i = 0; i < length; i++) {
            QueuedQuad q0;
            QueuedQuad q1;
            QueuedQuad q2;
            QueuedQuad q3;
            if (i < length - 1) {
                q0 = queuedQuads.get(i * 4);
                q1 = queuedQuads.get(i * 4 + 1);
                q2 = queuedQuads.get(i * 4 + 2);
                q3 = queuedQuads.get(i * 4 + 3);

            } else {
                if (rest == 0) {
                    q0 = queuedQuads.get(i * 4);
                    q1 = queuedQuads.get(i * 4 + 1);
                    q2 = queuedQuads.get(i * 4 + 2);
                    q3 = queuedQuads.get(i * 4 + 3);
                } else {
                    //System.out.println("length:" + length + " index:" + i + " rest:" + rest);
                    q0 = queuedQuads.get(i * 4);
                    q1 = rest >= 2 ? queuedQuads.get(i * 4 + 1) : new QueuedQuad(null, null, null, null, 0);
                    q2 = rest >= 3 ? queuedQuads.get(i * 4 + 2) : new QueuedQuad(null, null, null, null, 0);
                    q3 = new QueuedQuad(null, null, null, null, 0);
                    //System.out.println("q0" + q0 + " q1:" + q1 + " q2:" + q2 + " q3:" + q3);
                }
            }
            //System.out.println(packAO(q0, q1, q2, q3));
            aoVertices[i] = new ChunkProgram.AOVertex(packAO(q0, q1, q2, q3));
            //System.out.println(aoVertices[i]);
        }
        /*for (int i = 0; i < length; i++) {
            System.out.println(aoVertices[i] + " index:" + i);
            System.out.println(aoVertices[i + 1] + " index:" + (i + 1));
            System.out.println(aoVertices[i + 2] + " index:" + (i + 2));
            System.out.println(aoVertices[i + 3] + " index:" + (i + 3));
        }*/
        return aoVertices;
    }

    public static int packInstance(QueuedQuad queuedQuad) {
        return Slice.packInstance(queuedQuad.position(), queuedQuad.texture(), queuedQuad.direction().ordinal());
    }

    public static int packInstance(Vec3i offset, Vec2i texture, int direction) {
        assert offset.max() < 32;
        assert texture.x() < 128 && texture.y() < 128;
        assert direction < 6;

        int result = 0;

        result |= offset.x() << 27;
        result |= offset.y() << 22;
        result |= offset.z() << 17;

        result |= texture.x() << 10;
        result |= texture.y() << 3;

        result |= direction;

        return result;
    }

    public static Vec3i unpackPosition(int instance) {
        int x = instance >> 27;
        int y = (instance >> 22) & 0x1f;
        int z = (instance >> 17) & 0x1f;
        return new Vec3i(x, y, z);
    }

    public static int packLight(Vec3i light) {
        assert LightStorage.MAX_TOTAL_VALUE < (1 << 10);
        assert light.x() < 1024 && light.y() < 1024 && light.z() < 1024;

        int result = 0;

        result |= light.x() << 20;
        result |= light.y() << 10;
        result |= light.z();

        return result;
    }

    public static int packAO(QueuedQuad q0, QueuedQuad q1, QueuedQuad q2, QueuedQuad q3) {
        assert q0.ao < 256;
        assert q1.ao < 256;
        assert q2.ao < 256;
        assert q3.ao < 256;


        int result = 0;

        result |= q0.ao;
        result |= q1.ao << 8;
        result |= q2.ao << 16;
        result |= q3.ao << 24;

        /*int byteIndex = 2;
        int byteShift = (byteIndex << 3); // equivalent to byteIndex * 8
        int byteMask = 255 << byteShift;
        int aoByte = (result & byteMask) >> byteShift;

        int bitIndex = 2;
        int bitShift = (bitIndex << 1); // equivalent to bitIndex * 2
        int bitMask = 3 << bitShift;
        int aoBit = (aoByte & bitMask) >> bitShift;*/


        /*System.out.println("a0:" + q0.ao + " a1:" + q1.ao + " a2:" + q2.ao + " a3:" + q3.ao + " result:" + result);
        System.out.println((result >>> 24) + " a3:" + q3.ao);
        System.out.println(((result << 8) >>> 24)+ "a2:" + q2.ao);
        System.out.println(((result << 16) >>> 24)+ "a1:" + q1.ao);
        System.out.println(((result << 24) >>> 24)+ "a0:" + q0.ao);*/

        return result;
    }


    record QueuedQuad(Direction direction, Vec3i position, Vec2i texture, Vec3i light, int ao) {}

    record Command(int commands, int offset) {}
}
