uniform int godraySamples;
uniform float godrayDensity;
uniform float godrayExposure;
uniform vec3 godrayColor;
uniform float godrayNoiseFactor;

uniform float fov;

float aspectRatio = viewport.x / viewport.y;

float circleDistanceSquared(vec2 uv) {
    vec2 vec = uv * 2 - 1;
    vec.x *= aspectRatio;
    return dot(vec, vec);
}

vec2 toNDC(vec2 screenSpace) {
    screenSpace = screenSpace * 2 - 1;
    screenSpace.x *= aspectRatio;

    return screenSpace;
}

/*
Mitchell. Volumetric Light Scattering as a Post-Process. 2007.
*/
vec3 CalculateGodrays(vec2 texCoord, vec2 lightPos) {
    if(lightPos == vec2(-1)) return vec3(0);

    float n = texture(noise, texCoord * 10).r;

    float rayAngle = tan(radians(fov / 2));
    float lightAngle = dot(normalize(vec3(toNDC(texCoord) * rayAngle, -1)), normalize(vec3(toNDC(lightPos) * rayAngle, -1)));
    float angularDecay = pow(lightAngle, 4);

    vec3 accumulator = vec3(0);
    vec2 delta = lightPos - texCoord;
    delta /= godraySamples;
    delta /= godrayDensity;

    //Offset starting position
    texCoord += delta * n * godrayNoiseFactor;

    for (int i = 0; i < godraySamples; i++) {
        vec4 color = textureLod(composite, texCoord, 0);

        //Transparent geometry (which lacks depth information) must have alpha < 1
        float transmittance = mix(1 - color.a, 1, step(1, color.a)); // = 1 for opaque geometry

        float attenuation = 1 - circleDistanceSquared(texCoord);
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

    return accumulator * godrayExposure * angularDecay;
}