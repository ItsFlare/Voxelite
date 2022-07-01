#version 410 core
in vec3 pos;
in vec4 color;

out vec4 Color;

uniform mat4 mvp;

void main() {
    gl_Position = mvp * vec4(pos, 1.0f);
    Color = color;
}