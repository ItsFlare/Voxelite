out float aoFactor;

uniform bool aoSet;

const vec4 aoMap = vec4(0.5, 0.7, 0.9, 1);

float AmbientOcclusion(uint data, int id) {
    return aoSet ? aoMap[(data >> ((id & 3) << 1)) & 3u] : 1;
}
