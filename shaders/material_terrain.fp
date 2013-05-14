/* ID of Terrain material, so the lighting shader knows what material
 * this pixel is. */
const int TERRAIN_MATERIAL_ID = 4;


uniform int NumExplosions;
uniform vec3 ExplosionPositions[5];

uniform float ExplosionRadius;


uniform sampler2D GrassTexture;
uniform sampler2D MossTexture;
uniform sampler2D RockTexture;
uniform sampler2D AshTexture;

/* Material properties passed from the application. */
uniform vec3 DiffuseColor;

/* Fragment position and normal, and texcoord, from vertex shader. */
varying vec3 EyespacePosition;
varying vec3 EyespaceNormal;

varying vec3 WorldspacePosition;
varying vec3 WorldspaceNormal;

float tex_scale = 1.0/4.0;

/* Encodes a normalized vector as a vec2. See Renderer.java for more info. */
vec2 encode(vec3 n)
{
	return normalize(n.xy) * sqrt(0.5 * n.z + 0.5);
}

void main()
{   
	vec3 grassColor, mossColor, rockColor;
	
	// Store diffuse color, position, encoded normal, material ID, and all other useful data in the g-buffer.
	vec3 eyespaceNormal = normalize(EyespaceNormal);
	vec3 worldspaceNormal = normalize(WorldspaceNormal);
	vec2 enc = encode(eyespaceNormal);
	float slope = 1.0 - worldspaceNormal.y;
	
	// Triplanar texturing, from GPUGems
	// Determine the blend weights for the 3 planar projections.  
	// N_orig is the vertex-interpolated normal vector.  
	vec3 blend_weights = abs( worldspaceNormal.xyz );   // Tighten up the blending zone:  
	blend_weights = (blend_weights - 0.2) * 7.0;  
	blend_weights = max(blend_weights, 0.0);      // Force weights to sum to 1.0 (very important!)  
	blend_weights /= blend_weights.x + blend_weights.y + blend_weights.z ;   
	
	// Now determine a color value and bump vector for each of the 3  
	// projections, blend them, and store blended results in these two  
	// vectors:  
	//float3 blended_bump_vec;  
	{
		// Compute the UV coords for each of the 3 planar projections.  
		// tex_scale (default ~ 1.0) determines how big the textures appear.  
		vec2 coord1 = fract(WorldspacePosition.yz * tex_scale);  
		vec2 coord2 = fract(WorldspacePosition.zx * tex_scale);  
		vec2 coord3 = fract(WorldspacePosition.xy * tex_scale);  
		// This is where you would apply conditional displacement mapping.  
		//if (blend_weights.x > 0) coord1 = . . .  
		//if (blend_weights.y > 0) coord2 = . . .  
		//if (blend_weights.z > 0) coord3 = . . .  
		// Sample color maps for each projection, at those UV coords, and blend them
		grassColor = 	texture2D(GrassTexture, coord1).rgb * blend_weights.x + 
						texture2D(GrassTexture, coord2).rgb * blend_weights.y +
						texture2D(GrassTexture, coord3).rgb * blend_weights.z;
		mossColor = 	texture2D(MossTexture, coord1).rgb * blend_weights.x + 
						texture2D(MossTexture, coord2).rgb * blend_weights.y +
						texture2D(MossTexture, coord3).rgb * blend_weights.z;
		rockColor = 	texture2D(RockTexture, coord1).rgb * blend_weights.x + 
						texture2D(RockTexture, coord2).rgb * blend_weights.y +
						texture2D(RockTexture, coord3).rgb * blend_weights.z;
		// Sample bump maps too, and generate bump vectors.  
		// (Note: this uses an oversimplified tangent basis.)  
		//float2 bumpFetch1 = bumpTex1.Sample(coord1).xy - 0.5;  
		//float2 bumpFetch2 = bumpTex2.Sample(coord2).xy - 0.5;  
		//float2 bumpFetch3 = bumpTex3.Sample(coord3).xy - 0.5;  
		//float3 bump1 = float3(0, bumpFetch1.x, bumpFetch1.y);  
		//float3 bump2 = float3(bumpFetch2.y, 0, bumpFetch2.x);  
		//float3 bump3 = float3(bumpFetch3.x, bumpFetch3.y, 0);  
	}
	
	vec3 diffuse_color = vec3(0.0);
	if (slope < 0.2) {
		float blendAmount = slope/0.2;
		diffuse_color = mix(grassColor, mossColor, blendAmount);
	} else if (slope > .5) {
		float blendAmount = (slope - 0.5) * (1.0 / 0.5);
		diffuse_color = mix(mossColor, rockColor, blendAmount);
	} else {
		diffuse_color = mossColor;
	}
	for (int i = 0; i < NumExplosions; i++) {
		vec3 explosionPosition = ExplosionPositions[i];
		float distanceToExplosion = distance(explosionPosition, WorldspacePosition);
		if (distanceToExplosion < ExplosionRadius) {
			diffuse_color = vec3(0.0, 0.0, 0.0);
			break;
		}
	}
  	gl_FragData[0] = vec4(diffuse_color, enc.x);
    gl_FragData[1] = vec4(EyespacePosition, enc.y);
  	gl_FragData[2] = vec4(TERRAIN_MATERIAL_ID);
	gl_FragData[3] = vec4(0.0, 0.0, 0.0, 0.0);
	
}
