#version 120

uniform float time;
uniform vec2 resolution;

float random(vec2 st) {
    return fract(sin(dot(st.xy, vec2(12.9898, 78.233))) * 43758.5453123);
}

float noise(vec2 st) {
    vec2 i = floor(st);
    vec2 f = fract(st);
    float a = random(i);
    float b = random(i + vec2(1.0, 0.0));
    float c = random(i + vec2(0.0, 1.0));
    float d = random(i + vec2(1.0, 1.0));
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
}

float fbm(vec2 st) {
    float v = 0.0;
    float a = 0.5;
    vec2 shift = vec2(100.0);
    mat2 rot = mat2(cos(0.5), sin(0.5), -sin(0.5), cos(0.50));
    for (int i = 0; i < 5; ++i) {
        v += a * noise(st);
        st = rot * st * 2.0 + shift;
        a *= 0.5;
    }
    return v;
}

void main() {
    vec2 st = gl_FragCoord.xy / resolution.xy * 3.0;
    st.x *= resolution.x / resolution.y;

    vec2 q = vec2(0.0);
    q.x = fbm(st + 0.00 * time);
    q.y = fbm(st + vec2(1.0));

    vec2 r = vec2(0.0);
    r.x = fbm(st + 1.0 * q + vec2(1.7, 9.2) + 0.15 * time);
    r.y = fbm(st + 1.0 * q + vec2(8.3, 2.8) + 0.126 * time);

    float f = fbm(st + r);

    vec3 color = mix(vec3(0.101961, 0.619608, 0.666667),
                     vec3(0.666667, 0.666667, 0.498039),
                     clamp((f * f) * 4.0, 0.0, 1.0));

    color = mix(color,
                vec3(0.0, 0.0, 0.164706),
                clamp(length(q), 0.0, 1.0));

    color = mix(color,
                vec3(0.666667, 1.0, 1.0),
                clamp(length(r.x), 0.0, 1.0));

    // Darken the background
    color *= 0.6;
    color = pow(color, vec3(1.2));

    gl_FragColor = vec4(color, 1.0);
}