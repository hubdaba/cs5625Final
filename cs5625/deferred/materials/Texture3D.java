package cs5625.deferred.materials;

import java.nio.Buffer;
import java.nio.FloatBuffer;

import javax.media.opengl.GL2;
import javax.vecmath.Point3f;

import cs5625.deferred.misc.OpenGLException;
import cs5625.deferred.scenegraph.TerrainRenderer;

public class Texture3D extends Texture {

	/*
	 * Texture object state. Public things have associated 'get' methods.
	 */
	private int mWidth = -1;
	private int mHeight = -1;
	private int mDepth = -1;
	private int mTarget = -1;
	Format format;
	Datatype datatype;
	float[] texture_val;

	public Texture3D(GL2 gl, Format format, Datatype datatype, int width, int height, int depth, Buffer buffer) throws OpenGLException {
		super(gl);
		initialize(gl, format, datatype, width, height, depth, buffer);
	}

	public Texture3D(GL2 gl, Format format, Datatype datatype, int width, int height, int depth) throws OpenGLException {
		this(gl, format, datatype, width, height, depth, null);
	}

	private void initialize(GL2 gl, Format format, Datatype datatype, int width, int height, int depth, Buffer buffer) throws OpenGLException {
		try {
			this.format = format;
			this.datatype = datatype;
			int gltype = datatype.toGLtype();
			int glformat = format.toGLformat();
			int glinternalformat = format.toGLinternalformat(datatype);
			mTarget = GL2.GL_TEXTURE_3D;

			mWidth = width;
			mHeight = height;
			mDepth = depth;
			mFormat = format;
			mDatatype = datatype;

			bind(gl, 0);

			int previousActive[] = new int[1];
			gl.glGetIntegerv(GL2.GL_ACTIVE_TEXTURE, previousActive, 0);
			gl.glActiveTexture(GL2.GL_TEXTURE0 + getBoundTextureUnit());

			gl.glTexImage3D(mTarget, 0, glinternalformat, width, height, depth, 0, glformat, gltype, buffer);
			gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_NEAREST);
			gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_NEAREST);
			gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP);
			gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP);
			gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL2.GL_TEXTURE_WRAP_R, GL2.GL_CLAMP);

			gl.glActiveTexture(previousActive[0]);



			unbind(gl);
			OpenGLException.checkOpenGLError(gl);
		} catch (OpenGLException err) {
			releaseGPUResources(gl);
			throw err;
		}
	}

	public int getWidth() {
		return mWidth;
	}

	public int getDepth() {
		return mDepth;
	}

	public int getHeight() {
		return mHeight;
	}

	@Override
	public int getTextureTarget() {
		// TODO Auto-generated method stub
		return mTarget;
	}
	
	private float mix(float val1, float val2, float frac) {
		return val1 + (val2 - val1) * frac;
	}
	
	private float readTexel(int i, int j, int k) {
		int yDim = TerrainRenderer.NUM_VOXELS + 1;
		int zDim = (int) Math.pow(TerrainRenderer.NUM_VOXELS + 1, 2);
		return texture_val[(i + j * yDim + (k + 1) * zDim * 4)];
	}
	
	public int hasTriangles(int i, int j, int k) {
		float tex1 = readTexel(i, j, k);
		float tex2 = readTexel(i + 1, j, k);
		float tex3 = readTexel(i + 1, j + 1, k);
		float tex4 = readTexel(i, j + 1, k);
		float tex5 = readTexel(i, j, k + 1);
		float tex6 = readTexel(i + 1, j, k + 1);
		float tex7 = readTexel(i + 1, j + 1, k + 1);
		float tex8 = readTexel(i, j + 1, k + 1);
		int voxelCode = 0;
		if (tex1 <= 0) {
			voxelCode = voxelCode | 1;
		}
		if (tex2 <= 0) {
			voxelCode = voxelCode | 2;
		}
		if (tex3 <= 0) {
			voxelCode = voxelCode | 4;
		}
		if (tex4 <= 0) {
			voxelCode = voxelCode | 8;
		}
		if (tex5 <= 0) {
			voxelCode = voxelCode | 16;
		}
		if (tex6 <= 0) {
			voxelCode = voxelCode | 32;
		}
		if (tex7 <= 0) {
			voxelCode = voxelCode | 64;
		}
		if (tex8 <= 0) {
			voxelCode = voxelCode | 128;
		}	
		return voxelCode;
	}
	
	public float sample3D(Point3f location) throws OpenGLException {
		int xcor = (int) Math.floor(TerrainRenderer.NUM_VOXELS * location.x);
		int ycor = (int) Math.floor(TerrainRenderer.NUM_VOXELS * location.y);
		int zcor = (int) Math.floor(TerrainRenderer.NUM_VOXELS * location.z) + 1;
		int yDim = TerrainRenderer.NUM_VOXELS + 1;
		int zDim = (int) Math.pow(TerrainRenderer.NUM_VOXELS + 1, 2);
		float texture_value1 = texture_val[(xcor + ycor * yDim + zcor * zDim) * 4];
		float texture_value2 = texture_val[((xcor + 1) + (ycor) * yDim + zcor * zDim) * 4];
		float texture_value3 = texture_val[(xcor + (ycor + 1) * yDim + zcor * zDim) * 4];
		float texture_value4 = texture_val[((xcor + 1) + (ycor + 1) * yDim + zcor * zDim) * 4];
		float texture_value5 = texture_val[((xcor) + (ycor) * yDim + (zcor + 1) * zDim) * 4];
		float texture_value6 = texture_val[((xcor + 1) + (ycor) * yDim + (zcor + 1) * zDim) * 4];
		float texture_value7 = texture_val[(xcor + (ycor + 1) * yDim + (zcor + 1) * zDim) * 4];
		float texture_value8 = texture_val[((xcor + 1) + (ycor + 1) * yDim + (zcor + 1) * zDim) * 4];
		float xFrac = location.x * TerrainRenderer.NUM_VOXELS - xcor;
		float yFrac = location.y * TerrainRenderer.NUM_VOXELS - ycor;
		float zFrac = location.z * TerrainRenderer.NUM_VOXELS - zcor;
		return mix(
					mix(mix(texture_value1, texture_value2, xFrac), 
						mix(texture_value3, texture_value4, xFrac), yFrac),
					mix(mix(texture_value5, texture_value6, xFrac),
						mix(texture_value7, texture_value8, xFrac), yFrac),
					zFrac);
		
	}

	public void getPixelData(GL2 gl) throws OpenGLException {
		int gltype = datatype.toGLtype();
		int glformat = format.toGLformat();
		mTarget = GL2.GL_TEXTURE_3D;

		FloatBuffer buffer = FloatBuffer.allocate(mHeight * mWidth * mDepth *  4);
		bind(gl, 0);
		gl.glGetTexImage(mTarget, 0, glformat, gltype, buffer);
		float[] array = buffer.array();
		unbind(gl);
		texture_val = array;
		
	}
	
	@Override
	public void releaseGPUResources(GL2 gl) {
		super.releaseGPUResources(gl);
		//texture_val = null;
	}



}
