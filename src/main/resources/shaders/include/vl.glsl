uniform int godraySamples;
uniform float godrayDensity;
uniform float godrayDecay;
uniform float godrayExposure;

const  int MAX_SAMPLES = 200;
const int lod = 0;

/*
Mitchell. Volumetric Light Scattering as a Post-Process. 2007.
*/
void CalculateVLS(vec2 texCoord, vec2 lightPos, inout vec3 color) {

    float accumulator = 0;
    vec2 deltaTexCoord = (texCoord - lightPos);
    deltaTexCoord *= 1.0 / godraySamples * godrayDensity;
    float d = 1;

    for (int i = 0; i < MAX_SAMPLES; i++) {
        if(i >= godraySamples) break;

        texCoord -= deltaTexCoord;
        float s = textureLod(depth, texCoord, lod).x < 1 ? -0.1 : 0.9;
        accumulator += s * d;
        d *= godrayDecay;
    }

    accumulator /= godraySamples;

    color += accumulator * godrayExposure;
}