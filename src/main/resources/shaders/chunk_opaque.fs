#version 410
in vec2 Tex;

out vec4 FragColor;

void main() {
    FragColor = vec4(1, Tex.x, Tex.y, 1);
}