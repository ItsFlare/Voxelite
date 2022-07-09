uniform bool fogSet;
uniform int fogRange;
uniform vec3 fogColor;

float getFogFactor(vec3 ViewPosition) {
    float distance = length(ViewPosition);

    float density = 0.015;
    float start = fogRange - 10;
    float end = fogRange + 25;

    return clamp((distance - start) / (end - start), 0, 1);
}