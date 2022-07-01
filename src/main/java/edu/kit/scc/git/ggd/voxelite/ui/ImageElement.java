package edu.kit.scc.git.ggd.voxelite.ui;

import edu.kit.scc.git.ggd.voxelite.util.AsyncProducer;
import imgui.ImGui;
import net.durchholz.beacon.math.Vec2i;
import net.durchholz.beacon.render.opengl.textures.Texture2D;
import net.durchholz.beacon.util.Image;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ImageElement extends TitledElement {

    record Result(ByteBuffer buffer, Vec2i size) {}

    private final Function<Vec2i, BufferedImage> renderer;
    private final Supplier<Vec2i>                size;
    private final Texture2D             t = new Texture2D();
    private final AsyncProducer<Result> producer;
    private final RenderTask            task;

    protected ImageElement(String title, Supplier<Vec2i> size, Function<Vec2i, BufferedImage> renderer) {
        super(title);
        this.size = size;
        this.renderer = renderer;
        this.task = new RenderTask();
        this.producer = new AsyncProducer<>(task, UserInterface.EXECUTOR);
    }

    @Override
    public void draw() {
        final Vec2i size = this.size.get();
        task.size = size;

        var img = producer.get();
        if (img != null) {
            t.use(() -> {
                t.image(img.buffer, img.size.x(), img.size.y());
                t.generateMipmap();
            });
        }

        t.use(() -> {
            if (t.width(0) > 0) {
                ImGui.image(t.id(), size.x(), size.y());
            }
        });
    }

    private class RenderTask implements Supplier<Result> {
        private volatile Vec2i size = new Vec2i(1);

        @Override
        public Result get() {
            var rendered = renderer.apply(size);
            var img = new Image(rendered);
            return new Result(img.toBuffer(), new Vec2i(img.width(), img.height()));
        }
    }
}
