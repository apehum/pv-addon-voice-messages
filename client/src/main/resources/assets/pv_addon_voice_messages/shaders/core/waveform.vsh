//? if >=26.3 {
//#version 330
//#extension GL_ARB_separate_shader_objects : require
//
//#include <minecraft:dynamictransforms.glsl>
//#include <minecraft:projection.glsl>
//
//layout(std140) uniform WaveformParams {
//    vec4 RadiiA;
//    vec4 RadiiB;
//    vec4 Scalars;
//    vec4 Rect;
//};
//
//layout(location = 0) in vec3 Position;
//layout(location = 1) in vec4 Color;
//
//layout(location = 0) out vec2 texCoord0;
//layout(location = 1) out vec4 vertexColor;
//?} else if >=1.21.6 {
//#version 150
//
//#moj_import <minecraft:dynamictransforms.glsl>
//#moj_import <minecraft:projection.glsl>
//
//layout(std140) uniform WaveformParams {
//    vec4 RadiiA;
//    vec4 RadiiB;
//    vec4 Scalars;
//    vec4 Rect;
//};
//
//in vec3 Position;
//in vec4 Color;
//
//out vec2 texCoord0;
//out vec4 vertexColor;
//?} else {
#version 150

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

uniform vec4 Rect;

in vec3 Position;
in vec4 Color;

out vec2 texCoord0;
out vec4 vertexColor;
//?}

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    texCoord0 = (Position.xy - Rect.xy) / Rect.zw;
    vertexColor = Color;
}
