package edu.kit.scc.git.ggd.voxelite.render;

import edu.kit.scc.git.ggd.voxelite.Main;
import edu.kit.scc.git.ggd.voxelite.util.Direction;
import edu.kit.scc.git.ggd.voxelite.world.Chunk;
import net.durchholz.beacon.data.IntVector;
import net.durchholz.beacon.math.Matrix4f;
import net.durchholz.beacon.math.Vec2i;
import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.math.Vec3i;
import net.durchholz.beacon.render.opengl.OpenGL;
import net.durchholz.beacon.render.opengl.buffers.*;
import net.durchholz.beacon.render.opengl.shader.Program;
import net.durchholz.beacon.render.opengl.shader.Shader;
import net.durchholz.beacon.render.opengl.shader.Uniform;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.lwjgl.opengl.GL43.*;

public class ChunkProgram extends Program {

    public static final int MAX_UNSIGNED_SHORT    = (1 << (Short.BYTES * 8)) - 1;
    public static final int PRIMITIVE_RESET_INDEX = MAX_UNSIGNED_SHORT;

    private static final VertexBuffer<QuadVertex> QUAD_VB      = new VertexBuffer<>(QuadVertex.LAYOUT, BufferLayout.INTERLEAVED, OpenGL.Usage.DYNAMIC_DRAW);
    private static final IBO                      QUAD_IBO     = new IBO();
    private static final short[]                  QUAD_INDICES = {0, 3, 1, 2};

    static {
        QuadVertex[] quadVertices = new QuadVertex[Direction.values().length * 4];
        for (int i = 0; i < Direction.values().length; i++) {
            Direction direction = Direction.values()[i];
            Vec3f normal = new Vec3f(direction.getAxis());
            quadVertices[i * 4 + 0] = new QuadVertex(new Vec3f(direction.getUnitQuad().v0()), new Vec2i(0, 0), normal);
            quadVertices[i * 4 + 1] = new QuadVertex(new Vec3f(direction.getUnitQuad().v1()), new Vec2i(0, 1), normal);
            quadVertices[i * 4 + 2] = new QuadVertex(new Vec3f(direction.getUnitQuad().v2()), new Vec2i(1, 1), normal);
            quadVertices[i * 4 + 3] = new QuadVertex(new Vec3f(direction.getUnitQuad().v3()), new Vec2i(1, 0), normal);
        }

        QUAD_VB.use(() -> QUAD_VB.data(quadVertices));
        QUAD_IBO.use(() -> QUAD_IBO.data(OpenGL.Usage.STATIC_DRAW, QUAD_INDICES));
    }

    public static boolean directionCulling = true;

    public ChunkProgram(Shader... shaders) {
        super(shaders);
    }

    public final Attribute<Vec3f>   position = attribute("pos", OpenGL.Type.FLOAT, 3);
    public final Attribute<Vec2i>   texture  = attribute("tex", OpenGL.Type.INT, 2);
    public final Attribute<Vec3f>   normal   = attribute("normal", OpenGL.Type.FLOAT, 3);
    public final Attribute<Integer> data     = attribute("data", OpenGL.Type.INT, 1);

    public final Uniform<Matrix4f> mvp                  = uniMatrix4f("mvp", true);
    public final Uniform<Vec3i>    chunk                = uniVec3i("chunk");
    public final Sampler           atlas                = sampler("atlas");
    public final Uniform<Vec3f>    camera               = uniVec3f("camera");
    public final Uniform<Vec3f>    lightDirection       = uniVec3f("light.direction");
    public final Uniform<Vec3f>    lightColor           = uniVec3f("light.color");
    public final Uniform<Float>    ambientStrength      = uniFloat("ambientStrength");
    public final Uniform<Float>    diffuseStrength      = uniFloat("diffuseStrength");
    public final Uniform<Float>    specularStrength     = uniFloat("specularStrength");
    public final Uniform<Integer>  phongExponent        = uniInteger("phongExponent");
    public final Uniform<Float>    normalizedSpriteSize = uniFloat("normalizedSpriteSize");

    public record QuadVertex(Vec3f position, Vec2i texture, Vec3f normal) implements Vertex {
        public static final VertexLayout<QuadVertex> LAYOUT   = new VertexLayout<>(QuadVertex.class);
        public static final VertexAttribute<Vec3f>   POSITION = LAYOUT.vec3f(false);
        public static final VertexAttribute<Vec2i>   TEXTURE  = LAYOUT.vec2i(false);
        public static final VertexAttribute<Vec3f>   NORMAL   = LAYOUT.vec3f(false);

        @Override
        public VertexLayout<QuadVertex> getLayout() {
            return LAYOUT;
        }
    }

    public record InstanceVertex(int data) implements Vertex {
        public static final VertexLayout<InstanceVertex> LAYOUT = new VertexLayout<>(InstanceVertex.class);
        public static final VertexAttribute<Integer>     DATA   = LAYOUT.primitive(false);

        @Override
        public VertexLayout<InstanceVertex> getLayout() {
            return LAYOUT;
        }
    }

    //TODO Subclass per RenderType?
    public class Slice {


        record QueuedQuad(Direction direction, Vec3i position, Vec2i texture) {}

        record Command(VBO buffer, int commands) {
            public static final int STRIDE = 20; //5 ints (see OpenGL.DrawMultiElementsIndirectCommand)
        }

        protected final Vec3i position, worldPosition;
        protected final RenderType renderType;

        protected final VertexArray                  vertexArray    = new VertexArray();
        protected final VertexBuffer<InstanceVertex> instanceBuffer = new VertexBuffer<>(InstanceVertex.LAYOUT, BufferLayout.INTERLEAVED, OpenGL.Usage.DYNAMIC_DRAW);
        protected final List<QueuedQuad>             queue          = new ArrayList<>();

