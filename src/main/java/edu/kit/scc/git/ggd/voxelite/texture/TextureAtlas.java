package edu.kit.scc.git.ggd.voxelite.texture;

import net.durchholz.beacon.math.Vec2f;
import net.durchholz.beacon.render.opengl.textures.GLTexture;
import net.durchholz.beacon.render.opengl.textures.Texture2D;
import net.durchholz.beacon.util.Image;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.lwjgl.opengl.GL11.glGenTextures;

public class TextureAtlas {
    int id;
    Texture2D texture;

    static int subImageWidth = 16;
    static int subImageHeight = 16;
    static int sheetWidth = subImageWidth * subImageWidth;
    static int sheetHeight = subImageHeight * subImageHeight;

    static int rowNumber = sheetWidth / subImageWidth;
    static int columnNumber = sheetHeight / subImageHeight;

    final File folder = new File("C:\\Users\\Gabu\\IdeaProjects\\ggd-voxelite\\src\\main\\resources\\texture.atlas\\wool");
    final List<File> fileList = Arrays.asList(folder.listFiles());

    public TextureAtlas() {
        this.id = glGenTextures();
        texture = new Texture2D(id);
        texture.allocate(sheetWidth, sheetHeight, GLTexture.SizedFormat.RGBA_8);
    }

    public void setSubImage() throws IOException {
        for (int i = 0; i < fileList.size(); i++) {
            Image img = new Image(fileList.get(i));
            texture.subImage(img, i * subImageWidth, i * subImageHeight);
        }
    }

    //returns the normalized coordinates of the given coordinates in the sheet
    public Vec2f getNormCoord(Vec2f vec) {
        float x = vec.x() / sheetWidth;
        float y = vec.y() / sheetHeight;
        return new Vec2f(x,y);
    }




}
