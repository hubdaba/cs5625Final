/**
 * ubershader.fp
 * 
 * Fragment shader for the "ubershader" which lights the contents of the gbuffer. This shader
 * samples from the gbuffer and then computes lighting depending on the material type of this 
 * fragment.
 * 
 * Written for Cornell CS 5625 (Interactive Computer Graphics).
 * Copyright (c) 2012, Computer Science Department, Cornell University.
 * 
 * @author Asher Dunn (ad488), Ivaylo Boyadzhiev (iib2)
 * @date 2012-03-24
 */

/* Copy the IDs of any new materials here. */
const int UNSHADED_MATERIAL_ID = 1;
const int LAMBERTIAN_MATERIAL_ID = 2;
const int BLINNPHONG_MATERIAL_ID = 3;
const int TERRAIN_MATERIAL_ID = 4;

/* Some constant maximum number of lights which GLSL and Java have to agree on. */
#define MAX_LIGHTS 40

/* Samplers for each texture of the GBuffer. */
uniform sampler2DRect DiffuseBuffer;
uniform sampler2DRect PositionBuffer;
uniform sampler2DRect MaterialParams1Buffer;
uniform sampler2DRect MaterialParams2Buffer;
uniform sampler2DRect SilhouetteBuffer;
uniform sampler2DRect SSAOBuffer;
uniform sampler2DRect ParticleBuffer;

uniform bool EnableToonShading;
uniform bool EnableSSAO;

/* Uniform specifying the sky (background) color. */
uniform vec3 SkyColor;

/* Uniforms describing the lights. */
uniform int NumLights;
uniform vec3 LightPositions[MAX_LIGHTS];
uniform vec3 LightAttenuations[MAX_LIGHTS];
uniform vec3 LightColors[MAX_LIGHTS];

#define MAX_SHADOW_MAPS 2

#define DEFAULT_SHADOW_MAP 0
#define PCF_SHADOW_MAP 1
#define PCSS SHADOW_MAP 2

/* Shadow map parameters */
uniform int NumShadowMaps;
uniform int ShadowMode;
uniform sampler2D ShadowMaps[MAX_SHADOW_MAPS];
uniform float bias[MAX_SHADOW_MAPS];
uniform float ShadowSampleWidth[MAX_SHADOW_MAPS];
uniform float ShadowMapWidth[MAX_SHADOW_MAPS];
uniform float ShadowMapHeight[MAX_SHADOW_MAPS];
uniform float LightWidth[MAX_SHADOW_MAPS];

uniform vec3 ShadowCamPosition[MAX_SHADOW_MAPS];
uniform vec3 ShadowLightAttenuations[MAX_SHADOW_MAPS];
uniform vec3 ShadowLightColors[MAX_SHADOW_MAPS];

/* Pass the shadow camera Projection * View matrix to help transform points, as well the Camera inverse-view Matrix */
uniform mat4 LightMatrix[MAX_SHADOW_MAPS];
uniform mat4 InverseViewMatrix[MAX_SHADOW_MAPS];


/* Decodes a vec2 into a normalized vector See Renderer.java for more info. */
vec3 decode(vec2 v)
{
	vec3 n;
	n.z = 2.0 * dot(v.xy, v.xy) - 1.0;
	n.xy = normalize(v.xy) * sqrt(1.0 - n.z*n.z);
	return n;
}


// Converts the depth buffer value to a linear value
float DepthToLinear(float value)
{
	float near = 0.1;
	float far = 100.0;
	return (2.0 * near) / (far + near - value * (far - near));
}

/** Returns how shadowed this coordinate is. 0 = shadowed, 1 = not shadowed, anywhere in between
 */
vec4 getShadowVal(vec4 shadowCoord, vec2 offset, int shadowSource) 
{
	// If your fragment is behind the camera, assume no occlusion
	if (shadowCoord.z >= 0.0) {  return 1.0;  }
	
	float texDepth = texture2D(ShadowMaps[shadowSource], shadowCoord.xy + offset).z;
	
	// If the shadowmap only sees the background, assume unoccluded
	if (texDepth == 0.0) {  return 1.0;  }
	
	// Otherwise, we have a valid occluder to test.  Test it!	
	if (shadowCoord.z <= texDepth + bias[shadowSource]) {
		return 1.0;
	}
	return 0.0;
}

