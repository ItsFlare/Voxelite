#version 410 core
in vec3 pos;

uniform mat4 mvp;

out vec4 vertexColor;

void main()
{
    gl_Position = mvp * vec4(pos, 1.0f);

    vertexColor = vec4(0.5, 0.0, 0.0, 1.0);
}