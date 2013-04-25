package cs5625.deferred.materials;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;

import javax.media.opengl.GL2;

import cs5625.deferred.misc.OpenGLException;

public class Texture3D extends Texture {

	/*
	 * Texture object state. Public things have associated 'get' methods.
	 */
	private int mWidth = -1;
	private int mHeight = -1;
	private int mDepth = -1;
	private int mTarget = -1;
	
	public Texture3D(GL2 gl, Format format, Datatype datatype, int width, int height, int depth, Buffer buffer) throws OpenGLException {
		super(gl);
		initialize(gl, format, datatype, width, height, depth, buffer);
	}
	
	public Texture3D(GL2 gl, Format format, Datatype datatype, int width, int height, int depth) throws OpenGLException {
		this(gl, format, datatype, width, height, depth, null);
	}
	
	private void initialize(GL2 gl, Format format, Datatype datatype, int width, int height, int depth, Buffer buffer) throws OpenGLException {
		try {
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
	
	

}
