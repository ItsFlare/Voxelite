#version 430
#include "include\ssr.glsl"

#ifndef DEFERRED
    #include "include\deferred.glsl"
#endif

out vec4 FragColor;

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

    FragColor = o;
}