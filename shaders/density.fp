uniform vec3 LowerCorner;
uniform int Layer;
uniform float NumVoxels;
uniform float BlockSize;

uniform sampler3D PerlinNoise;

void main() {
	float px = gl_FragCoord.x;
	float py = gl_FragCoord.y; 
	
	if (Layer == 0) {
		gl_FragColor.rgb = vec3(0.6);
		return;
	}
	vec3 worldCoord = vec3((px - 0.5)/(NumVoxels), ((py - 0.5)/(NumVoxels)), (float(Layer) - 1.0)/(NumVoxels));
	worldCoord = worldCoord * BlockSize + LowerCorner; 
	//gl_FragColor.rgb = vec3(worldCoord.y + sin(worldCoord.z) + sin(worldCoord.x));
	vec3 worldCoordFract = vec3(fract(worldCoord.x), fract(worldCoord.y), fract(worldCoord.z)); 
	float noise = texture3D(PerlinNoise, worldCoordFract).x;
	gl_FragColor.rgb = vec3(worldCoord.y + 5.0 * noise);
    
}
