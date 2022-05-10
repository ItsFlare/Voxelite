#version 410
in vec2 Tex;

out vec4 FragColor;

uniform sampler2D atlas;

void main() {
    FragColor = texture(atlas, Tex);
}