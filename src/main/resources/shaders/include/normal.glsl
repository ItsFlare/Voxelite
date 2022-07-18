flat in ivec3 Normal;
flat in mat3 TBN;

uniform bool normalMapSet;
vec2 atlasSize = textureSize(atlas, 0).xy;

vec3 GetNormal(vec2 uv) {
    if(!normalMapSet) return Normal;

    vec3 n = texture(atlas, vec3(Tex, 1)).rgb;
    if(n == vec3(0)) return Normal;

    n = n * 2 - 1;
    n = normalize(TBN * n);

    return n;
}