uniform int Layer;

void main() {
//  gl_Layer = Layer;
  for (int i = 0; i < 3; i++) {
    gl_Position = gl_PositionIn[i];
    EmitVertex();
  }
  EndPrimitive();
 
}
