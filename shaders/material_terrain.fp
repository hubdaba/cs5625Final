/* ID of Terrain material, so the lighting shader knows what material
 * this pixel is. */
const int TERRAIN_MATERIAL_ID = 4;

/* Material properties passed from the application. */
uniform vec3 DiffuseColor;

/* Fragment position and normal, and texcoord, from vertex shader. */
varying vec3 EyespacePosition;
varying vec3 EyespaceNormal;

/* Encodes a normalized vector as a vec2. See Renderer.java for more info. */
vec2 encode(vec3 n)
{
	return normalize(n.xy) * sqrt(0.5 * n.z + 0.5);
}

void main()
{
	// Store diffuse color, position, encoded normal, material ID, and all other useful data in the g-buffer.
	vec2 enc = encode(normalize(EyespaceNormal));
  	gl_FragData[0] = vec4(DiffuseColor, enc.x);
    gl_FragData[1] = vec4(EyespacePosition, enc.y);
  	gl_FragData[2] = vec4(TERRAIN_MATERIAL_ID);
	gl_FragData[3] = vec4(0.0, 0.0, 0.0, 0.0);
	
}
