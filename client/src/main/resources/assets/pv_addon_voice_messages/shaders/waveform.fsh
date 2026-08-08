#version 110

uniform sampler2D Sampler0;

varying vec2 texCoord0;
varying vec4 vertexColor;

const float TAU = 6.28318530718;
const float SEGMENTS = 8.0;
const float PARAMETERS = 9.0;

// how much of its own radius the button gains at full volume
const float CIRCLE_GROWTH = 0.5;
// how far out the outline sits in silence
const float IDLE_REACH = 0.35;

vec4 parameterAt(float index) {
    return texture2D(Sampler0, vec2((index + 0.5) / PARAMETERS, 0.5));
}

float radiusAt(float segment) {
    return parameterAt(mod(segment, SEGMENTS)).r;
}

void main() {
    vec4 scalars = parameterAt(SEGMENTS);

    float amplitude = scalars.r;
    float outerSize = floor(scalars.g * 255.0 + 0.5);
    float circleSize = floor(scalars.b * 255.0 + 0.5);
    float blobAlpha = scalars.a;

    float pixelSize = 1.0 / outerSize;
    float circleRadius = circleSize * 0.5 / outerSize;

    vec2 uv = (floor(texCoord0 / pixelSize) + 0.5) * pixelSize;

    vec2 offset = uv - vec2(0.5);
    float dist = length(offset);
    float angle = atan(offset.y, offset.x);

    float circle = circleRadius * (1.0 + amplitude * CIRCLE_GROWTH);
    float room = 0.5 - circle;

    float position = (angle / TAU + 0.5) * SEGMENTS;
    float segment = floor(position);
    float shape = mix(
        radiusAt(segment),
        radiusAt(segment + 1.0),
        smoothstep(0.0, 1.0, position - segment)
    );

    float reach = IDLE_REACH + (1.0 - IDLE_REACH) * amplitude;
    float blob = circle + room * reach * shape;

    if (dist <= circle) {
        gl_FragColor = vertexColor;
        return;
    }

    if (dist > blob) {
        discard;
    }

    float falloff = smoothstep(0.0, 1.0, 1.0 - (dist - circle) / max(blob - circle, 1e-4));

    gl_FragColor = vec4(vertexColor.rgb, vertexColor.a * blobAlpha * falloff);
}
