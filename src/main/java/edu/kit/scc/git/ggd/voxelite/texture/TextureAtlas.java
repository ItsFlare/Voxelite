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

    private final ArrayTexture2D arrayTexture;
    private final Map<String, Vec2i> map = new HashMap<>();
    private final float              normalizedSpriteSize;

    public TextureAtlas(String folder, String normalMapFolder) throws IOException, URISyntaxException {
        arrayTexture = new ArrayTexture2D();
        List<Path> pathList = (List<Path>) Util.listResourceFolder(folder);
        List<Path> pathListNormal = (List<Path>) Util.listResourceFolder(normalMapFolder);
        pathList.removeIf(Files::isDirectory);
        pathListNormal.removeIf(Files::isDirectory);
        int atlasGridSize = (int) Math.ceil(Math.sqrt(pathList.size()));
        normalizedSpriteSize = 1 / (float) atlasGridSize;
        List<Image> imageList = new ArrayList<>();
        List<Image> normalMapList = new ArrayList<>();


        arrayTexture.use(() -> {
            arrayTexture.magFilter(MagFilter.NEAREST);
            arrayTexture.minFilter(MinFilter.LINEAR);

            int spriteSize = 0;

            try {
                for (int i = 0; i < pathList.size(); i++) {
                    Path path = pathList.get(i);
                    Path normalMapPath = pathListNormal.get(i);

                    Image img = new Image(Files.newInputStream(path));
                    Image imgNormal = new Image(Files.newInputStream(normalMapPath));

                    if (i == 0) {
                        spriteSize = img.width();
                    }

                    if (img.width() != spriteSize || img.height() != spriteSize) {
                        throw new IllegalArgumentException("All sprites should have the same size");
                    }
                    if (imgNormal.width() != spriteSize || imgNormal.height() != spriteSize) {
                        throw new IllegalArgumentException("All normal sprites should have the same size as regular sprites");
                    }

                    imageList.add(img);
                    normalMapList.add(imgNormal);
                }

                final int maxLevel = log2(spriteSize);


                for (int level = 0; level <= maxLevel; level++) {
                    int targetSize = spriteSize >> level;
                    int atlasPixelSize = atlasGridSize * targetSize;
                    arrayTexture.allocate(atlasPixelSize, atlasPixelSize, 2, GLTexture.SizedFormat.RGBA_8, level);
                    for (int i = 0; i < imageList.size(); i++) {
                        final int x = i % atlasGridSize, y = i / atlasGridSize;
                        Image image = imageList.get(i);
                        Image imageNormal = normalMapList.get(i);

                        if (level == 0) {
                            String path = pathList.get(i).getFileName().toString();
                            map.put(path, new Vec2i(x, y));
                        } else {
                            BufferedImage resizedBufferedImage = resizeImage(image.image(), targetSize, targetSize);
                            BufferedImage resizedBufferedImageNormal = resizeImage(imageNormal.image(), targetSize, targetSize);
                            image = new Image(resizedBufferedImage);
                            imageNormal = new Image(resizedBufferedImageNormal);
                        }
                        arrayTexture.subImage(image, x * targetSize,y * targetSize, 0, level);
                        arrayTexture.subImage(imageNormal,x * targetSize,y * targetSize, 1, level);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });



        /*texture.use(() -> {
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
        });*/
    }

    public Vec2i getSprite(String name) {
        return map.get(name);
    }

    public Vec2i getSpriteNormal(String name) {
        return map.get(name.replace("_normal", ""));
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
