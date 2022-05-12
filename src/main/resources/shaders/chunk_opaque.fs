#version 410
in vec2 Tex;
in vec3 Pos;
in vec3 Normal;

out vec4 FragColor;


uniform sampler2D atlas;
uniform vec3 camera;

uniform float ambientStrength;
uniform float diffuseStrength;
uniform float specularStrength;

uniform struct Light {
    vec3 direction;
    vec3 color;
} light;

const int PHONG_EXPONENT = 128;

vec3 DirectionalLight(Light light, vec3 normal, vec3 viewDirection)
{
    vec3 color = light.color.xyz;
    vec3 lightDirection = normalize(-light.direction);

    //diffuse
    float diff = max(dot(normal, lightDirection), 0.0);

    //specular (blinn)
    vec3 halfwayDirection = normalize(lightDirection + viewDirection);
    float spec = pow(max(dot(normal, halfwayDirection), 0.0), PHONG_EXPONENT);

    vec3 ambient  = color * ambientStrength;
    vec3 diffuse  = color * diffuseStrength * diff;
    vec3 specular = color * specularStrength * spec;
    return (ambient + diffuse + specular);
}

void main() {
    FragColor = vec4(DirectionalLight(light, Normal, normalize(camera - Pos)), 1) * texture(atlas, Tex);
}