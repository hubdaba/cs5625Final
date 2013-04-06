/**
 * ssao.fp
 * 
 * Written for Cornell CS 5625 (Interactive Computer Graphics).
 * Copyright (c) 2013, Computer Science Department, Cornell University.
 * 
 * @author Sean Ryan (ser99)
 * @date 2013-03-23
 */

uniform sampler2DRect DiffuseBuffer;
uniform sampler2DRect PositionBuffer;

#define MAX_RAYS 100
uniform int NumRays;
uniform vec3 SampleRays[MAX_RAYS];
uniform float SampleRadius;

uniform mat4 ProjectionMatrix;
uniform vec2 ScreenSize;

/* Decodes a vec2 into a normalized vector See Renderer.java for more info. */
vec3 decode(vec2 v)
{
	vec3 n;
	n.z = 2.0 * dot(v.xy, v.xy) - 1.0;
	n.xy = normalize(v.xy) * sqrt(1.0 - n.z*n.z);
	return n;
}

void main()
{
	// TODO PA4: Implement SSAO. Your output color should be grayscale where white is unobscured and black is fully obscured.
	vec4 diffuse = texture2DRect(DiffuseBuffer, gl_FragCoord.xy);
	vec4 position = texture2DRect(PositionBuffer, gl_FragCoord.xy);
	int i;
	vec3 normal = normalize(decode(vec2(diffuse.a, position.a)));
	
	vec3 axis1 = cross(normal, vec3(0.0, 0.0, 1.0));
	vec3 axis2 = cross(normal, axis1);
	
	mat3 basisMat = mat3(axis1, axis2, normal);
	//mat3 basisMat = mat3(vec3(0.0, 0.0, 1.0), vec3(1.0, 0.0, 0.0), vec3(0.0, 1.0, 0.0));
	float numObstructed = 0.0;
	float dotSum = 0.0;
	
	vec4 clipSpacePos = ProjectionMatrix * vec4(position.xyz, 1.0);
	if (clipSpacePos.z <= 0.0) {
		gl_FragColor = vec4(1.0);
		return;
	}
	for (i = 0; i < NumRays; i++) {
		vec3 transformedRay = SampleRadius * SampleRays[i];
		if (dot(transformedRay, normal) < 0.0) {
			continue;
		}
		vec4 clipSpace = ProjectionMatrix * vec4(transformedRay + position.xyz, 1.0);
		clipSpace = clipSpace / clipSpace.w;
		vec2 pixelCoord = ScreenSize * ((clipSpace.xy + 1.0)/2.0);
		vec3 intersectPos = texture2DRect(PositionBuffer, pixelCoord).xyz;
		vec4 clipSpaceIntersect = ProjectionMatrix * vec4(intersectPos, 1.0);
		clipSpaceIntersect = clipSpaceIntersect / clipSpaceIntersect.w;
		if (clipSpaceIntersect.z < clipSpace.z && clipSpaceIntersect.z > 0.0) {
			numObstructed += dot(normal, normalize(SampleRays[i]));
		}
		dotSum += dot(normal, normalize(SampleRays[i]));
	}
	
	gl_FragColor = vec4((dotSum - numObstructed)/dotSum);
	//gl_FragColor = vec4(1.0);
}
