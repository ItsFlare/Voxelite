#version 410 core
out vec4 FragColor;

in vec3 uv;

uniform samplerCube skybox;
uniform float alpha;

void main()
{
    vec4 texColor = texture(skybox, uv);
    texColor.a = alpha;
    if (texColor.xyz == vec3(0)) {
        texColor.a = 0;
    }
    FragColor = texColor;
}