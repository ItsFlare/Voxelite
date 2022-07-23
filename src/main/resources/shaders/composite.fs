#version 430
#include "include\deferred.glsl"
#include "include\ssr.glsl"
#include "include\csm.glsl"

layout(location = 0) out vec4 color;

uniform vec3 camera;
uniform float fov;
uniform mat4 viewToLight;

uniform sampler2D normal;
uniform sampler2D mer;

vec2 pixel = gl_FragCoord.xy / viewport;

void main() {
    vec4 o = texture(opaque, pixel);
    o.a = 1;
    color = o;

    vec3 n = normalize(texture(normal, pixel).xyz);
    vec3 MER = texture(mer, pixel).rgb;

    //If pixel has world geometry
    if (n != vec3(0)) {
        if(reflections) {
            float roughness = MER.b;
            CalculateReflection(toViewSpace(pixel), n, roughness, color);
        }

        vec2 viewPortCoord = gl_FragCoord.xy / viewport;
        vec2 clipCoord = -1 + 2 * viewPortCoord;
        clipCoord.x *= aspectRatio;

        float iter = 3;
        float stride = 1;
        vec3 rayDirection = vec3(clipCoord.xy * tan(radians(fov / 2)), -1);
        float accumulator = 0;
        vec3 debugColor;
        vec3 ViewSpacePos = vec3(0);
        vec3 direction = normalize(toViewSpace(pixel)) * stride;
        float depth = toViewSpace(pixel).z;

        for(int i = 0; i < iter && ViewSpacePos.z > depth; i++) {
            ViewSpacePos += direction;
            vec3 LightSpacePos = (viewToLight * vec4(ViewSpacePos, 1)).xyz;
            accumulator += ShadowCalculation(LightSpacePos, vec3(0, 1, 0), debugColor);
        }

        color = mix(color, vec4(vec3(accumulator / iter), 1), 0.01);
    }
}