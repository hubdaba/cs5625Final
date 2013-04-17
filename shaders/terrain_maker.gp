#version 120
#extension GL_EXT_gpu_shader4 : enable
#extension GL_EXT_geometry_shader4 : enable


uniform sampler2D TriTable;
uniform sampler3D DensityFunction;
uniform vec3 LowerCorner;

int triTableValue(int i, int j) {
	return int(texelFetch2D(TriTable, ivec2(j, i), 0).a);
}

vec3 cubePos (int i) {
    
    if (i == 0) return gl_PositionIn[0].xyz;
    if (i == 1) return gl_PositionIn[0].xyz + vec3(0.0, 0.0, 1.0);
    if (i == 2) return gl_PositionIn[0].xyz + vec3(1.0, 0.0, 1.0);
    if (i == 3) return gl_PositionIn[0].xyz + vec3(1.0, 0.0, 0.0);
    if (i == 4) return gl_PositionIn[0].xyz + vec3(0.0, 1.0, 0.0);
    if (i == 5) return gl_PositionIn[0].xyz + vec3(0.0, 1.0, 1.0);
    if (i == 6) return gl_PositionIn[0].xyz + vec3(1.0, 1.0, 1.0);
    if (i == 7) return gl_PositionIn[0].xyz + vec3(1.0, 1.0, 0.0);
}

float cubeVal(int i) {
  return texelFetch3D(DensityFunction, ivec3(
  					int(cubePos(i).x),
  					int(cubePos(i).y), 
  					int(cubePos(i).z) + 1), 0).x;
}

vec3 vertexInterp(vec3 v0, float l0, vec3 v1, float l1) {
  return mix(v0/32.0 + LowerCorner, v1/32.0 + LowerCorner, -l0 / (l1 - l0));
  //return mix(v0/32.0 + LowerCorner, v1/32.0 + LowerCorner, 0.5);
}

void main() {

  int cubeindex = 0;

  if (cubeVal(0) <= 0.0) cubeindex = cubeindex | 1;
  if (cubeVal(1) <= 0.0) cubeindex = cubeindex | 2;
  if (cubeVal(2) <= 0.0) cubeindex = cubeindex | 4;
  if (cubeVal(3) <= 0.0) cubeindex = cubeindex | 8;
  if (cubeVal(4) <= 0.0) cubeindex = cubeindex | 16;
  if (cubeVal(5) <= 0.0) cubeindex = cubeindex | 32;
  if (cubeVal(6) <= 0.0) cubeindex = cubeindex | 64;
  if (cubeVal(7) <= 0.0) cubeindex = cubeindex | 128;
  
  
  
  
  if (cubeindex == 0  || cubeindex == 255) {
	return;
  } 
  
  vec3 vertlist[12];
 
  
 
    vertlist[0] = vertexInterp(cubePos(0), cubeVal(0), cubePos(1), cubeVal(1));
    vertlist[1] = vertexInterp(cubePos(1), cubeVal(1), cubePos(2), cubeVal(2));
    vertlist[2] = vertexInterp(cubePos(2), cubeVal(2), cubePos(3), cubeVal(3));
    vertlist[3] = vertexInterp(cubePos(3), cubeVal(3), cubePos(0), cubeVal(0));
    vertlist[4] = vertexInterp(cubePos(4), cubeVal(4), cubePos(5), cubeVal(5));
    vertlist[5] = vertexInterp(cubePos(5), cubeVal(5), cubePos(6), cubeVal(6));
    vertlist[6] = vertexInterp(cubePos(6), cubeVal(6), cubePos(7), cubeVal(7));
    vertlist[7] = vertexInterp(cubePos(7), cubeVal(7), cubePos(4), cubeVal(4));
    vertlist[8] = vertexInterp(cubePos(0), cubeVal(0), cubePos(4), cubeVal(4));
    vertlist[9] = vertexInterp(cubePos(1), cubeVal(1), cubePos(5), cubeVal(5));
    vertlist[10] = vertexInterp(cubePos(2), cubeVal(2), cubePos(6), cubeVal(6));
    vertlist[11] = vertexInterp(cubePos(3), cubeVal(3), cubePos(7), cubeVal(7));
  
  int i = 0;
  
   while (true) {
  	
    if (triTableValue(cubeindex, i) >= 0 && triTableValue(cubeindex, i) < 12) {
      gl_Position = vec4(vertlist[triTableValue(cubeindex, i)], 1.0);
      EmitVertex();
      gl_Position = vec4(vertlist[triTableValue(cubeindex, i+1)], 1.0);
      EmitVertex();
      gl_Position = vec4(vertlist[triTableValue(cubeindex, i+2)], 1.0);
      EmitVertex();
      EndPrimitive();
    } else {
      break;
    }
    i = i + 3;
  }
 
 
}
