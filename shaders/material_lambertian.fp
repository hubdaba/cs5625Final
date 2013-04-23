/**
 * material_lambertian.fp
 * 
 * Fragment shader shader which writes material information needed for Lambertian shading to
 * the gbuffer.
 * 
 * Written for Cornell CS 5625 (Interactive Computer Graphics).
 * Copyright (c) 2012, Computer Science Department, Cornell University.
 * 
 * @author Asher Dunn (ad488)
 * @date 2012-03-27
 */

/* ID of Lambertian material, so the lighting shader knows what material
 * this pixel is. */
const int LAMBERTIAN_MATERIAL_ID = 2;

/* Material properties passed from the application. */
uniform vec3 DiffuseColor;

/* Textures and flags for whether they exist. */
uniform sampler2D DiffuseTexture;
uniform bool HasDiffuseTexture;

uniform sampler3D DiffuseTexture3D;
uniform bool HasDiffuseTexture3D;

/* Fragment position, normal, and texcoord passed from the vertex shader. */
varying vec3 EyespacePosition;
varying vec3 EyespaceNormal;
varying vec2 TexCoord;
varying vec3 TexCoord3D;

/* Encodes a normalized vector as a vec2. See Renderer.java for more info. */
vec2 encode(vec3 n)
{
	return normalize(n.xy) * sqrt(0.5 * n.z + 0.5);
}

void main()
{
    vec2 enc = encode(normalize(EyespaceNormal));
  // Store diffuse color, position, encoded normal, material ID, and all other useful data in the g-buffer.
   if (HasDiffuseTexture) {
      gl_FragData[0] = vec4(texture2D(DiffuseTexture, TexCoord).xyz, enc.x);
    } else {
  		gl_FragData[0] = vec4(DiffuseColor, enc.x);
   	}
 	if (HasDiffuseTexture3D) {
   		float val = texture3D(DiffuseTexture3D, TexCoord3D).x;
   		if (val > 1.0) {
   			gl_FragData[0] = vec4(0.0, 0.0, 1.0, enc.x);
   		} else if (val < -0.05) {
   			gl_FragData[0] = vec4(1.0, 0.0, 0.0, enc.x);
   		} else if (val > -0.01 && val < 0.05) {
   			gl_FragData[0] = vec4(0.0, 1.0, 1.0, enc.x);
   		} else {
   			gl_FragData[0] = vec4(0.0, 1.0, 0.0, enc.x);
   		}
   	} else {
   		gl_FragData[0] = vec4(1.0, 1.0, 0.0, enc.x);
   	}
   	gl_FragData[1] = vec4(EyespacePosition, enc.y);
    gl_FragData[2] = vec4(float(LAMBERTIAN_MATERIAL_ID), 0.0, 0.0, 0.0);
    gl_FragData[3] = vec4(0.0);
}
