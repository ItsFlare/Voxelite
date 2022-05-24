#version 330

in vec2 aPos;
in vec2 aTex;
in vec4 aTint;
in ivec2 aInstance;

out vec2 tex;
out vec4 tint;

uniform ivec2 viewport;

void main() {
    gl_Position = vec4(aPos, 0, 1);
    tex = aTex;
    tint = aTint;
}