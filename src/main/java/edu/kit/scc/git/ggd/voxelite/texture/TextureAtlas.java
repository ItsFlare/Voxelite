package edu.kit.scc.git.ggd.voxelite.texture;

import edu.kit.scc.git.ggd.voxelite.util.Util;
import net.durchholz.beacon.math.Vec2f;
import net.durchholz.beacon.render.opengl.textures.GLTexture;
import net.durchholz.beacon.render.opengl.textures.Texture2D;
import net.durchholz.beacon.util.Image;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TextureAtlas implements GLTexture {

    private final Texture2D texture;
    private float normalizedImageSize;

    private final Map<String, Vec2f> map = new HashMap<>();

    public TextureAtlas(String folder) throws IOException, URISyntaxException {
        texture = new Texture2D();
        List<Path> pathList = (List<Path>) Util.listResourceFolder(folder);
        pathList.removeIf(Files::isDirectory);
        int atlasGridSize = (int) Math.ceil(Math.sqrt(pathList.size()));
        List<Image> imageList = new ArrayList<>();

        texture.use(() -> {
            texture.magFilter(MagFilter.NEAREST);
            texture.minFilter(MinFilter.LINEAR);

            int spriteSize = 0;

            try {
                //Load images
                for (int i = 0; i < pathList.size(); i++) {
                    Path path = pathList.get(i);
                    InputStream is = Files.newInputStream(path);
                    //System.out.println(pathList.get(i).toString());
                    Image img = new Image(is);

                    if (i == 0) {
                        spriteSize = img.width();
                        normalizedImageSize = 1 / (float) atlasGridSize;
                    }

                    if (img.width() != spriteSize || img.height() != spriteSize) {
                        throw new IllegalArgumentException("All subimages should have the same size");
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
                        Image image = imageList.get(i);
                        int x = (i % atlasGridSize) * targetSize, y = (i / atlasGridSize) * targetSize;

                        if (level == 0) {
                            map.put(pathList.get(i).getFileName().toString(), new Vec2f(x, y).divide(new Vec2f(atlasPixelSize)));
                        } else {
                            BufferedImage resizedBufferedImage = resizeImage(image.image(), targetSize, targetSize);
                            image = new Image(resizedBufferedImage);
                        }

                        texture.subImage(image, x, y, level);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    //returns the normalized coordinates of the given coordinates in the sheet fot the block type
    public Vec2f[] getNormCoord(String fileName) {
        Vec2f origin = map.get(fileName);
        float x = origin.x();
        float y = origin.y();
        float size = normalizedImageSize;

        Vec2f[] coord = new Vec2f[4];
        //bottom left
        coord[0] = new Vec2f(x, y);
        //bottom right
        coord[1] = new Vec2f(x + size, y);
        //top left
        coord[2] = new Vec2f(x, y + size);
        //top right
        coord[3] = new Vec2f(x + size, y + size);
        return coord;
    }

    private BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) throws IOException {
        java.awt.Image resultingImage = originalImage.getScaledInstance(targetWidth, targetHeight, java.awt.Image.SCALE_DEFAULT);
        BufferedImage outputImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        outputImage.getGraphics().drawImage(resultingImage, 0, 0, null);
        return outputImage;
    }

    private static int log2(int n) {
        return (int) (Math.log(n) / Math.log(2));
    }

    @Override
    public int id() {
        return texture.id();
    }
    @Override
    public Type type() {
        return texture.type();
    }
}
