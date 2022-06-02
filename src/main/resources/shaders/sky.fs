#version 410 core

out vec4 FragColor;

uniform vec3 color;
uniform vec2 viewPortResolution;
uniform vec3 direction;
uniform float dayPercentage;


void main()
{
    vec3 white = vec3(1,1,1);
    vec3 yellow = vec3(1.0,1.0, 0.0);
    vec3 orange = vec3(1.0,0.5,0.0);
    vec3 red = vec3(1.0,0.0, 0.0);
    vec3 black = vec3(0,0,0);
    vec2 viewPortCoord = gl_FragCoord.xy / viewPortResolution.xy;
    vec2 pixelCoord = -1 + 2 * viewPortCoord;
    vec3 col;

    float sundot = 0.2;

    vec3 redSky = vec3(0.8,0.8,0.6);

    col =  color;

    float num = clamp(-2 * pow(direction.y,3), -1.,0.);
    col = mix(white, color, -num );
    col = mix(orange, col, -1 + 2 * dayPercentage);
    col = mix(red, col, -1 + 2 * dayPercentage);

    col = mix(col, color, (pixelCoord.y + 1) /2 );
    col = mix(black, col, dayPercentage);

    FragColor= vec4(col,1.0);
}