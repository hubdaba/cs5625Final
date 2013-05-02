package cs5625.deferred.misc;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Random;

import javax.media.opengl.GL2;
import javax.vecmath.Point3f;
import javax.vecmath.Point3i;
import javax.vecmath.Tuple3f;
import javax.vecmath.Vector3f;

import cs5625.deferred.materials.Texture.Datatype;
import cs5625.deferred.materials.Texture.Format;
import cs5625.deferred.materials.Texture.TextureWrapCoordinate;
import cs5625.deferred.materials.Texture.WrapMode;
import cs5625.deferred.materials.Texture2D;

public class PerlinNoise {
	
	private static Texture2D permutationTexture;
	
	public static int[][] gradients;
	private static Texture2D gradientTexture;
	
	public static int[] permutation;
	
	public static void init(GL2 gl) throws OpenGLException {
		// permutation setup
		permutation = new int[256];
		for (int i = 0; i < 256; i++) {
			permutation[i] = i;
		}
		
		Random rand = new Random();
		for (int i = 255; i > 0; i--) {
			int index = rand.nextInt(i + 1);
			int tmp = permutation[i];
			permutation[i] = permutation[index];
			permutation[index] = tmp;
		}
		byte[] permutationBytes = new byte[256 * 4];
		int count = 0;
		for (int i = 0; i < 255; i++) {
			permutationBytes[count++] = (byte)(0xFF & permutation[i]);
			permutationBytes[count++] = (byte)(0xFF & permutation[i]);
			permutationBytes[count++] = (byte)(0xFF & permutation[i]);
			permutationBytes[count++] = (byte)(0xFF & permutation[i]);
		}
		ByteBuffer permBuff = ByteBuffer.wrap(permutationBytes);
		
		permutationTexture = new Texture2D(gl, Format.RGBA, Datatype.INT8, 256, 1, permBuff);
		permutationTexture.setWrapModeOne(gl,  WrapMode.REPEAT, TextureWrapCoordinate.S);
		permutationTexture.setWrapModeOne(gl,  WrapMode.CLAMP, TextureWrapCoordinate.T);
		permutationTexture.enableInterpolation(gl, false);
		
		// gradient setup
		gradients = new int[][]{{1,1,0}, {-1,1,0}, {1,-1,0}, {-1,-1,0},
				  {1,0,1}, {-1,0,1}, {1,0,-1}, {-1,0,-1},
				  {0,1,1}, {0,-1,1}, {0,1,-1}, {0,-1,-1},
				  {1,1,0}, {0,-1,1}, {-1,1,0}, {0,-1,-1}};
		
		float[] gradientBytes = new float[16 * 4];
		for (int i = 0 ; i < 16; i++) {
			gradientBytes[i * 4] = gradients[i][0];
			gradientBytes[i * 4 + 1] = gradients[i][1];
			gradientBytes[i * 4 + 2] = gradients[i][2];
			gradientBytes[i * 4 + 3] = 1;
		}
		
		FloatBuffer gradBuff = FloatBuffer.wrap(gradientBytes);
		
		gradientTexture = new Texture2D(gl, Format.RGBA, Datatype.FLOAT16, 16, 1, gradBuff);
		//gradientTexture.setWrapModeOne(gl, WrapMode.REPEAT, TextureWrapCoordinate.S);
		//gradientTexture.setWrapModeOne(gl, WrapMode.CLAMP, TextureWrapCoordinate.T);
		gradientTexture.setWrapModeAll(gl, WrapMode.REPEAT);
		gradientTexture.enableInterpolation(gl, false);
	}
	
	public static void bind(GL2 gl, int textureUnit1, int textureUnit2) throws OpenGLException {
		permutationTexture.bind(gl, textureUnit1);
		gradientTexture.bind(gl, textureUnit2);
	}
	
	public static void unbind(GL2 gl) {
		permutationTexture.unbind(gl);
		gradientTexture.unbind(gl);
	}
	
	private static Point3f fade(Tuple3f point) {
		Point3f faded = new Point3f();
		faded.x = point.x * point.x * point.x * (point.x * (point.x * 6 - 15 + 10));
		faded.y = point.y * point.y * point.y * (point.y * (point.y * 6 - 15 + 10));
		faded.z = point.z * point.z * point.z * (point.z * (point.z * 6 - 15 + 10));
		return faded;
	}
	
	private static int perm(int hash) {
		int index = hash % 256;
		if (index < 0) {
			index += 256;
		}
		return permutation[index];
	}
	
	private static float grad(int x, Tuple3f p) {
		int index = x % 16;
		if (index < 0) {
			index += 16;
		}
		
		int[] gradient = gradients[index];
		return gradient[0] * p.x + gradient[1] * p.y + gradient[2] * p.z;
	}
	
	private static float lerp(float p1, float p2, float t) {
		return p1 + (p2 - p1) * t;
	}
	
	public static float noise(Tuple3f p) {
		Point3i pFloor = new Point3i();
		pFloor.x = (int)Math.floor(p.x);
		pFloor.y = (int)Math.floor(p.y);
		pFloor.z = (int)Math.floor(p.z);
		Point3f pFract = new Point3f(p);
		pFract.x = pFract.x % 1;
		pFract.y = pFract.y % 1;
		pFract.z = pFract.z % 1;
		Point3f faded = fade(pFract);
		int A = perm(pFloor.x) + pFloor.y;
		int AA = perm(A) + pFloor.z;
		int AB = perm(A + 1) + pFloor.z;
		int B = perm(pFloor.x + 1) + pFloor.y;
		int BA = perm(B ) + pFloor.z;
		int BB = perm(B + 1) + pFloor.z;
		
		Point3f corner1 = new Point3f(pFract);
		Point3f corner2 = new Point3f(pFract);
		corner2.add(new Vector3f(-1, 0, 0));
		Point3f corner3 = new Point3f(pFract);
		corner3.add(new Vector3f(0, -1, 0));
		Point3f corner4 = new Point3f(pFract);
		corner4.add(new Vector3f(-1, -1, 0));
		Point3f corner5 = new Point3f(pFract);
		corner5.add(new Vector3f(0, 0, -1));
		Point3f corner6 = new Point3f(pFract);
		corner6.add(new Vector3f(-1, 0, -1));
		Point3f corner7 = new Point3f(pFract);
		corner7.add(new Vector3f(0, -1, -1));
		Point3f corner8 = new Point3f(pFract);
		corner8.add(new Vector3f(-1, -1, -1));
		
		float lerp1 = lerp(grad(perm(AA), corner1), grad(perm(BA), corner2), faded.x);
		float lerp2 = lerp(grad(perm(AB), corner3), grad(perm(BB), corner4), faded.x);
		float lerp3 = lerp(grad(perm(AA + 1), corner5), grad(perm(BA + 1), corner6), faded.x);
		float lerp4 = lerp(grad(perm(AB + 1), corner7), grad(perm(BB + 1), corner8), faded.x);
		
		float lerp12 = lerp(lerp1, lerp2, faded.y);
		float lerp34 = lerp(lerp3, lerp4, faded.y);
		
		return lerp(lerp12, lerp34, faded.z);
		
		
		
		
	}
	
	
}
