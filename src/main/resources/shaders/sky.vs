#version 410 core
in vec2 ndc;

void main() {
    gl_Position = vec4(ndc, 1, 1);
}