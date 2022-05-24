#version 410 core
in vec3 pos;

uniform mat4 vp;
uniform vec3 center;

out vec4 vertexColor;

void main()
{
    vec3 vertexPosition_worldspace = center;

    // Get the screen-space position of the particle's center
    gl_Position = vp * vec4(vertexPosition_worldspace, 1.0f);

    // Here we have to do the perspective division ourselves.
    //gl_Position /= gl_Position.w;

    // Move the vertex in directly screen space. No need for CameraUp/Right_worlspace here.
    gl_Position.xy += pos.xy * vec2(0.2, 0.05);

    gl_Position.z = -0.2;

    vertexColor = vec4(0.5, 0.0, 0.0, 1.0);
}