/** Calculates regular shadow map algorithm shadow strength
 *
 * @param shadowCoord The location of the position in the light projection space
 */
 vec4 getDefaultShadowMapVal(vec4 shadowCoord, int shadowSource)
 {
	return getShadowVal(shadowCoord, vec2(0.0), shadowSource);
 }
 
 /** Calculates PCF shadow map algorithm shadow strength
 *
 * @param shadowCoord The location of the position in the light projection space
 */
float getPCFShadowMapVal(vec4 shadowCoord, int shadowSource)
 {
 	float shade = 0.0;
 	for (float i=-ShadowSampleWidth[shadowSource]/ShadowMapWidth[shadowSource]; i<=ShadowSampleWidth[shadowSource]/ShadowMapWidth[shadowSource]; i+=1.0/ShadowMapWidth[shadowSource]) {
 		for (float j=-ShadowSampleWidth[shadowSource]/ShadowMapHeight[shadowSource]; j<=ShadowSampleWidth[shadowSource]/ShadowMapHeight[shadowSource]; j+=1.0/ShadowMapHeight[shadowSource]) {
 			shade += getShadowVal(shadowCoord, vec2(i,j), shadowSource);
 		}
 	}
 	float width = 2.0 * ShadowSampleWidth[shadowSource] + 1.0;
 	return shade / (width * width);
 }
 
 /** Calculates PCSS shadow map algorithm shadow strength
 *
 * @param shadowCoord The location of the position in the light projection space
 */
 float getPCSSShadowMapVal(vec4 shadowCoord, int shadowSource)
 {
 	
 	// Average visible depth values
 	float avgOccDepth = 0.0;
 	float numAvgd = 0.0;
 	float fragDepth = DepthToLinear(shadowCoord.z);
 	float texDepth;
 	// Use LigthWidth as a RADIUS of our square range to average over
 	// Note: physically it may be meant to be the side length, but it is kind of arbitrary
 	for (float i=-LightWidth[shadowSource]/ShadowMapWidth[shadowSource]; i<=LightWidth[shadowSource]/ShadowMapWidth[shadowSource]; i+=1.0/ShadowMapWidth[shadowSource]) {
 		for (float j=-LightWidth[shadowSource]/ShadowMapHeight[shadowSource]; j<=LightWidth[shadowSource]/ShadowMapHeight[shadowSource]; j+=1.0/ShadowMapHeight[shadowSource]) {
 			texDepth = DepthToLinear(texture2D(ShadowMaps[shadowSource], shadowCoord.xy + vec2(i,j)).z);
 			if (fragDepth > texDepth + bias[shadowSource]) {
 				// It is an occluder
 				numAvgd += 1.0;
 				avgOccDepth += texDepth;
 			}
 		}
 	}
 	if (numAvgd == 0.0) {
 		return 1.0;      // Certainly unoccluded
 	}
 	avgOccDepth /= numAvgd;
 	
 	// Perform PCF with width acquired by average occluder depth
 	float sampleWidth = ceil((DepthToLinear(shadowCoord.z)-avgOccDepth) * LightWidth[shadowSource] / avgOccDepth); // DONE: use ceil or floor to reduce blocky artifacts
 	
 	float shade = 0.0;
 	for (float i=-sampleWidth/ShadowMapWidth[shadowSource]; i<=sampleWidth/ShadowMapWidth[shadowSource]; i+=1.0/ShadowMapWidth[shadowSource]) {
 		for (float j=-sampleWidth/ShadowMapHeight[shadowSource]; j<=sampleWidth/ShadowMapHeight[shadowSource]; j+=1.0/ShadowMapHeight[shadowSource]) {
			shade += getShadowVal(shadowCoord, vec2(i,j), 0);
 		}
 	}
 	float width = 2.0 * sampleWidth + 1.0;
 	return shade / (width * width);
 }

/** Gets the shadow value based on the current shadowing mode
 *
 * @param position The eyespace position of the surface at this fragment
 *
 * @return A 0-1 value for how shadowed the object is. 0 = shadowed and 1 = lit
 */
