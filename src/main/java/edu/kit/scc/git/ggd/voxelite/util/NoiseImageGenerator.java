package edu.kit.scc.git.ggd.voxelite.util;

import net.durchholz.beacon.math.Vec2f;
import net.durchholz.beacon.math.Vec2i;
import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.util.ToFloatFunction;

import java.awt.*;
import java.awt.image.BufferedImage;

public class NoiseImageGenerator {

    public static BufferedImage generate(ToFloatFunction<Vec3f> source, Vec3f position, Vec2i resolution, float zoom) {
        int dotRadius = 2;
        float zoomFactor = 1 / zoom;
        Vec2f halfRes = new Vec2f(resolution.x(), resolution.y()).scale(0.5f);

        BufferedImage img = new BufferedImage(resolution.x(), resolution.y(), BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < resolution.x(); x++) {
            for (int y = 0; y < resolution.y(); y++) {

                //Center dot
                if (Math.abs(x - halfRes.x()) < dotRadius && Math.abs(y - halfRes.y()) < dotRadius) {
                    img.setRGB(x, y, Color.RED.getRGB());
                    continue;
                }

                var pixelPos = position.add(new Vec3f(x, 0, y).subtract(new Vec3f(halfRes.x(), 0, halfRes.y())).scale(zoomFactor));
                int c = (int) (source.applyAsFloat(pixelPos) * 255f);
                final Color color;
                if(c < 0) {
                    color = new Color(0, 0, 255);
                } else if(c > 255) {
                    color = new Color(255, 0, 0);
                } else {
                    color = new Color(c, c, c);
                }

                img.setRGB(x, y, color.getRGB());
            }
        }
        img.flush();

        return img;
    }

}
