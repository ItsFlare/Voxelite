#version 410 core
in vec3 pos;
in vec2 uv;

out vec2 TexCoord;

uniform mat4 mvp;
uniform vec2 sprite;
uniform float normalizedSpriteSize;

void main()
{
    gl_Position = mvp * vec4(pos, 1.0f);
    TexCoord = (uv + sprite) * normalizedSpriteSize;
}