vec4 getShadowStrength(vec3 position, int shadowSource) {
	// DONE PA3: Transform position to ShadowCoord
	vec4 unshiftedShadowCoord = LightMatrix[shadowSource] * InverseViewMatrix[shadowSource] * vec4(position, 1.0);
	if (unshiftedShadowCoord.w != 0.0) {
		unshiftedShadowCoord /= unshiftedShadowCoord.w;
	}
	// Multiply by bias
	vec4 ShadowCoord = vec4( unshiftedShadowCoord.xyz * 0.5 + 0.5, 1.0 );
	if (ShadowMode == DEFAULT_SHADOW_MAP)
	{
		//return ShadowCoord.z;
		return getDefaultShadowMapVal(ShadowCoord, 0);
	}
	else if (ShadowMode == PCF_SHADOW_MAP)
	{
		return vec4(getPCFShadowMapVal(ShadowCoord, 0));
	}
	else
	{
		return vec4(getPCSSShadowMapVal(ShadowCoord, 0));
	}
}

/**
 * Performs Lambertian shading on the passed fragment data (color, normal, etc.) for a single light.
 * 
 * @param diffuse The diffuse color of the material at this fragment.
 * @param position The eyespace position of the surface at this fragment.
 * @param normal The eyespace normal of the surface at this fragment.
 * @param lightPosition The eyespace position of the light to compute lighting from.
 * @param lightColor The color of the light to apply.
 * @param lightAttenuation A vector of (constant, linear, quadratic) attenuation coefficients for this light.
 * 
 * @return The shaded fragment color; for Lambertian, this is `lightColor * diffuse * n_dot_l`.
 */
vec3 shadeLambertian(vec3 diffuse, vec3 position, vec3 normal, vec3 lightPosition, vec3 lightColor, vec3 lightAttenuation)
{
	vec3 lightDirection = normalize(lightPosition - position);
	float ndotl = max(0.0, dot(normal, lightDirection));

	// TODO PA4 Prereq (Optional): Paste in your n.l and n.h thresholding code if you like toon shading.
	
	
	float r = length(lightPosition - position);
	float attenuation = 1.0 / dot(lightAttenuation, vec3(1.0, r, r * r));
	
	return lightColor * attenuation * diffuse * ndotl;
}

/**
 * Performs Blinn-Phong shading on the passed fragment data (color, normal, etc.) for a single light.
 *  
 * @param diffuse The diffuse color of the material at this fragment.
 * @param specular The specular color of the material at this fragment.
 * @param exponent The Phong exponent packed into the alpha channel. 
 * @param position The eyespace position of the surface at this fragment.
 * @param normal The eyespace normal of the surface at this fragment.
 * @param lightPosition The eyespace position of the light to compute lighting from.
 * @param lightColor The color of the light to apply.
 * @param lightAttenuation A vector of (constant, linear, quadratic) attenuation coefficients for this light.
 * 
 * @return The shaded fragment color.
 */
vec3 shadeBlinnPhong(vec3 diffuse, vec3 specular, float exponent, vec3 position, vec3 normal,
	vec3 lightPosition, vec3 lightColor, vec3 lightAttenuation)
{
	vec3 viewDirection = -normalize(position);
	vec3 lightDirection = normalize(lightPosition - position);
	vec3 halfDirection = normalize(lightDirection + viewDirection);
		
	float ndotl = max(0.0, dot(normal, lightDirection));
	float ndoth = max(0.0, dot(normal, halfDirection));
	
	// TODO PA4 Prereq (Optional): Paste in your n.l and n.h thresholding code if you like toon shading.
	
	
	float pow_ndoth = (ndotl > 0.0 && ndoth > 0.0 ? pow(ndoth, exponent) : 0.0);


	float r = length(lightPosition - position);
	float attenuation = 1.0 / dot(lightAttenuation, vec3(1.0, r, r * r));
	
	return lightColor * attenuation * (diffuse * ndotl + specular * pow_ndoth);
}

