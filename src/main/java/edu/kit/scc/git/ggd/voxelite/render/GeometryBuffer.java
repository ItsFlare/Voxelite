package edu.kit.scc.git.ggd.voxelite.render;

import net.durchholz.beacon.render.opengl.Bindable;
import net.durchholz.beacon.render.opengl.BindableType;
import net.durchholz.beacon.render.opengl.OpenGL;
import net.durchholz.beacon.render.opengl.buffers.FBO;
import net.durchholz.beacon.render.opengl.textures.GLTexture;
import net.durchholz.beacon.render.opengl.textures.Texture2D;

public record GeometryBuffer(FBO fbo, Texture2D opaque, Texture2D transparent, Texture2D normal, Texture2D mer, Texture2D position, Texture2D depth) implements Bindable {

    public GeometryBuffer(int width, int height) {
        this(new FBO(), new Texture2D(), new Texture2D(), new Texture2D(), new Texture2D(), new Texture2D(), new Texture2D());
        assert width > 0 && height > 0;

        opaque.use(() -> {
            opaque.minFilter(GLTexture.MinFilter.LINEAR_MIPMAP_LINEAR);
            opaque.magFilter(GLTexture.MagFilter.LINEAR);
        });

        setFilters(transparent);
        setFilters(normal);
        setFilters(mer);
        setFilters(position);
        setFilters(depth);

        use(() -> {
            allocate(width, height);
            fbo.color(0, opaque);
            fbo.color(1, transparent);
            fbo.color(2, normal);
            fbo.color(3, mer);
            fbo.color(4, position);

            //Remove use
            depth.use(() -> {
                fbo.depth(depth, 0);
            });
        });
    }

    @Override
    public int id() {
        return fbo.id();
    }

    @Override
    public BindableType type() {
        return FBO.TYPE;
    }

    public void allocate(int width, int height) {
        opaque.use(() -> opaque.allocate(width, height, GLTexture.SizedFormat.RGBA_8, OpenGL.Type.UNSIGNED_BYTE));
        transparent.use(() -> transparent.allocate(width, height, GLTexture.SizedFormat.RGBA_8, OpenGL.Type.UNSIGNED_BYTE));
        normal.use(() -> normal.allocate(width, height, GLTexture.SizedFormat.RGBA_16F, OpenGL.Type.FLOAT));
        mer.use(() -> mer.allocate(width, height, GLTexture.SizedFormat.RGB_8, OpenGL.Type.FLOAT));
        position.use(() -> position.allocate(width, height, GLTexture.SizedFormat.RGBA_16F, OpenGL.Type.FLOAT));
        depth.use(() -> depth.allocate(width, height, GLTexture.BaseFormat.DEPTH_COMPONENT));
    }

    private static void setFilters(Texture2D texture) {
        texture.use(() -> {
            texture.minFilter(GLTexture.MinFilter.NEAREST);
            texture.magFilter(GLTexture.MagFilter.NEAREST);
        });
    }

    @Override
    public void delete() {
        fbo.delete();
        opaque.delete();
        transparent.delete();
        normal.delete();
        mer.delete();
        position.delete();
        depth.delete();
    }
}
