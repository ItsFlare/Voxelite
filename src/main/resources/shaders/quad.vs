#version 410 core
in vec3 pos;
in vec2 uv;

out vec2 TexCoord;

uniform mat4 mvp;
uniform vec2 size;
uniform vec2 offset;

void main()
{
    gl_Position = mvp * vec4(pos, 1.0f);
    TexCoord = (uv + offset) * size;
}