void main()
{
	/* Sample gbuffer. */
	vec3 diffuse         = texture2DRect(DiffuseBuffer, gl_FragCoord.xy).xyz;
	vec3 position        = texture2DRect(PositionBuffer, gl_FragCoord.xy).xyz;
	vec4 materialParams1 = texture2DRect(MaterialParams1Buffer, gl_FragCoord.xy);
	vec4 materialParams2 = texture2DRect(MaterialParams2Buffer, gl_FragCoord.xy);
	vec3 normal          = decode(vec2(texture2DRect(DiffuseBuffer, gl_FragCoord.xy).a,
	                                   texture2DRect(PositionBuffer, gl_FragCoord.xy).a));
	
	/* Initialize fragment to black. */
	gl_FragColor = vec4(0.0, 0.0, 0.0, 1.0);

	/* Branch on material ID and shade as appropriate. */
	int materialID = int(materialParams1.x);

	if (materialID == 0)
	{
		/* Must be a fragment with no geometry, so set to sky (background) color. */
		gl_FragColor = vec4(SkyColor, 1.0);
	}
	else if (materialID == UNSHADED_MATERIAL_ID)
	{
		/* Unshaded material is just a constant color. */
		gl_FragColor.rgb = diffuse;
	}
	
	// TODO PA4 Prereq: Update our Lambertian and Blinn-Phong cases to use your material encoding schemes.
	else if (materialID == LAMBERTIAN_MATERIAL_ID)
	{
		/* Accumulate Lambertian shading for each light. */
		for (int i = 0; i < NumLights; ++i)
		{
			gl_FragColor.rgb += shadeLambertian(diffuse, position, normal, LightPositions[i], LightColors[i], LightAttenuations[i]);
		}
		/* Accumulate Shadow light source contributions */
		for (int j = 0; j < NumShadowMaps; ++j)
		{
			gl_FragColor.rgb += shadeLambertian(diffuse, position, normal, ShadowCamPosition[j], ShadowLightColors[j], ShadowLightAttenuations[j]) * 
									getShadowStrength(position, j).x;
		}
	}
	else if (materialID == BLINNPHONG_MATERIAL_ID)
	{
		/* Accumulate Blinn-Phong shading for each light. */
		for (int i = 0; i < NumLights; ++i)
		{
			gl_FragColor.rgb += shadeBlinnPhong(diffuse, materialParams1.gba, materialParams2.a,
				position, normal, LightPositions[i], LightColors[i], LightAttenuations[i]);
		}
		/* Accumulate Shadow light source contributions */
		for (int j = 0; j < NumShadowMaps; ++j)
		{
			gl_FragColor.rgb += shadeBlinnPhong(diffuse, materialParams1.gba, materialParams2.a,
										position, normal, ShadowCamPosition[j], ShadowLightColors[j], ShadowLightAttenuations[j]) * 
									getShadowStrength(position, j).x;
		}
	}
	else if (materialID == TERRAIN_MATERIAL_ID)
	{
		/* Accumulate Lambertian shading for each light. */
		for (int i = 0; i < NumLights; ++i)
		{
			gl_FragColor.rgb += shadeLambertian(diffuse, position, normal, LightPositions[i], LightColors[i], LightAttenuations[i]);
		}
		/* Accumulate Shadow light source contributions */
		for (int j = 0; j < NumShadowMaps; ++j)
		{
			gl_FragColor.rgb += shadeLambertian(diffuse, position, normal, ShadowCamPosition[j], ShadowLightColors[j], ShadowLightAttenuations[j]) * 
									getShadowStrength(position, j).x;
		}
	}
	else
	{
		/* Unknown material, so just use the diffuse color. */
		gl_FragColor.rgb = diffuse;
	}
	
	if (EnableSSAO)
	{
		gl_FragColor.rgb *= texture2DRect(SSAOBuffer, gl_FragCoord.xy).rgb;
	}
	
	/* Add in alpha blended particles */
	vec4 pColor = texture2DRect(ParticleBuffer, gl_FragCoord.xy);
	gl_FragColor.rgb = (gl_FragColor.rgb*(1.0-pColor.a) + pColor.rgb); //*pColor.a);		//pColor.rgb is already weighted...
}
