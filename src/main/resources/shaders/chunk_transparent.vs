#version 400 core
in uint data;
in uint light;
in uint ao;

out uint Data;
out uint Light;
out uint AO;
out uint AO_SHIFT;

void main() {
    Data = data;
    Light = light;
    AO = ao;
    AO_SHIFT = (gl_VertexID & 3) << 1;
}