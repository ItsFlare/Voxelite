#version 400 core
in uint data;
in uint light;

out uint Data;
out uint Light;

void main() {
    Data = data;
    Light = light;
}