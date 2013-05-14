/** 
 *
 * soft_particles.fp
 * 
 */

uniform sampler2DRect DiffuseBuffer;
uniform sampler2DRect PositionBuffer;
uniform sampler2D SmokeTexture;

uniform int EnableSoftParticles;
uniform float NearPlane;
uniform float Tau;

varying float r;
varying float z;
varying vec2 TexCoord0;
varying float tau0;
 
void main() {
	vec4 color = vec4(0.0);

	float alpha = 0.0;
	float x = 2.0*r*(TexCoord0.x-0.5);
	float y = 2.0*r*(TexCoord0.y-0.5);
	
 	if (EnableSoftParticles == 1) {
 		// From the Umenhoffer et al. (Budapest University) paper on Spherical Billboards
 		float d = sqrt(x*x+y*y);
 		if (d < r) {
 			float w = sqrt(r*r - d*d);
 			float F = -z - w;
 			float B = -z + w;
 			float texDepth = texture2DRect(PositionBuffer, gl_FragCoord.xy).z;
 			float ds = min(-texDepth, B) - max(F, NearPlane);
 			if (texDepth == 0.0) {
 				ds = B - max(F, NearPlane);
 			}
 			if (ds > 0.0) {
 				alpha = max(0.0, 1.0 - exp(-tau0 * (1.0-d/r) * ds));
 			}
 		}
 	} else { 
 		
 		alpha = max(1.0-(x*x+y*y)/(r*r), 0.0);
 	} 
 	// Texture!!!
 	vec3 fromTex = texture2D(SmokeTexture, TexCoord0).xyz;
	color = vec4(1.0, 1.0, 1.0, alpha*fromTex.r);
	
 	gl_FragColor = vec4(color.rgb*color.a, color.a);
 	//gl_FragColor = vec4(1.0);
}