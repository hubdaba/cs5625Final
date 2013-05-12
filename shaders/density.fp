uniform vec3 LowerCorner;
uniform int Layer;
uniform float NumVoxels;
uniform float BlockSize;

uniform int NumExplosions;
uniform vec3 ExplosionPositions[5];

uniform float ExplosionRadius;

uniform sampler2D Permutation;
uniform sampler2D Gradient;

vec3 fade(vec3 coord) {
	vec3 fade_vec = vec3(0.0);
	fade_vec.x = coord.x * coord.x * coord.x * (coord.x * (coord.x * 6.0 - 15.0) + 10.0);
	fade_vec.y = coord.y * coord.y * coord.y * (coord.y * (coord.y * 6.0 - 15.0) + 10.0);
	fade_vec.z = coord.z * coord.z * coord.z * (coord.z * (coord.z * 6.0 - 15.0) + 10.0);
	return fade_vec;
}

// return something between 0 and 255
float perm(float x) {
	return texture2D(Permutation, vec2(fract(x/256.0), 0.5)).x * 256.0;
}

float grad (float x, vec3 p) {
	return dot(texture2D(Gradient, vec2(fract(x/16.0), 0.5)).xyz, p);
}

float inoise(vec3 p) {
	vec3 remainder = vec3(0.0);
	int px_int = int(floor(p.x));
	int py_int = int(floor(p.y));
	int pz_int = int(floor(p.z));
	remainder.x = float(px_int - 256 * (px_int / 256));
	remainder.y = float(py_int - 256 * (py_int / 256));
	remainder.z = float(pz_int - 256 * (pz_int / 256));
	
	p = fract(p);
	vec3 f = fade(p);
	//vec3 f = p;
	
	float A = perm(remainder.x) + remainder.y;
	float AA = perm(A) + remainder.z;
	float AB = perm(A + 1.0) + remainder.z;
	float B = perm(remainder.x + 1.0) + remainder.y;
	float BA = perm(B) + remainder.z;
	float BB = perm(B + 1.0) + remainder.z;
	float corner1 = grad(perm(AA), p);
	float corner2 = grad(perm(BA), p + vec3(-1.0, 0.0, 0.0));
	float corner3 = grad(perm(AB), p + vec3(0.0, -1.0, 0.0)); 
	float corner4 = grad(perm(BB), p + vec3(-1.0, -1.0, 0.0));
	float corner5 = grad(perm(AA + 1.0), p + vec3(0.0, 0.0, -1.0));
	float corner6 = grad(perm(BA + 1.0), p + vec3(-1.0, 0.0, -1.0));
	float corner7 = grad(perm(AB + 1.0), p + vec3(0.0, -1.0, -1.0));
	float corner8 = grad(perm(BB + 1.0), p + vec3(-1.0, -1.0, -1.0));
	
	return mix(
			mix(mix(corner1, corner2, f.x), mix(corner3, corner4, f.x), f.y),
			mix(mix(corner5, corner6, f.x), mix(corner7, corner8, f.x), f.y),
			f.z);
}



void main() {
	float px = gl_FragCoord.x;
	float py = gl_FragCoord.y; 
	
	if (Layer == 0) {
		gl_FragColor.rgb = vec3(0.6);
		return;
	}
	vec3 worldCoord = vec3((px - 0.5)/(NumVoxels), ((py - 0.5)/(NumVoxels)), (float(Layer) - 1.0)/(NumVoxels));
	worldCoord = worldCoord * BlockSize + LowerCorner; 
	
	float explosion_offset = 0.0;
	
	for (int i = 0; i < NumExplosions; i++) {
		vec3 ExplosionCenter = ExplosionPositions[i];
		float distance = distance(ExplosionCenter, worldCoord);
		if (distance < ExplosionRadius) {
			explosion_offset += 200.0 * ((ExplosionRadius - distance)/ExplosionRadius);
		} 
	}
	
	float val = worldCoord.y + explosion_offset;
	for (float i = 0.0; i < 10.0; i+=1.0) {
		float n = 2.0 * i + 1.0;
		val += 0.5 * inoise((1.0 / n) * worldCoord) * (n);
	}
	
	gl_FragColor.rgb = vec3(val);
}
