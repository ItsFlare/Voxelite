#ifndef DEFERRED
    #include "include\deferred.glsl"
#endif

vec2 viewport = textureSize(opaque, 0);
float aspectRatio = viewport.x / viewport.y;

float LinearDepth(float depth) {
    depth = depth * 2 - 1; //Convert to NDC
    float a = projection[2][2];
    float b = projection[3][2];
    float z_view = b / (a + depth);

    return z_view;
}

float SampleDepth(vec2 uv) {
    return LinearDepth(texture(depth, uv).x);
}

vec3 toViewSpace(vec2 uv) {
    float z = SampleDepth(uv);

    uv = uv * 2 - 1; //Convert to NDC
    vec3 viewPos = vec3(0);
    viewPos.x = z * uv.x / projection[0][0];
    viewPos.y = z * uv.y / projection[1][1];
    viewPos.z = -z;

    return viewPos;
}

vec2 toScreenSpace(vec3 view) {
    vec4 projected = projection * vec4(view, 1);
    projected.xy /= projected.w;
    projected.xy = projected.xy * 0.5 + 0.5;
    return projected.xy;
}