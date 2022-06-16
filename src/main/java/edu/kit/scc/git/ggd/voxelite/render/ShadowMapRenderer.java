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

import static org.lwjgl.opengl.GL43.GL_NONE;

public class ShadowMapRenderer {

    public static final ChunkShadowProgram PROGRAM             = new ChunkShadowProgram(Shader.vertex(Util.readShaderResource("chunk_opaque_shadow.vs")), Shader.fragment(Util.readShaderResource("default.fs")));
    public static final int                RANGE_BEHIND_CAMERA = 250;

    private final FBO            fbo     = new FBO();
    private final ArrayTexture2D texture = new ArrayTexture2D();

    public int       cascades;
    public int       resolution;
    public Cascade[] c;
    public int[]     cullCounts;
    public boolean   frustumCull;
    public float     constantBias, splitCorrection;

    record Cascade(float far, Vec3f scale, Vec3f translation) {}

    public ShadowMapRenderer(int resolution, int cascades) {
        this.resolution = resolution;
        this.cascades = cascades;
        this.c = new Cascade[cascades];
        this.cullCounts = new int[this.cascades];

        OpenGL.use(fbo, texture, () -> {
            texture.allocate(resolution, resolution, cascades, GLTexture.BaseFormat.DEPTH_COMPONENT);
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
        if (!format.baseFormat().isDepth()) throw new IllegalArgumentException();
        texture.use(() -> texture.allocate(resolution, resolution, cascades, format));
    }

    public Frustum[] split(Camera camera) {
        record Range(float near, float far) {}

        final Frustum[] f = new Frustum[cascades];
        final Range[] r = new Range[cascades];
        final Matrix4f view = camera.view(true, true);
        final Matrix4f cameraProjection = camera.projection();

        final float far = camera.getFar();
        final float near = camera.getNear();
        final float ratio = far / near;

        r[0] = new Range(near, 0);
        for (int i = 1; i < cascades; i++) {
            float si = i / (float) cascades;
            r[i] = new Range((float) (splitCorrection * (near * Math.pow(ratio, si)) + (1 - splitCorrection) * (near + (far - near) * si)), 0);
            r[i - 1] = new Range(r[i - 1].near, r[i].near * 1.005f);
        }
        r[cascades - 1] = new Range(r[cascades - 1].near, far);

        for (int i = 0; i < cascades; i++) {
            Range range = r[i];
            c[i] = new Cascade(0.5f * new Vec4f(0, 0, -r[i].far, 1).transform(cameraProjection).z() / r[i].far + 0.5f, c[i] != null ? c[i].scale : null, c[i] != null ? c[i].translation : null);

            Matrix4f projection = Matrix4f.perspective(camera.getFOV(), Main.INSTANCE.getWindow().getViewport().aspectRatio(), range.near, range.far);
            projection.multiply(view);
            f[i] = new Frustum(projection);
        }

        return f;
    }

    public Matrix4f lightView(Vec3f lightDirection) {
        final var targetPos = Main.INSTANCE.getRenderer().getCamera().getPosition();
        return Matrix4f.look(targetPos.subtract(lightDirection), targetPos);
    }

    public Matrix4f lightTransform(int cascades, Vec3f lightDirection) {
        var projection = lightProjection(cascades, lightDirection);
        projection.multiply(lightView(lightDirection));
        return projection;
    }

    public Matrix4f lightProjection(int cascade, Vec3f lightDirection) {
        final Matrix4f view = lightView(lightDirection);
        final var frustumCorners = split(Main.INSTANCE.getRenderer().getCamera())[cascade].corners().clone();

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

        Vec3f scale = new Vec3f(crop.get(0, 0), crop.get(1, 1), crop.get(2, 2));
        Vec3f offset = crop.translation();
        c[cascade] = new Cascade(c[cascade].far, scale, offset);

        return crop;
    }

    public void render(Vec3f lightDirection) {
        OpenGL.setViewport(new Viewport(resolution, resolution));
        OpenGL.depthTest(true);
        OpenGL.depthMask(true);
        OpenGL.colorMask(false);
        OpenGL.blend(false);

        final int visibility = RenderChunk.directionCull(lightDirection.scale(-1000), new Vec3i(0));

        OpenGL.use(PROGRAM, fbo, texture, () -> {
            final var renderChunks = Main.INSTANCE.getRenderer().getWorldRenderer().getRenderChunks().toArray(RenderChunk[]::new);
            int culled = 0;

            //Reverse order for successive frustum culling
            for (int c = cascades - 1; c >= 0; c--) {
                fbo.depth(texture, 0, c);
                OpenGL.clearDepth();

                final Matrix4f lightTransform = lightTransform(c, lightDirection);
                PROGRAM.mvp.set(lightTransform);

                if (frustumCull) {
                    //TODO Fix some issue that overculls
                    //TODO Replace with OBB?
                    final Frustum frustum = new Frustum(lightTransform);

                    for (int i = 0; i < renderChunks.length; i++) {
                        RenderChunk renderChunk = renderChunks[i];
                        if (renderChunk == null) continue;
                        if (!frustum.intersects(renderChunk.getChunk().getBoundingBox())) { //TODO Intersection with center sufficient?
                            renderChunks[i] = null;
                            culled++;
                        }
                    }
                }

                cullCounts[c] = culled;

                for (RenderChunk renderChunk : renderChunks) {
                    if (renderChunk == null) continue;

                    PROGRAM.chunk.set(Chunk.toWorldPosition(renderChunk.getChunk().getPosition()));
                    renderChunk.renderShadow(RenderType.OPAQUE, visibility);
                }
            }
        });


        OpenGL.setViewport(Main.INSTANCE.getWindow().getViewport());
    }

    public ArrayTexture2D getTexture() {
        return texture;
    }
}