        protected final Command[]        commands = new Command[1 << Direction.values().length];
        protected OpenGL.DrawMultiElementsIndirectCommand[][] nextCommands;
        protected InstanceVertex[] nextVertices;
        protected volatile int              quadCount;

        public Slice(Vec3i position, RenderType renderType) {
            this.position = position;
            this.worldPosition = Chunk.toWorldPosition(position);
            this.renderType = renderType;

            OpenGL.use(vertexArray, QUAD_IBO, () -> {
                QUAD_VB.use(() -> {
                    vertexArray.set(ChunkProgram.this.position, QuadVertex.POSITION, QUAD_VB, 0);
                    vertexArray.set(ChunkProgram.this.texture, QuadVertex.TEXTURE, QUAD_VB, 0);
                    vertexArray.set(ChunkProgram.this.normal, QuadVertex.NORMAL, QUAD_VB, 0);
                });

                instanceBuffer.use(() -> {
                    vertexArray.set(ChunkProgram.this.data, InstanceVertex.DATA, instanceBuffer, 1);
                });
            });
        }

        public synchronized void build() {
            if (queue.isEmpty()) return;

            this.nextVertices = queue
                    .stream()
                    .sorted(Comparator.comparingInt(value -> value.direction.ordinal()))
                    .map(queuedQuad -> new InstanceVertex(packInstance(queuedQuad.position(), queuedQuad.texture())))
                    .toArray(InstanceVertex[]::new);

            this.nextCommands = generateCommands();

            queue.clear();
        }


        public synchronized void upload() {
            if(nextVertices == null) return;

            instanceBuffer.use(() -> {
                instanceBuffer.data(nextVertices);
            });

            for (int i = 0; i < nextCommands.length; i++) {
                OpenGL.DrawMultiElementsIndirectCommand[] directionCommands = nextCommands[i];
                VBO vbo = new VBO();
                vbo.use(() -> vbo.data(OpenGL.Usage.DYNAMIC_DRAW, IntVector.merge(directionCommands)));
                this.commands[i] = new Command(vbo, directionCommands.length);
            }

            this.quadCount = nextVertices.length;
            this.nextVertices = null;
            this.nextCommands = null;
        }

        private OpenGL.DrawMultiElementsIndirectCommand[][] generateCommands() {
            OpenGL.DrawMultiElementsIndirectCommand[][] cmds = new OpenGL.DrawMultiElementsIndirectCommand[commands.length][];

            //Calculate partition sizes
            int[] directionCounts = new int[Direction.values().length];
            for (Slice.QueuedQuad queuedQuad : queue) {
                directionCounts[queuedQuad.direction().ordinal()]++;
            }

            //For all bitset permutations, precompute command data arrays
            for (int i = 0; i < cmds.length; i++) {
                final List<OpenGL.DrawMultiElementsIndirectCommand> dataList = new ArrayList<>(Direction.values().length);

                int offset = 0;
                for (int dir = 0; dir < Direction.values().length; dir++) {
                    int directionQuadCount = directionCounts[dir];
                    if (directionQuadCount == 0) continue;

                    //IF visible
                    if ((i & (1 << dir)) != 0) {
                        dataList.add(new OpenGL.DrawMultiElementsIndirectCommand(4, directionQuadCount, 0, 4 * dir, offset));
                    }

                    offset += directionQuadCount;
                }

                final OpenGL.DrawMultiElementsIndirectCommand[] directionCommands = dataList.toArray(OpenGL.DrawMultiElementsIndirectCommand[]::new);
                cmds[i] = directionCommands;
            }

            return cmds;
        }

        public void render() {
            if (quadCount == 0) return;
            ChunkProgram.this.chunk.set(worldPosition);
            final Vec3f cameraPosition = Main.INSTANCE.getRenderer().getCamera().getPosition();
            final int visibilityBitset;

            if (directionCulling) {
                /*
                Direction culling (geometry partitioned by face direction)
                TODO Unroll loop and replace dot product with comparison?
                */

                final Vec3i chunkCenter = worldPosition.add(Chunk.CENTER);
                final Direction[] directions = Direction.values();

                //Calculate visibility bitset
                int bitset = 0;
                for (int i = 0; i < directions.length; i++) {

                    final var direction = directions[i];
                    final var planePos = chunkCenter.subtract(direction.getAxis().scale(Chunk.WIDTH >> 1));
                    final var planeToCam = cameraPosition.subtract(planePos);

                    if (planeToCam.dot(direction.getAxis()) > 0f) {
                        bitset |= (1 << i);
                    }
                }

                visibilityBitset = bitset;
            } else {
                //Render everything
                visibilityBitset = (1 << 6) - 1;
            }

            final var cmd = commands[visibilityBitset];
            glBindBuffer(GL_DRAW_INDIRECT_BUFFER, cmd.buffer.id());
            vertexArray.use(() -> glMultiDrawElementsIndirect(GL_TRIANGLE_STRIP, GL_UNSIGNED_SHORT, 0L, cmd.commands, Command.STRIDE));
            glBindBuffer(GL_DRAW_INDIRECT_BUFFER, 0);
        }

        public int getQuadCount() {
            return quadCount;
        }

        public static int packInstance(Vec3i offset, Vec2i texture) {
            assert offset.x() < Chunk.WIDTH && offset.y() < Chunk.HEIGHT && offset.z() < Chunk.WIDTH;
            assert texture.x() < 256 && texture.y() < 256;

            int result = 0;

            result |= offset.x() << 27;
            result |= offset.y() << 22;
            result |= offset.z() << 17;

            result |= texture.x() << 8;
            result |= texture.y();

            return result;
        }
    }
}
