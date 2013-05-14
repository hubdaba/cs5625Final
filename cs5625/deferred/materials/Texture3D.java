package cs5625.deferred.materials;

import java.nio.Buffer;
import java.nio.FloatBuffer;

import javax.media.opengl.GL2;
import javax.vecmath.Point3f;

import cs5625.deferred.misc.OpenGLException;

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
	
	public float sample3D(Point3f location) throws OpenGLException {
		int xcor = (int) (mWidth * location.x);
		int ycor = (int) (mHeight * location.y);
		int zcor = (int) (mDepth * location.z) + 1;
		float texture_value = texture_val[(xcor + ycor * 20 + zcor * 400) * 4];
		return texture_value;
	}

	public void getPixelData(GL2 gl) throws OpenGLException {
		int gltype = datatype.toGLtype();
		int glformat = format.toGLformat();
		mTarget = GL2.GL_TEXTURE_3D;

		FloatBuffer buffer = FloatBuffer.allocate(21 * 21 * 22 *  4);
		bind(gl, 0);
		gl.glGetTexImage(mTarget, 0, glformat, gltype, buffer);
		float[] array = buffer.array();
		unbind(gl);
		texture_val = array;
		
	}
	
	@Override
	public void releaseGPUResources(GL2 gl) {
		super.releaseGPUResources(gl);
		texture_val = null;
	}



}
