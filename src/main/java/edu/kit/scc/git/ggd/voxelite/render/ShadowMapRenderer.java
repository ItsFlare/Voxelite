package edu.kit.scc.git.ggd.voxelite.render;

import edu.kit.scc.git.ggd.voxelite.Main;
import edu.kit.scc.git.ggd.voxelite.util.Frustum;
import edu.kit.scc.git.ggd.voxelite.util.Util;
import edu.kit.scc.git.ggd.voxelite.world.Chunk;
import net.durchholz.beacon.math.*;
import net.durchholz.beacon.render.opengl.OpenGL;
import net.durchholz.beacon.render.opengl.buffers.FBO;
import net.durchholz.beacon.render.opengl.shader.Shader;
import net.durchholz.beacon.render.opengl.textures.ArrayTexture2D;
import net.durchholz.beacon.render.opengl.textures.GLTexture;
import net.durchholz.beacon.window.Viewport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import static org.lwjgl.opengl.GL43.GL_NONE;

public class ShadowMapRenderer {

    public static final ChunkShadowProgram PROGRAM             = new ChunkShadowProgram(Shader.vertex(Util.readShaderResource("chunk_opaque_shadow.vs")), Shader.fragment(Util.readShaderResource("default.fs")));
    public static final int                RANGE_BEHIND_CAMERA = 250;

    private final FBO            fbo     = new FBO();
    private final ArrayTexture2D texture = new ArrayTexture2D();

    public int      cascades;
    public int      resolution;
    public Float[]  cascadeFar;
    public Vec4f[]  cascadeScale;
    public int[]  chunkCounts;
    public Matrix4f lightView = new Matrix4f(1);
    public boolean  frustumCull;
    public float constantBias;

    public ShadowMapRenderer(int resolution, int cascades) {
        this.resolution = resolution;
        this.cascades = cascades;
        cascadeFar = new Float[this.cascades];
        cascadeScale = new Vec4f[this.cascades];
        Arrays.fill(cascadeFar, (float) 0);
        Arrays.fill(cascadeScale, new Vec4f(1));
        chunkCounts = new int[this.cascades];

        OpenGL.use(fbo, texture, () -> {
            texture.allocate(resolution, resolution, cascades, GLTexture.BaseFormat.DEPTH_COMPONENT);
            texture.minFilter(GLTexture.MinFilter.LINEAR);
            texture.magFilter(GLTexture.MagFilter.LINEAR);
            texture.wrapMode(GLTexture.TextureCoordinate.R, GLTexture.WrapMode.CLAMP_TO_EDGE);
            texture.wrapMode(GLTexture.TextureCoordinate.S, GLTexture.WrapMode.CLAMP_TO_EDGE);
            texture.depthCompare(true);

            OpenGL.setDrawBuffers(GL_NONE);
        });
    }

    public void allocate() {
        texture.use(() -> texture.allocate(resolution, resolution, cascades, GLTexture.BaseFormat.DEPTH_COMPONENT));
    }

    public void hardwareFiltering(boolean enable) {
        texture.use(() -> {
            texture.minFilter(enable ? GLTexture.MinFilter.LINEAR : GLTexture.MinFilter.NEAREST);
            texture.magFilter(enable ? GLTexture.MagFilter.LINEAR : GLTexture.MagFilter.NEAREST);
        });
    }

    public void depthFormat(GLTexture.Format format) {
        if(!format.baseFormat().isDepth()) throw new IllegalArgumentException();
        texture.use(() -> texture.allocate(resolution, resolution, cascades, format));
    }

    private Frustum[] split() {
        record Range(float near, float far) {}

        final Frustum[] f = new Frustum[cascades];
        final Range[] r = new Range[cascades];
        final Camera camera = Main.INSTANCE.getRenderer().getCamera();
        Matrix4f view = camera.view(true, true);
        Matrix4f cameraProjection = camera.projection();

        float lambda = 0.75f;

        float far = Camera.FAR_PLANE;
        float near = Camera.NEAR_PLANE;
        float ratio = far / near;

        r[0] = new Range(near, 0);
        for (int i = 1; i < cascades; i++) {
            float si = i / (float) cascades;
            r[i] = new Range((float) (lambda * (near * Math.pow(ratio, si)) + (1 - lambda) * (near + (far - near) * si)), 0);
            r[i - 1] = new Range(r[i - 1].near, r[i].near * 1.005f);
        }
        r[cascades - 1] = new Range(r[cascades - 1].near, far);

        for (int i = 0; i < cascades; i++) {
            Range range = r[i];
            cascadeFar[i] = 0.5f * new Vec4f(0, 0, -r[i].far, 1).transform(cameraProjection).z() / r[i].far + 0.5f;

            Matrix4f projection = Matrix4f.perspective(camera.getFOV(), Main.INSTANCE.getWindow().getViewport().aspectRatio(), range.near, range.far);
            projection.multiply(view);
            f[i] = new Frustum(camera.getPosition(), projection);
        }

        return f;
    }

