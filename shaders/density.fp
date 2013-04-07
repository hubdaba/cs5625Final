uniform vec3 LowerCorner;
uniform float VoxelDim;
uniform int Layer;

void main() {
	float px = gl_FragCoord.x;
	float py = gl_FragCoord.y; 
	
	vec3 worldCoord = vec3(px * VoxelDim, py * VoxelDim, float(Layer) * VoxelDim); 
	
   // gl_FragColor.rgb = vec3(-worldCoord.y);
  gl_FragColor.rgb = vec3(1.0, 0.0, 0.0);
}
