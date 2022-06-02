#version 330

in vec2 tex;
in vec4 tint;
out vec4 FragColor;

uniform sampler2D sampler;

void main() {
    FragColor = texture(sampler, tex) * tint;
    if(FragColor.a <= 0) discard;
}