    public Matrix4f lightTransform(int cascade, Vec3f lightDirection) {
        final var targetPos = Main.INSTANCE.getRenderer().getCamera().getPosition();
        final Matrix4f view = Matrix4f.look(targetPos.subtract(lightDirection), targetPos);
        lightView = view;
        final var frustumCorners = split()[cascade].corners().clone();

        //Project frustum corners into light space
        for (int i = 0; i < frustumCorners.length; i++) {
            Vec3f corner = frustumCorners[i];
            Vec4f transform = corner.extend(1).transform(view);
            transform = transform.divide(transform.w());
            frustumCorners[i] = new Vec3f(transform.x(), transform.y(), transform.z());
        }

        //Find projected bounding box
        final var aabb = AABB.calculate(frustumCorners);

        //Generic orthographic projection with configured Z ranges
        final Matrix4f projection = Matrix4f.orthographic(-1, 1, -1, 1, -(aabb.max().z() + RANGE_BEHIND_CAMERA), -aabb.min().z());

        //Crop X and Y to fit bounding box
        final Matrix4f crop = new Matrix4f(1);
        crop.set(0, 0, 2 / (aabb.max().x() - aabb.min().x()));
        crop.set(1, 1, 2 / (aabb.max().y() - aabb.min().y()));
        crop.set(0, 3, -0.5f * (aabb.min().x() + aabb.max().x()) * crop.get(0, 0));
        crop.set(1, 3, -0.5f * (aabb.min().y() + aabb.max().y()) * crop.get(1, 1));

        //Result is CPV
        crop.multiply(projection);
        cascadeScale[cascade] = new Vec4f(new Vec4f(1, 0, 0, 0).transform(crop).x(), new Vec4f(0, 1, 0, 0).transform(crop).y(), crop.get(2, 2), crop.get(2, 3));
        crop.multiply(view);

        return crop;
    }

    public void render(Vec3f lightDirection) {
        OpenGL.setViewport(new Viewport(resolution, resolution));

        final int visibilityBitset = ChunkProgram.Slice.directionCull(lightDirection.scale(-1000), new Vec3i(0));

        OpenGL.use(PROGRAM, fbo, texture, () -> {
            final Collection<RenderChunk> renderChunks = new ArrayList<>(Main.INSTANCE.getRenderer().getWorldRenderer().getRenderChunks());

            //Reverse order for inherited frustum culling
            for (int c = cascades - 1; c >= 0; c--) {
                fbo.depth(texture, 0, c);
                OpenGL.clearAll();
                final Matrix4f lightTransform = lightTransform(c, lightDirection);
                PROGRAM.mvp.set(lightTransform);
                final double blocksPerScreen = 2 / cascadeScale[c].y();
                final double blocksPerPixel = blocksPerScreen / (float) resolution; //adjacent
                final double depthPerBlock = cascadeScale[c].z() * -2; //opposite
                final double scale = blocksPerPixel * depthPerBlock;

                if(c == WorldRenderer.frustumNumber) {
                    final float dot = new Vec3f(0, 1, 0).dot(lightDirection.scale(-1));
                    final float abs = Math.abs(dot);
                    final float min = Math.min(abs, 1);
                    final double acos = Math.acos(min);
                    final double tan = Math.tan(acos);
                    System.out.printf("Angle: %.5f | Tan: %.5f | Bias: %.5f%n", Math.toDegrees(acos), tan, tan * scale);
                }

                if (frustumCull) {
                    //TODO Replace with OBB?
                    final Frustum frustum = new Frustum(Main.INSTANCE.getRenderer().getCamera().getPosition(), lightTransform);
                    renderChunks.removeIf(renderChunk -> !frustum.intersects(renderChunk.getChunk().getBoundingBox()));
                }

                chunkCounts[c] = renderChunks.size();

                for (RenderChunk renderChunk : renderChunks) {
                    PROGRAM.chunk.set(Chunk.toWorldPosition(renderChunk.getChunk().getPosition()));
                    renderChunk.renderShadow(RenderType.OPAQUE, visibilityBitset);
                }
            }
        });


        OpenGL.setViewport(Main.INSTANCE.getWindow().getViewport());
    }

    public ArrayTexture2D getTexture() {
        return texture;
    }
}
