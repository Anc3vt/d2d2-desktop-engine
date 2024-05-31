#version 330 core

layout(location = 0) in vec3 a_Position; // Входной атрибут с координатами вершин

void main() {
    gl_Position = vec4(a_Position, 1.0); // Присваиваем позицию вершины в гомогенных координатах
}