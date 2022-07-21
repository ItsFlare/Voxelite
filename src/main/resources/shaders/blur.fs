#version 430

uniform sampler2D sampler;
uniform vec2 direction;

out vec4 color;

const float offset[3] = float[3](0.0, 1.3846153846, 3.2307692308);
const float weight[3] = float[3](0.2270270270, 0.3162162162, 0.0702702703);

void main() {
    vec2 sizeReciprocal = 1.0 / vec2(textureSize(sampler, 0));
    color = texture2D(sampler, gl_FragCoord.xy * sizeReciprocal) * weight[0];

    for (int i = 1; i < 3; i++) {
        color += texture2D(sampler, (gl_FragCoord.xy + direction * offset[i]) * sizeReciprocal) * weight[i];
        color += texture2D(sampler, (gl_FragCoord.xy - direction * offset[i]) * sizeReciprocal) * weight[i];
    }

}