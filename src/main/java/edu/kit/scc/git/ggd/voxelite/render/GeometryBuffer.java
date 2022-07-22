package edu.kit.scc.git.ggd.voxelite.render;

import net.durchholz.beacon.render.opengl.Bindable;
import net.durchholz.beacon.render.opengl.BindableType;
import net.durchholz.beacon.render.opengl.buffers.FBO;
import net.durchholz.beacon.render.opengl.textures.GLTexture;
import net.durchholz.beacon.render.opengl.textures.Texture2D;

public record GeometryBuffer(FBO fbo,
                             Texture2D opaque,
                             Texture2D normal,
                             Texture2D mer,
                             Texture2D depth,
                             Texture2D bloom,
                             Texture2D composite) implements Bindable {

    public GeometryBuffer(int width, int height) {
        this(new FBO(), new Texture2D(), new Texture2D(), new Texture2D(), new Texture2D(), new Texture2D(), new Texture2D());
        assert width > 0 && height > 0;

        opaque.use(() -> {
            opaque.minFilter(GLTexture.MinFilter.LINEAR_MIPMAP_LINEAR);
            opaque.magFilter(GLTexture.MagFilter.LINEAR);
        });

        setFilters(normal);
        setFilters(mer);
        setFilters(depth);
        setFilters(composite);

        bloom.use(() -> {
            bloom.magFilter(GLTexture.MagFilter.LINEAR);
            bloom.minFilter(GLTexture.MinFilter.LINEAR);
            bloom.wrapMode(GLTexture.TextureCoordinate.S, GLTexture.WrapMode.CLAMP_TO_EDGE);
            bloom.wrapMode(GLTexture.TextureCoordinate.T, GLTexture.WrapMode.CLAMP_TO_EDGE);
        });

        use(() -> {
            allocate(width, height);
            fbo.color(0, opaque);
            fbo.color(1, normal);
            fbo.color(2, mer);
            fbo.color(3, composite);
            fbo.color(4, bloom);

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
        opaque.use(() -> opaque.allocate(width, height, GLTexture.SizedFormat.RGB_16F));
        normal.use(() -> normal.allocate(width, height, GLTexture.SizedFormat.RGB_16F));
        mer.use(() -> mer.allocate(width, height, GLTexture.SizedFormat.RGB_8));
        depth.use(() -> depth.allocate(width, height, GLTexture.BaseFormat.DEPTH_COMPONENT));
        bloom.use(() -> bloom.allocate(width, height, GLTexture.SizedFormat.RGB_8));
        composite.use(() -> composite.allocate(width, height, GLTexture.SizedFormat.RGBA_8));
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
        normal.delete();
        mer.delete();
        depth.delete();
        bloom.delete();
        composite.delete();
    }
}
