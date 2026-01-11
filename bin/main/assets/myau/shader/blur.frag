#version 120

uniform sampler2D textureIn;
uniform float radius;

void main() {
    vec2 texelSize = 1.0 / vec2(textureSize(textureIn, 0));
    vec3 result = texture2D(textureIn, gl_TexCoord[0].xy).rgb;
    
    for (float i = -radius; i <= radius; i++) {
        for (float j = -radius; j <= radius; j++) {
            vec2 offset = vec2(i, j) * texelSize;
            result += texture2D(textureIn, gl_TexCoord[0].xy + offset).rgb;
        }
    }
    
    result /= (2.0 * radius + 1.0) * (2.0 * radius + 1.0);
    gl_FragColor = vec4(result, 1.0);
}