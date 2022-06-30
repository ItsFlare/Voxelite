#version 330

in vec2 pos;

out vec2 pixel;

void main() {
    gl_Position = vec4(pos.xy, 0, 1);
    pixel = 0.5 * (pos + 1);
}