uniform int godraySamples;
uniform float godrayDensity;
uniform float godrayDecay;
uniform float godrayExposure;

const  int MAX_SAMPLES = 200;
const int lod = 0;

/*
Mitchell. Volumetric Light Scattering as a Post-Process. 2007.
*/
float CalculateVLS(vec2 texCoord, vec2 lightPos) {

    float accumulator = 0;
    vec2 delta = texCoord - lightPos;
    delta *= 1.0 / godraySamples * godrayDensity;
    float d = 1;

    for (int i = 0; i < MAX_SAMPLES; i++) {
        if(i >= godraySamples) break;

        texCoord -= delta; //Move from sun towards pixel
        accumulator += step(1, textureLod(depth, texCoord, lod).x) * d; //Add 1 for sky or 0 for geometry
        d *= godrayDecay;
    }

    accumulator /= godraySamples; //Average

    return accumulator * godrayExposure;
}