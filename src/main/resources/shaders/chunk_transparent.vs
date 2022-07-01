#version 400 core
in uint data;
in uint light;
in uint ao;
in ivec3 tangent;
in ivec3 bitangent;
in ivec3 normal;

out uint Data;
out uint Light;
out uint AO;
out mat3 inTBN;

void main() {
    Data = data;
    Light = light;
    AO = ao;
    inTBN = mat3(vec3(tangent), vec3(bitangent), vec3(normal));
}