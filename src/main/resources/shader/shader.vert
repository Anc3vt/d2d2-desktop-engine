#version 110

uniform vec2 dxy;

void main() {
    // gl_ModelViewMatrix
    // gl_ProjectionMatrix
    // gl_ModelViewProjectionMatrix

    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;

    gl_Position.xy += dxy;
}