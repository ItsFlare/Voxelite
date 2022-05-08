package edu.kit.scc.git.ggd.voxelite.texture;

import edu.kit.scc.git.ggd.voxelite.objects.Block;
import net.durchholz.beacon.math.Vec2f;
import net.durchholz.beacon.render.opengl.textures.GLTexture;
import net.durchholz.beacon.render.opengl.textures.Texture2D;
import net.durchholz.beacon.util.Image;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import static org.lwjgl.opengl.GL11.glGenTextures;

public class TextureAtlas {
    int id;
    Texture2D texture;

    public static int subImageWidth = 16;
    public static int subImageHeight = 16;
    static int sheetWidth = subImageWidth * subImageWidth;
    static int sheetHeight = subImageHeight * subImageHeight;

    static int rowNumber = sheetWidth / subImageWidth;
    static int columnNumber = sheetHeight / subImageHeight;

    URL url = ClassLoader.getSystemResource("resources/texture/atlas/wool");
    final File folder = new File(url.getFile());
    final List<File> fileList = Arrays.asList(folder.listFiles());

    public TextureAtlas() {
        this.id = glGenTextures();
        texture = new Texture2D(id);
        texture.allocate(sheetWidth, sheetHeight, GLTexture.SizedFormat.RGBA_8);
    }

    public void setSubImage() throws IOException {
        for (int i = 0; i < fileList.size(); i++) {
            Image img = new Image(fileList.get(i));
            texture.subImage(img, (i % rowNumber) * subImageWidth, (i / columnNumber) * subImageHeight);
        }
    }

    //returns the normalized coordinates of the given coordinates in the sheet fot the block type
    public static Vec2f[] getNormCoord(Block.Type blockType) {
        Vec2f[] coord = new Vec2f[4];
        //bottom left
        coord[0] = new Vec2f(((blockType.id() % rowNumber) * subImageWidth) / sheetWidth, ((blockType.id() / rowNumber) * subImageHeight) / sheetHeight);
        //bottom right
        coord[1] = new Vec2f((((blockType.id() % rowNumber) + 1) * subImageWidth) / sheetWidth, ((blockType.id() / rowNumber) * subImageHeight) / sheetHeight);
        //top left
        coord[2] = new Vec2f(((blockType.id() % rowNumber) * subImageWidth) / sheetWidth, (((blockType.id() / rowNumber) + 1) * subImageHeight) / sheetHeight);
        //top right
        coord[3] = new Vec2f((((blockType.id() % rowNumber) + 1) * subImageWidth) / sheetWidth, (((blockType.id() / rowNumber) + 1) * subImageHeight) / sheetHeight);
        return coord;
    }


}
