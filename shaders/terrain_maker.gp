#version 120
#extension GL_EXT_gpu_shader4 : enable
#extension GL_EXT_geometry_shader4 : enable

uniform float NumVoxels;
uniform float BlockSize;

uniform sampler2D TriTable;
uniform sampler3D DensityFunction;
uniform vec3 LowerCorner;

int triTableValue(int i, int j) {
	return int(round(texelFetch2D(TriTable, ivec2(j, i), 0).a));
}

vec3 cubePos (int i) {
    
    if (i == 0) return gl_PositionIn[0].xyz;
    if (i == 1) return gl_PositionIn[0].xyz + vec3(1.0, 0.0, 0.0);
    if (i == 2) return gl_PositionIn[0].xyz + vec3(1.0, 1.0, 0.0);
    if (i == 3) return gl_PositionIn[0].xyz + vec3(0.0, 1.0, 0.0);
    if (i == 4) return gl_PositionIn[0].xyz + vec3(0.0, 0.0, 1.0);
    if (i == 5) return gl_PositionIn[0].xyz + vec3(1.0, 0.0, 1.0);
    if (i == 6) return gl_PositionIn[0].xyz + vec3(1.0, 1.0, 1.0);
    if (i == 7) return gl_PositionIn[0].xyz + vec3(0.0, 1.0, 1.0);
}

float cubeVal(int i) {
  return texelFetch3D(DensityFunction, ivec3(
  					round(cubePos(i).x),
  					round(cubePos(i).y), 
  					round(cubePos(i).z) + 1), 0).x;
 }

vec3 vertexInterp(vec3 v0, float l0, vec3 v1, float l1) {
  return mix(BlockSize * v0/NumVoxels + LowerCorner, BlockSize * v1/NumVoxels + LowerCorner, (-l0) / (l1 - l0));
 }

void main() {

  int cubeindex = 0;
  
  float cubeVal0 = cubeVal(0);
  float cubeVal1 = cubeVal(1);
  float cubeVal2 = cubeVal(2);
  float cubeVal3 = cubeVal(3);
  float cubeVal4 = cubeVal(4);
  float cubeVal5 = cubeVal(5);
  float cubeVal6 = cubeVal(6);
  float cubeVal7 = cubeVal(7);

  cubeindex += int(cubeVal0 <= 0) * 1;
  cubeindex += int(cubeVal1 <= 0) * 2;
  cubeindex += int(cubeVal2 <= 0) * 4;
  cubeindex += int(cubeVal3 <= 0) * 8;
  cubeindex += int(cubeVal4 <= 0) * 16;
  cubeindex += int(cubeVal5 <= 0) * 32;
  cubeindex += int(cubeVal6 <= 0) * 64;
  cubeindex += int(cubeVal7 <= 0) * 128;
  
  
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
