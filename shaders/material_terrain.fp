/* ID of Terrain material, so the lighting shader knows what material
 * this pixel is. */
const int TERRAIN_MATERIAL_ID = 4;

uniform sampler2D GrassTexture;
uniform sampler2D MossTexture;
uniform sampler2D RockTexture;

/* Material properties passed from the application. */
uniform vec3 DiffuseColor;

/* Fragment position and normal, and texcoord, from vertex shader. */
varying vec3 EyespacePosition;
varying vec3 EyespaceNormal;

varying vec3 WorldspacePosition;
varying vec3 WorldspaceNormal;

/* Encodes a normalized vector as a vec2. See Renderer.java for more info. */
vec2 encode(vec3 n)
{
	return normalize(n.xy) * sqrt(0.5 * n.z + 0.5);
}

void main()
{
	// Store diffuse color, position, encoded normal, material ID, and all other useful data in the g-buffer.
	vec3 eyespaceNormal = normalize(EyespaceNormal);
	vec3 worldspaceNormal = normalize(WorldspaceNormal);
	vec2 enc = encode(eyespaceNormal);
	float slope = 1.0 - worldspaceNormal.y;
	
	vec3 grassColor = texture2D(GrassTexture, fract(WorldspacePosition.xz/4.0)).rgb;
	vec3 mossColor = texture2D(MossTexture, fract(WorldspacePosition.xz/4.0)).rgb;
	vec3 rockColor = texture2D(RockTexture, fract(WorldspacePosition.xz/4.0)).rgb;
	
	vec3 diffuse_color = vec3(0.0);
	if (slope < 0.2) {
		float blendAmount = slope/0.2;
		diffuse_color = mix(grassColor, mossColor, blendAmount);
	} else if (slope > .7) {
		float blendAmount = (slope - 0.2) * (1.0 / 0.5);
		diffuse_color = mix(mossColor, rockColor, blendAmount);
	} else {
		diffuse_color = mossColor;
	}
  	gl_FragData[0] = vec4(diffuse_color, enc.x);
    gl_FragData[1] = vec4(EyespacePosition, enc.y);
  	gl_FragData[2] = vec4(TERRAIN_MATERIAL_ID);
	gl_FragData[3] = vec4(0.0, 0.0, 0.0, 0.0);
	
}
