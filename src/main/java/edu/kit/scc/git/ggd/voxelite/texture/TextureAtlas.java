package edu.kit.scc.git.ggd.voxelite.texture;

import edu.kit.scc.git.ggd.voxelite.Main;
import net.durchholz.beacon.math.Vec2f;
import net.durchholz.beacon.render.opengl.textures.GLTexture;
import net.durchholz.beacon.render.opengl.textures.Texture2D;
import net.durchholz.beacon.util.Image;
import net.durchholz.beacon.util.Util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static org.lwjgl.opengl.GL11.glGenTextures;

public class TextureAtlas implements GLTexture {
    private Texture2D texture;
    public static int subImageSize;
    private static int atlasSize;
    private int atlasSquareSize;

    private static HashMap<String, Vec2f> map = new HashMap<String, Vec2f>();

    private List<File> fileList;

    public TextureAtlas(String ressourceUrl) throws IOException, URISyntaxException {
        texture = new Texture2D();
        fileList = getAllFilesFromResource(ressourceUrl);
        subImageSize = new Image(fileList.get(0)).width();
        atlasSize = subImageSize * subImageSize;

        atlasSquareSize = (int) Math.ceil(Math.sqrt(fileList.size()));

        texture.use(() -> {
                texture.allocate(atlasSize, atlasSize, GLTexture.SizedFormat.RGBA_8);
            try {
                setSubImage();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void setSubImage() throws IOException {
        for (int i = 0; i < fileList.size(); i++) {
            Image img = new Image(fileList.get(i));
            if (img.width() != subImageSize) {
                System.out.print("All subimages should have the same size");
                return;
            }
           /* System.out.print(fileList.get(i).getPath() + "\n");
            System.out.print("x:" + (i % atlasSquareSize) * subImageSize + " y:" + (i / atlasSquareSize) * subImageSize + "\n");*/
            map.put(fileList.get(i).getPath(), new Vec2f((i % atlasSquareSize) * subImageSize, (i / atlasSquareSize) * subImageSize));
            texture.subImage(img, (i % atlasSquareSize) * subImageSize, (i / atlasSquareSize) * subImageSize);
        }
    }

    //returns the normalized coordinates of the given coordinates in the sheet fot the block type
    public static Vec2f[] getNormCoord(String fileName) {
        Vec2f vec = map.get(fileName);
        float num1 = vec.x() / atlasSize;
        float num2 = (vec.x() + 1) / atlasSize;

        Vec2f[] coord = new Vec2f[4];
        //bottom left
        coord[0] = new Vec2f(num1, num1);
        //bottom right
        coord[1] = new Vec2f(num2, num1);
        //top left
        coord[2] = new Vec2f(num1, num2);
        //top right
        coord[3] = new Vec2f(num2, num2);
        return coord;
    }

    private List<File> getAllFilesFromResource(String folder)
            throws URISyntaxException, IOException {

        ClassLoader classLoader = getClass().getClassLoader();

        URL resource = classLoader.getResource(folder);

        List<File> collect = Files.walk(Paths.get(resource.toURI()))
                .filter(Files::isRegularFile)
                .map(x -> x.toFile())
                .collect(Collectors.toList());

        return collect;
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
