uniform int godraySamples;
uniform float godrayDensity;
uniform float godrayExposure;
uniform float godrayNoiseFactor;

uniform vec3 lightView;
uniform vec2 lightScreen;

uniform float fov;

uniform vec3 sunColor = vec3(1, 0.4, 0);
uniform vec3 moonColor = vec3(0.25, 0.25, 0.3);

#include "include\util.glsl"

float aspectRatio = viewport.x / viewport.y;

float rectangleDistanceSquared(vec2 uv) {
    vec2 vec = uv * 2 - 1;
    float d = max(vec.x, vec.y);
    return d * d;
}

vec2 toNDC(vec2 screenSpace) {
    screenSpace = screenSpace * 2 - 1;
    screenSpace.x *= aspectRatio;

    return screenSpace;
}

/*
Mitchell. Volumetric Light Scattering as a Post-Process. 2007.
*/
vec3 CalculateGodrays(vec2 texCoord) {

    float n = texture(noise, texCoord * 10).r;

    float rayAngle = tan(radians(fov / 2));
    vec3 ray = normalize(vec3(toNDC(texCoord) * rayAngle, -1));

    float lightRayAngle = dot(ray, lightView);
    float lightViewAngle = dot(vec3(0, 0, -1), lightView);

    bool moon = lightViewAngle < 0;
    vec3 godrayColor = moon ? moonColor : sunColor;
    if(moon) lightRayAngle = -lightRayAngle;

    //Step ensures godrays are invisible if ray is facing away from nearest light
    //Left side of min ensures godrays are invisible if light is straight above, as the screen space position would be invalid
    float angularDecay = step(0, lightRayAngle) * min(abs(lightViewAngle), pow(lightRayAngle, 2) * 0.25 + pow(lightRayAngle, 8) * 0.75);

    vec2 delta = clampRay(texCoord, lightScreen);
    delta /= godraySamples * godrayDensity;

    //Offset starting position
    texCoord += clamp(delta * n * godrayNoiseFactor, -1, 1);

    vec3 accumulator = vec3(0);

    // ~5 ms / 100 samples @ 1440p (GTX 1660S)
    for(int i = 0; i < godraySamples; i++) {
        vec4 color = textureLod(composite, texCoord, 0);

        //Transparent geometry (which lacks depth information) must have alpha < 1
        float transmittance = mix(1 - color.a, 1, step(1, color.a)); // = 1 for opaque geometry

        float attenuation = clamp(1 - rectangleDistanceSquared(texCoord), 0, 1);
        float emission = step(1, texture(depth, texCoord).x) * attenuation * transmittance;

        //Because this happens after lighting, limit correction to account for very dim colors
        float correction = min(1.0 / max(color.r, max(color.g, color.b)), 10);
        vec3 spectrum = color.rgb * correction;

        //Energy if the current fragment is translucent
        vec3 t = emission * spectrum;

        //Energy if the current fragment is opaque
        vec3 o = vec3(emission);

        //Assumes opaque pixels have an alpha of >=1
        accumulator += godrayColor * mix(t, o, step(1, color.a));
        texCoord += delta;
    }

    accumulator /= godraySamples; //Average
    accumulator *= godrayExposure * angularDecay;
    return accumulator;
}