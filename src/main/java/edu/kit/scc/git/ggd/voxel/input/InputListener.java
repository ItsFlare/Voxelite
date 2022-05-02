package edu.kit.scc.git.ggd.voxel.input;

import edu.kit.scc.git.ggd.voxel.Main;
import edu.kit.scc.git.ggd.voxel.render.Camera;
import net.durchholz.beacon.event.Listener;
import net.durchholz.beacon.input.Button;
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
    public static final float DEFAULT_CAMERA_SPEED = 0.5f;

    record Input(Button button, Runnable action) {}

    private final Main        main;
    private final List<Input> inputs = new ArrayList<>();
    private       Vec2d       cursor = new Vec2d(0);

    public float sensitivity;
    public float cameraSpeed;

    public InputListener(Main main) {
        this.main = main;

        inputs.add(new Input(Button.ESCAPE, () -> this.main.getWindow().shouldClose(true)));

        inputs.add(new Input(Button.W,          () -> this.main.getRenderer().getCamera().move(new Vec3f(0, 0, -1).rotate(this.main.getRenderer().getCamera().getRotation()).scale(cameraSpeed))));
        inputs.add(new Input(Button.A,          () -> this.main.getRenderer().getCamera().move(new Vec3f(-1, 0, 0).rotate(this.main.getRenderer().getCamera().getRotation()).scale(cameraSpeed))));
        inputs.add(new Input(Button.S,          () -> this.main.getRenderer().getCamera().move(new Vec3f(0, 0, 1).rotate(this.main.getRenderer().getCamera().getRotation()).scale(cameraSpeed))));
        inputs.add(new Input(Button.D,          () -> this.main.getRenderer().getCamera().move(new Vec3f(1, 0, 0).rotate(this.main.getRenderer().getCamera().getRotation()).scale(cameraSpeed))));
        inputs.add(new Input(Button.SPACE,      () -> this.main.getRenderer().getCamera().move(new Vec3f(0, 1, 0).scale(cameraSpeed))));
        inputs.add(new Input(Button.LEFT_SHIFT, () -> this.main.getRenderer().getCamera().move(new Vec3f(0, -1, 0).scale(cameraSpeed))));

        inputs.add(new Input(Button.F3, () -> this.main.getWindow().setCursorMode(this.main.getWindow().getCursorMode() == Window.CursorMode.DISABLED ? Window.CursorMode.NORMAL : Window.CursorMode.DISABLED)));
        inputs.add(new Input(Button.F1, () -> this.main.getRenderer().renderUI = !this.main.getRenderer().renderUI));
    }

    @Listener
    public void onKeyboard(KeyboardEvent event) {
        if(event.action() == GLFW.GLFW_RELEASE) return;

        for (Input input : inputs) {
            if (input.button().scancode() == event.scancode()) {
                input.action().run();
            }
        }
    }


    @Listener
    public void onMouse(MouseEvent event) {
        if(main.getWindow().getCursorMode() != Window.CursorMode.DISABLED) return;

        final Quaternion yaw = Quaternion.ofAxisAngle(new Vec3f(0, 1, 0), (float) ((cursor.x() - event.x()) * sensitivity));
        final Quaternion pitch = Quaternion.ofAxisAngle(new Vec3f(1, 0, 0), (float) ((cursor.y() - event.y()) * sensitivity));

        final Camera camera = main.getRenderer().getCamera();
        camera.setRotation(yaw.multiply(camera.getRotation()).multiply(pitch).normalized());

        cursor = new Vec2d(event.x(), event.y());
    }
}
