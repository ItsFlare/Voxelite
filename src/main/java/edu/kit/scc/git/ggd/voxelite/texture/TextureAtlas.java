package edu.kit.scc.git.ggd.voxelite.texture;

import edu.kit.scc.git.ggd.voxelite.util.Util;
import net.durchholz.beacon.math.Vec2i;
import net.durchholz.beacon.render.opengl.textures.GLTexture;
import net.durchholz.beacon.render.opengl.textures.Texture2D;
import net.durchholz.beacon.util.Image;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TextureAtlas implements GLTexture {

    private final Texture2D          texture;
    private final Map<String, Vec2i> map = new HashMap<>();
    private final float              normalizedSpriteSize;

    public TextureAtlas(String folder) throws IOException, URISyntaxException {
        texture = new Texture2D();
        List<Path> pathList = (List<Path>) Util.listResourceFolder(folder);
        pathList.removeIf(Files::isDirectory);
        int atlasGridSize = (int) Math.ceil(Math.sqrt(pathList.size()));
        normalizedSpriteSize = 1 / (float) atlasGridSize;
        List<Image> imageList = new ArrayList<>();

        texture.use(() -> {
            texture.magFilter(MagFilter.NEAREST);
            texture.minFilter(MinFilter.LINEAR);

            int spriteSize = 0;

            try {
                //Load images
                for (int i = 0; i < pathList.size(); i++) {
                    Path path = pathList.get(i);
                    Image img = new Image(Files.newInputStream(path));

                    if (i == 0) {
                        spriteSize = img.width();
                    }

                    if (img.width() != spriteSize || img.height() != spriteSize) {
                        throw new IllegalArgumentException("All sprites should have the same size");
                    }

                    imageList.add(img);
                }

                final int maxLevel = log2(spriteSize);

                //Upload all mipmap levels
                for (int level = 0; level <= maxLevel; level++) {
                    int targetSize = spriteSize >> level;
                    int atlasPixelSize = atlasGridSize * targetSize;
                    texture.allocate(atlasPixelSize, atlasPixelSize, GLTexture.SizedFormat.RGBA_8, level);

                    for (int i = 0; i < imageList.size(); i++) {
                        final int x = i % atlasGridSize, y = i / atlasGridSize;
                        Image image = imageList.get(i);

                        if (level == 0) {
                            map.put(pathList.get(i).getFileName().toString(), new Vec2i(x, y));
                        } else {
                            BufferedImage resizedBufferedImage = resizeImage(image.image(), targetSize, targetSize);
                            image = new Image(resizedBufferedImage);
                        }

                        texture.subImage(image, x * targetSize, y * targetSize, level);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public Vec2i getSprite(String name) {
        return map.get(name);
    }

    public float getNormalizedSpriteSize() {
        return normalizedSpriteSize;
    }

    @Override
    public int id() {
        return texture.id();
    }

    @Override
    public Type type() {
        return texture.type();
    }

    private static BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) throws IOException {
        java.awt.Image resultingImage = originalImage.getScaledInstance(targetWidth, targetHeight, java.awt.Image.SCALE_DEFAULT);
        BufferedImage outputImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        outputImage.getGraphics().drawImage(resultingImage, 0, 0, null);
        return outputImage;
    }

    private static int log2(int n) {
        return (int) (Math.log(n) / Math.log(2));
    }
}
