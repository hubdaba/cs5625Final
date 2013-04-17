uniform int Layer;

void main() {
  gl_Layer = Layer;
  for (int i = 0; i < gl_VerticesIn; i++) {
    gl_Position = gl_PositionIn[i];
    EmitVertex();
  }
  EndPrimitive();
 
}
