#version 330

in vec3 pos;
in vec3 min;
in vec3 max;
in int id;

flat out int ID;

uniform mat4 mvp;

void main() {
    gl_Position = mvp * vec4(min + (max * pos), 1);
    ID = id + 1;
}