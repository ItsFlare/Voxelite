package edu.kit.scc.git.ggd.voxelite.texture;

import edu.kit.scc.git.ggd.voxelite.util.Util;
import net.durchholz.beacon.math.Vec2i;
import net.durchholz.beacon.render.opengl.textures.ArrayTexture2D;
import net.durchholz.beacon.render.opengl.textures.GLTexture;
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

    private final ArrayTexture2D     arrayTexture;
    private final Map<String, Vec2i> map = new HashMap<>();
    private final float              normalizedSpriteSize;

    record Sprite(String name, Image color, Image normal, Image mer) {}

    public TextureAtlas(String folder) throws IOException, URISyntaxException {
        arrayTexture = new ArrayTexture2D();
        List<Path> pathList = (List<Path>) Util.listResourceFolder(folder + "/color");
        pathList.removeIf(Files::isDirectory);
        int atlasGridSize = (int) Math.ceil(Math.sqrt(pathList.size()));
        normalizedSpriteSize = 1 / (float) atlasGridSize;
        List<Sprite> spriteList = new ArrayList<>();

        arrayTexture.use(() -> {
            arrayTexture.magFilter(MagFilter.NEAREST);
            arrayTexture.minFilter(MinFilter.LINEAR);

            int spriteSize = 0;

            try {
                for (Path path : pathList) {
                    final String name = path.getFileName().toString().split("\\.")[0];
                    final Path normalPath = path.getParent().resolveSibling("normal").resolve(name + "_normal.png");
                    final Path merPath = path.getParent().resolveSibling("mer").resolve(name + "_mer.png");

                    final Image color = new Image(Files.newInputStream(path));
                    final Image normal = Files.isRegularFile(normalPath) ? new Image(Files.newInputStream(normalPath)) : null;
                    final Image mer = Files.isRegularFile(merPath) ? new Image(Files.newInputStream(merPath)) : null;

                    if (spriteSize == 0) {
                        spriteSize = color.width();
                    }

                    if (color.width() != spriteSize || color.height() != spriteSize
                            || normal.width() != spriteSize || normal.height() != spriteSize
                            || mer.width() != spriteSize || mer.height() != spriteSize) {
                        throw new IllegalArgumentException("All sprites should have the same size");
                    }

                    spriteList.add(new Sprite(name, color, normal, mer));
                }

                final int maxLevel = log2(spriteSize);


                for (int level = 0; level <= maxLevel; level++) {
                    int targetSize = spriteSize >> level;
                    int atlasPixelSize = atlasGridSize * targetSize;
                    arrayTexture.allocate(atlasPixelSize, atlasPixelSize, 3, GLTexture.SizedFormat.RGBA_8, level);

                    for (int i = 0; i < spriteList.size(); i++) {
                        final int x = i % atlasGridSize, y = i / atlasGridSize;
                        final Sprite sprite = spriteList.get(i);
                        final Image color, normal, mer;

                        if (level == 0) {
                            map.put(sprite.name, new Vec2i(x, y));
                            color = sprite.color;
                            normal = sprite.normal;
                            mer = sprite.mer;
                        } else {
                            color = resizeImage(sprite.color, targetSize, targetSize, false);
                            normal = resizeImage(sprite.normal, targetSize, targetSize, true);
                            mer = resizeImage(sprite.normal, targetSize, targetSize, false);
                        }

                        final int posX = x * targetSize;
                        final int posY = y * targetSize;
                        arrayTexture.subImage(color, posX, posY, 0, level);
                        arrayTexture.subImage(normal, posX, posY, 1, level);
                        arrayTexture.subImage(mer, posX, posY, 2, level);
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
        return arrayTexture.id();
    }

    @Override
    public Type type() {
        return arrayTexture.type();
    }

    private static Image resizeImage(Image originalImage, int targetWidth, int targetHeight, boolean normalize) throws IOException {
        //TODO Normalize
        java.awt.Image resultingImage = originalImage.image().getScaledInstance(targetWidth, targetHeight, java.awt.Image.SCALE_DEFAULT);
        BufferedImage bi = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        bi.getGraphics().drawImage(resultingImage, 0, 0, null);
        return new Image(bi);
    }

    private static int log2(int n) {
        return (int) (Math.log(n) / Math.log(2));
    }
}
