#version 410 core
in vec3 pos;
out vec3 uv;

uniform mat4 mvp;

void main()
{
    uv = pos;
    gl_Position = mvp * vec4(pos, 1.0);
}