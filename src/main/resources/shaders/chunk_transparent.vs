#version 400 core
in uint data;
in uint light;
in uint ao;

out uint Data;
out uint Light;
out uint AO;
out uint BYTEINDEX;
out uint BITINDEX;

void main() {
    Data = data;
    Light = light;
    AO = ao;
    BYTEINDEX = gl_InstanceID % 4;
    BITINDEX = gl_VertexID % 4;
}