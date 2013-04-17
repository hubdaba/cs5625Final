package cs5625.deferred.rendering;

import javax.media.opengl.GL2;

import cs5625.deferred.materials.Texture3D;
import cs5625.deferred.misc.OpenGLException;

public class FramebufferObject3D {
	private Texture3D mTexture;
	private boolean mIsBound = false;
	private int mPreviousBinding[] = new int[1];
	private int mHandle = -1;
	private int mWidth = -1;
	private int mHeight = -1;
	private int mDepth = -1;
	
	public FramebufferObject3D(GL2 gl, Texture3D.Format format, Texture3D.Datatype datatype, int width,
									int height, int depth) throws OpenGLException {
		mWidth = width;
		mHeight = height;
		mDepth = depth;
		
		int names[] = new int[1];
		gl.glGenFramebuffers(1, names, 0);
		mHandle = names[0];
		
		int previousBinding[] = new int[1];
		gl.glGetIntegerv(GL2.GL_FRAMEBUFFER_BINDING, previousBinding, 0);
		
		gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, mHandle);
		
		mTexture = new Texture3D(gl, format, datatype, width, height, depth);
		gl.glFramebufferTextureEXT(GL2.GL_FRAMEBUFFER, GL2.GL_COLOR_ATTACHMENT0, mTexture.getHandle(), 0);
		
		int status = gl.glCheckFramebufferStatus(GL2.GL_FRAMEBUFFER);
		if (status != GL2.GL_FRAMEBUFFER_COMPLETE) {
			throw new OpenGLException("Framebuffer incomplete: " + status + ".");
		}
		
		gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, previousBinding[0]);
	
	}
	
	public void releaseGPUResources(GL2 gl) {
		mTexture.releaseGPUResources(gl);
		if (mHandle >= 0) {
			int names[] = new int[1];
			names[0] = mHandle;
			gl.glDeleteFramebuffers(1, names, 0);
			mHandle = -1;
		}
	}
	
	public void bind(GL2 gl) throws OpenGLException {
		if (!mIsBound) {
			gl.glPushAttrib(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_VIEWPORT_BIT);
			gl.glGetIntegerv(GL2.GL_FRAMEBUFFER_BINDING, mPreviousBinding, 0);
			gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, getHandle());
			gl.glViewport(0, 0, mWidth, mHeight);
			mIsBound = true;
		}	
		gl.glFramebufferTextureEXT(GL2.GL_FRAMEBUFFER, GL2.GL_COLOR_ATTACHMENT0, mTexture.getHandle(), 0);
	//	gl.glDrawBuffer(GL2.GL_COLOR_ATTACHMENT0);
	}
	
	public void unbind(GL2 gl) throws OpenGLException {
		if (mIsBound) {
			gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, mPreviousBinding[0]);
			gl.glPopAttrib();
			mIsBound = false;
			
			OpenGLException.checkOpenGLError(gl);
		}
	}
	
	public int getHandle() {
		return mHandle;
	}
	
	public int getWidth() {
		return mWidth;
	}
	
	public int getHeight() {
		return mHeight;
	}
	
	public int getDepth() {
		return mDepth;
	}
	
	public Texture3D getTexture() {
		return this.mTexture;
	}
	
	
}


