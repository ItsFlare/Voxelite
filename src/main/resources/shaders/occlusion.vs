#version 330

in vec3 pos;
in vec3 min;
in vec3 max;

flat out int id;

uniform mat4 mvp;

void main() {
    gl_Position = mvp * vec4(min + (max * pos), 1);
    id = gl_InstanceID;
}