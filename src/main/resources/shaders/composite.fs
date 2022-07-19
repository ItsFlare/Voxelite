#version 430
#include "include\deferred.glsl"
#include "include\ssr.glsl"
#include "include\csm.glsl"

out vec4 FragColor;

uniform vec3 camera;
uniform float fov;
uniform mat4 viewToLight;

uniform sampler2D normal;
uniform sampler2D mer;

vec2 pixel = gl_FragCoord.xy / viewport;

void main() {
    //Copy depth into default framebuffer
    gl_FragDepth = texture(depth, pixel).x;

    vec4 o = texture(opaque, pixel);
    o.a = 1;

    vec3 n = texture(normal, pixel).xyz;

    //No geometry (background)
    if (n == vec3(0)) {
        FragColor = o;
        return;
    }

    if(reflections) {
        float roughness = texture(mer, pixel).b;
        CalculateReflection(toViewSpace(pixel), n, roughness, o);
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

    FragColor = o;
    FragColor = mix(FragColor, vec4(vec3(accumulator / iter), 1), 0.75);

    //if(raycast(vec2(0) * viewport, vec2(1) * viewport, viewport)) FragColor = vec4(0, 1, 0, 1);
    //if(raycast(vec2(0, 1) * viewport, vec2(1, 0) * viewport, viewport)) FragColor = vec4(0, 1, 0, 1);

    //if(raycast(vec2(0, 0.5) * viewport, vec2(1000, viewport.y / 2), viewport)) FragColor = vec4(1, 1, 0, 1);
    //if(raycast(vec2(0.5, 0) * viewport, vec2(0.5, 1) * viewport, viewport)) FragColor = vec4(1, 0, 1, 1);

    //DDA line debug
//    FragColor = mix(FragColor, vec4(SampleDepth(pixel) / 25), 0.99999);
//    FragColor.a = 1;
//    vec3 hit;
//    bool b = raymarch(vec3(1, 0.5 * viewport.y, 0), vec3(1 * viewport.x, 0.5 * viewport.y, -25), 2, 10000, hit);
//    if(b) {
//        if(length(ivec2(hit.xy) - ivec2(gl_FragCoord.xy)) < 10) FragColor = vec4(1, 0, 0, 1);
//    } else {
//        FragColor = vec4(0);
//    }
}