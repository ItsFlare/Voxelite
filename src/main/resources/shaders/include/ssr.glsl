#extension GL_ARB_shading_language_include : require
#include "util.glsl"

#ifndef DEFERRED
    #include "deferred.glsl"
#endif

uniform float debugRoughness;
uniform bool reflections;
uniform bool coneTracing;

const int SEARCH_STEPS = 25;
const int MAX_STEPS = 200;
const float RAY_STRIDE = 1;
const int CONE_STEPS = 8;

vec2 search(vec3 position, vec3 direction) {
    for (int i = 0; i < SEARCH_STEPS; i++){
        direction *= 0.8;

        float z = -SampleDepth(toScreenSpace(position));

        position += sign(z - position.z) * direction;
    }

    return toScreenSpace(position);
}

bool rayMarchView(vec3 position, vec3 direction, out vec2 hitPixel) {
    direction *= RAY_STRIDE;

    for (int i = 0; i < MAX_STEPS; i++) {
        position += direction;
        vec2 ss = toScreenSpace(position).xy;
        if (ss.y < 0 || ss.y > 1) break;
        if (ss.x < 0 || ss.x > 1) break;

        float z = -SampleDepth(ss);

        if (position.z <= z) {
            hitPixel = search(position, direction);
            //hitPixel = ss;
            return true;
        }
    }

    //Pretend our ray converged to infinity
    hitPixel = toScreenSpace(position).xy;
    return true;
}

/*
Uludag. Hi-z screen-space cone-traced reflections. 2014.
*/

float CalculateConeAngle(float roughness) {
    float a = 2 + (16 - 2) * roughness;
    return cos(0.244 / (a + 1));
}

float CalculateIsoscelesTriangleBase(float angle, float rayLength) {
    return 2 * rayLength * tan(angle);
}

float CalculateIsoscelesInRadius(float base, float rayLength) {
    return (base * (sqrt(base * base + 4 * rayLength * rayLength) - base)) / (4 * rayLength);
}

float CalculateFade(vec2 hit) {
    float I_end = 1;
    float I_start = 0.7;
    float D_boundary = length(hit - vec2(0.5)) * 2;
    float f_border = clamp((D_boundary - I_start) / (I_end - I_start), 0.0, 1.0);

    return f_border;
}

vec4 ConeTrace(vec2 rayOrigin, vec2 rayDirection, float roughness) {
    vec4 color = vec4(0);
    vec2 rayDirectionNormalized = normalize(rayDirection);
    float rayLength = length(rayDirection);

    float glossiness = 1 - min(roughness, 0.999); //TODO Improve singularity fix
    int maxLevel = textureQueryLevels(opaque);

    for (int i = 0; i < CONE_STEPS; i++) {
        float angle = CalculateConeAngle(roughness);
        float base = CalculateIsoscelesTriangleBase(angle, rayLength);
        float radius = CalculateIsoscelesInRadius(base, rayLength);

        vec2 pos = rayOrigin + rayDirectionNormalized * (rayLength - radius);
        float level = clamp(log2(radius * max(viewport.x, viewport.y)), 0, maxLevel);

        color += vec4(textureLod(opaque, pos.xy, level).rgb * glossiness, glossiness);
        rayLength -= 2 * radius;
        glossiness *= glossiness;
    }

    return color / color.a;
}

/* ----- */

void CalculateReflection(vec3 viewPos, vec3 normal, in float roughness, inout vec4 color) {
    roughness = clamp(roughness + debugRoughness, 0, 1);

    vec3 rayDirection = reflect(normalize(viewPos), normalize(normal));

    if (dot(vec3(0, 0, -1), rayDirection) > 0) {
        vec2 hitPixel;
        bool hit = rayMarchView(viewPos, rayDirection, hitPixel);

        if (hit) {
            vec4 reflectionColor = coneTracing ? ConeTrace(hitPixel, hitPixel - toScreenSpace(viewPos), roughness) : texture(opaque, hitPixel);
            reflectionColor.a = 1;

            color = mix(mix(reflectionColor, color, roughness), color, CalculateFade(hitPixel.xy));
            //o = mix(o, vec4(1, 0, 0, 1), 0.1);
        }
    }
}