package edu.kit.scc.git.ggd.voxelite.input;

import edu.kit.scc.git.ggd.voxelite.Main;
import edu.kit.scc.git.ggd.voxelite.render.Camera;
import net.durchholz.beacon.event.Listener;
import net.durchholz.beacon.input.Button;
import net.durchholz.beacon.input.ButtonAction;
import net.durchholz.beacon.input.event.KeyboardEvent;
import net.durchholz.beacon.input.event.MouseEvent;
import net.durchholz.beacon.math.Quaternion;
import net.durchholz.beacon.math.Vec2d;
import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.window.Window;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class InputListener {

    public static final float DEFAULT_SENSITIVITY  = 0.5f;
    public static final int DEFAULT_CAMERA_SPEED = 20;

    record Input(Button button, Runnable action) {}

    private final List<Input> inputs = new ArrayList<>();
    private       Vec2d       cursor = new Vec2d(0);

    public float sensitivity = DEFAULT_SENSITIVITY;
    public float cameraSpeed = DEFAULT_CAMERA_SPEED;

    public InputListener() {

        inputs.add(new Input(Button.ESCAPE, () -> Main.INSTANCE.getWindow().shouldClose(true)));
        inputs.add(new Input(Button.F3, () -> Main.INSTANCE.getWindow().setCursorMode(Main.INSTANCE.getWindow().getCursorMode() == Window.CursorMode.DISABLED ? Window.CursorMode.NORMAL : Window.CursorMode.DISABLED)));
        inputs.add(new Input(Button.F1, () -> Main.INSTANCE.getRenderer().renderUI = !Main.INSTANCE.getRenderer().renderUI));
    }

    @Listener
    private void onKeyboard(KeyboardEvent event) {
        if (event.action() == GLFW.GLFW_RELEASE) return;

        for (Input input : inputs) {
            if (input.button().scancode() == event.scancode()) {
                input.action().run();
            }
        }
    }

    public void move(float alpha) {
        final Window window = Main.INSTANCE.getWindow();
        final Camera camera = Main.INSTANCE.getRenderer().getCamera();

        if (window.getButton(Button.W) == ButtonAction.PRESSED) camera.move(new Vec3f(0, 0, -1).rotate(camera.getRotation()).scale(cameraSpeed).scale(alpha));
        if (window.getButton(Button.A) == ButtonAction.PRESSED) camera.move(new Vec3f(-1, 0, 0).rotate(camera.getRotation()).scale(cameraSpeed).scale(alpha));
        if (window.getButton(Button.S) == ButtonAction.PRESSED) camera.move(new Vec3f(0, 0, 1).rotate(camera.getRotation()).scale(cameraSpeed).scale(alpha));
        if (window.getButton(Button.D) == ButtonAction.PRESSED) camera.move(new Vec3f(1, 0, 0).rotate(camera.getRotation()).scale(cameraSpeed).scale(alpha));
        if (window.getButton(Button.SPACE) == ButtonAction.PRESSED) camera.move(new Vec3f(0, 1, 0).scale(cameraSpeed).scale(alpha));
        if (window.getButton(Button.LEFT_SHIFT) == ButtonAction.PRESSED) camera.move(new Vec3f(0, -1, 0).scale(cameraSpeed).scale(alpha));
    }

    @Listener
    private void onMouse(MouseEvent event) {
        if (Main.INSTANCE.getWindow().getCursorMode() != Window.CursorMode.DISABLED) return;

        final Quaternion yaw = Quaternion.ofAxisAngle(new Vec3f(0, 1, 0), (float) ((cursor.x() - event.x()) * sensitivity));
        final Quaternion pitch = Quaternion.ofAxisAngle(new Vec3f(1, 0, 0), (float) ((cursor.y() - event.y()) * sensitivity));

        final Camera camera = Main.INSTANCE.getRenderer().getCamera();
        camera.setRotation(yaw.multiply(camera.getRotation()).multiply(pitch).normalized());

        cursor = new Vec2d(event.x(), event.y());
    }
}
