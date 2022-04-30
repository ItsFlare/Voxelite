package edu.kit.scc.git.ggd.voxel.input;

import edu.kit.scc.git.ggd.voxel.Main;
import net.durchholz.beacon.event.Listener;
import net.durchholz.beacon.input.Button;
import net.durchholz.beacon.input.event.KeyboardEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class InputListener {

    record Input(Button button, Runnable action) {}

    private final List<Input> inputs = new ArrayList<>();

    {
        inputs.add(new Input(Button.ESCAPE, () -> Main.WINDOW.shouldClose(true)));
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
}
