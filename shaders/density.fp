uniform vec3 LowerCorner;
uniform int Layer;


void main() {
	float px = gl_FragCoord.x;
	float py = gl_FragCoord.y; 
	
	if (Layer == 0) {
		gl_FragColor.rgb = vec3(1.0);
		return;
	}
	vec3 worldCoord = vec3((px - 0.5)/33., ((py - 0.5)/33.), (float(Layer) - 1.0)/ 33.0);
	worldCoord = worldCoord + LowerCorner; 
	gl_FragColor.rgb = vec3(worldCoord.y + 0.5 * sin(worldCoord.z) + 0.5 * sin(worldCoord.x));
	
    
}
