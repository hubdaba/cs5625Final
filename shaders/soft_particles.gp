#version 120
#extension GL_EXT_gpu_shader4 : enable
#extension GL_EXT_geometry_shader4 : enable
 

uniform int EnableSoftParticles;
uniform float NearPlane;
uniform float Tau;

varying in float rad[];

varying out float r;
varying out vec2 TexCoord0;
varying out float z;

void main() {
	// Take position in and vector to camera, and emit a tri strip,
	// ordering SW, NW, SE, NE to match the tri strip ordering
	
	// Maybe, feed in the origin vertex, then do the model view transform
	// here, so billboarding is straightforward?
	vec4 loc = gl_ModelViewMatrix * gl_PositionIn[0];
	r = rad[0];
	z = loc.z;
	
	gl_Position = gl_ProjectionMatrix * (loc + vec4(-r, -r, 0.0, 0.0) );
	TexCoord0 = vec2(0.0, 0.0);
	EmitVertex();
	
	gl_Position = gl_ProjectionMatrix * (loc + vec4(-r,  r, 0.0, 0.0) );
	TexCoord0 = vec2(0.0, 1.0);
	EmitVertex();
	
	gl_Position = gl_ProjectionMatrix * (loc + vec4( r, -r, 0.0, 0.0) );
	TexCoord0 = vec2(1.0, 0.0);
	EmitVertex();
	
	gl_Position = gl_ProjectionMatrix * (loc + vec4( r,  r, 0.0, 0.0) );
	TexCoord0 = vec2(1.0, 1.0);
	EmitVertex(); 
	
	EndPrimitive();
}