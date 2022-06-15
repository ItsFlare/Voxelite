package edu.kit.scc.git.ggd.voxelite.render;

import edu.kit.scc.git.ggd.voxelite.util.Direction;
import net.durchholz.beacon.math.Matrix4f;
import net.durchholz.beacon.math.Vec2i;
import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.math.Vec3i;
import net.durchholz.beacon.render.opengl.OpenGL;
import net.durchholz.beacon.render.opengl.buffers.*;
import net.durchholz.beacon.render.opengl.shader.Program;
import net.durchholz.beacon.render.opengl.shader.Shader;
import net.durchholz.beacon.render.opengl.shader.Uniform;

public class ChunkProgram extends Program {

    public static final VertexBuffer<QuadVertex> QUAD_VB      = new VertexBuffer<>(QuadVertex.LAYOUT, BufferLayout.INTERLEAVED, OpenGL.Usage.DYNAMIC_DRAW);
    public static final IBO                      QUAD_IBO     = new IBO();
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

    public ChunkProgram(Shader... shaders) {
        super(shaders);
    }


    public final Attribute<Integer> data     = attribute("data", OpenGL.Type.INT, 1);
    public final Attribute<Integer> light    = attribute("light", OpenGL.Type.INT, 1);

    public final Uniform<Matrix4f> mvp                  = uniMatrix4f("mvp", true);
    public final Uniform<Vec3i>    chunk                = uniVec3i("chunk");
    public final Sampler           atlas                = sampler("atlas");
    public final Sampler           shadowMap            = sampler("shadowMap");
    public final Uniform<Vec3f>    camera               = uniVec3f("camera");
    public final Uniform<Vec3f>    lightDirection       = uniVec3f("light.direction");
    public final Uniform<Vec3f>    lightColor           = uniVec3f("light.color");
    public final Uniform<Float>    ambientStrength      = uniFloat("ambientStrength");
    public final Uniform<Float>    diffuseStrength      = uniFloat("diffuseStrength");
    public final Uniform<Float>    specularStrength     = uniFloat("specularStrength");
    public final Uniform<Integer>  phongExponent        = uniInteger("phongExponent");
    public final Uniform<Float>    normalizedSpriteSize = uniFloat("normalizedSpriteSize");
    public final Uniform<Float>    constantBias         = uniFloat("constantBias");
    public final Uniform<Integer>  maxLightValue        = uniInteger("maxLightValue");
    public final Uniform<Matrix4f> lightView            = uniMatrix4f("lightView", true);
    public final Uniform<Integer>  shadows              = uniInteger("shadows");
    public final Uniform<Vec3f[]>  cascadeScales        = uniVec3fArray("cascades", "scale", 4);
    public final Uniform<Vec3f[]>  cascadeTranslations  = uniVec3fArray("cascades", "translation", 4);
    public final Uniform<Float[]>  cascadeFar           = uniFloatArray("cascades", "far", 4);
    public final Uniform<Integer>  cascadeDebug         = uniInteger("cascadeDebug");
    public final Uniform<Integer>  kernel               = uniInteger("kernel");

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

    public record InstanceLightVertex(int light) implements Vertex {
        public static final VertexLayout<InstanceLightVertex> LAYOUT = new VertexLayout<>(InstanceLightVertex.class);
        public static final VertexAttribute<Integer>          LIGHT  = LAYOUT.primitive(false);

        @Override
        public VertexLayout<InstanceLightVertex> getLayout() {
            return LAYOUT;
        }
    }

}
