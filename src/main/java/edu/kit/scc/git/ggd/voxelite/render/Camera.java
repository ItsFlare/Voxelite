package edu.kit.scc.git.ggd.voxelite.render;

import edu.kit.scc.git.ggd.voxelite.render.event.CameraMoveEvent;
import net.durchholz.beacon.math.Matrix4f;
import net.durchholz.beacon.math.Quaternion;
import net.durchholz.beacon.math.Vec3f;
import net.durchholz.beacon.window.Window;

public class Camera {
    public static final int   DEFAULT_FOV = 120;
    public static final float NEAR_PLANE  = 0.2f;
    public static final float FAR_PLANE   = 500f;

    private final Window window;

    private Vec3f      position = new Vec3f(0);
    private Quaternion rotation = new Quaternion();
    private int        fov      = DEFAULT_FOV;

    public Camera(Window window) {
        this.window = window;
    }

    public Vec3f getPosition() {
        return position;
    }

    public void setPosition(Vec3f position) {
        Vec3f previous = this.position;
        this.position = position;
        new CameraMoveEvent(previous, position).fire();
    }

    public void move(Vec3f delta) {
        Vec3f previous = this.position;
        this.position = this.position.add(delta);
        new CameraMoveEvent(previous, this.position).fire();
    }

    public Quaternion getRotation() {
        return rotation;
    }

    public void setRotation(Quaternion rotation) {
        this.rotation = rotation;
    }

    public void rotate(Quaternion rotation) {
        this.rotation = this.rotation.multiply(rotation).normalized();
    }

    public int getFOV() {
        return fov;
    }

    public void setFOV(int fov) {
        this.fov = fov;
    }

    public Matrix4f view(boolean translate, boolean rotate) {
        final Matrix4f view = rotate ? Matrix4f.rotation(rotation) : new Matrix4f(1);
        if (translate) view.translate(position);
        view.invert();

        return view;
    }

    public Matrix4f projection() {
        return Matrix4f.perspective(fov, window.getViewport().aspectRatio(), NEAR_PLANE, FAR_PLANE);
    }

    public Matrix4f transform() {
        final Matrix4f projection = projection();
        projection.multiply(view(true, true));

        return projection;
    }
}
