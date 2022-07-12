ivec3 decodePosition(uint data) {
    uint x = data >> 27;
    uint y = (data >> 22) & uint(0x1f);
    uint z = (data >> 17) & uint(0x1f);

    return ivec3(x, y, z);
}

ivec2 decodeTexture(uint data) {
    uint u = (data >> 10) & uint(0x7f);
    uint v = (data >> 3) & uint(0x7f);

    return ivec2(u, v);
}

vec3 decodeLight(uint data) {
    return vec3(data >> 20, (data >> 10) & uint(0x3ff), data & uint(0x3ff));
}