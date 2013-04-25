package cs5625.deferred.misc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

import javax.media.opengl.GL2;

import cs5625.deferred.materials.Texture.Datatype;
import cs5625.deferred.materials.Texture.Format;
import cs5625.deferred.materials.Texture3D;

public class PerlinNoise {
	
	private static Texture3D perlinNoise;
	public static int[][][][] noise;
	
	public static void init(GL2 gl) {
		File file = new File("src" + File.separator + 
						"textures" + File.separator + "perlin_noise.tex");
		try {
			perlinNoise = createFromFile(gl, 
					file);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (OpenGLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	public static void bind(GL2 gl, int textureUnit) throws OpenGLException {
		perlinNoise.bind(gl, textureUnit);
	}
	
	public static void unbind(GL2 gl) {
		perlinNoise.unbind(gl);
	}
	
	public static Texture3D createFromFile(GL2 gl, File file) throws IOException, OpenGLException {
		FileInputStream stream = new FileInputStream(file);
		byte texdata[] = readEntireFile(stream);
		noise = new int[64][64][64][4];
		int count = 0;
		for (int i = 0; i < 64; i++) {
			for (int j = 0; j < 64; j++) {
				for (int k = 0; k < 64; k++) {
					noise[i][j][k][0] = 0xFF & (int)texdata[count++];
					noise[i][j][k][1] = 0xFF & (int)texdata[count++];
					noise[i][j][k][2] = 0xFF & (int)texdata[count++];
					noise[i][j][k][3] = 0xFF & (int)texdata[count++];
					
				}
			}
		}
		ByteBuffer buf = ByteBuffer.wrap(texdata);
		return new Texture3D(gl, Format.RGBA, Datatype.INT8, 64, 64, 64, buf);
	}
	
	private static byte[] readEntireFile(InputStream inputStream) throws IOException
	{
	    ByteArrayOutputStream out = new ByteArrayOutputStream();
	    
	    byte[] data = new byte[4096];
	    int count = inputStream.read(data);
	    
	    while(count != -1)
	    {
	        out.write(data, 0, count);
	        count = inputStream.read(data);
	    }

	    return out.toByteArray();
	}
}
