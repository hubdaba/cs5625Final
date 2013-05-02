package cs5625.deferred.rendering;

import javax.media.opengl.GL2;

import cs5625.deferred.misc.OpenGLResourceObject;

public class VertexBuffer implements OpenGLResourceObject {
	
	int bufferHandle;
	
	public VertexBuffer(GL2 gl, int numVoxels) {
		int[] buffer = new int[1];

		gl.glGenBuffers(1, buffer, 0);
		gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, buffer[0]);
		gl.glBufferData(GL2.GL_ARRAY_BUFFER, 4L * (long)Math.pow(numVoxels, 3) * 15L * 8L, null, GL2.GL_STREAM_DRAW);
		bufferHandle = buffer[0];
		gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
	}
	
	public void bind(GL2 gl) {
		gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, this.bufferHandle);
	}
	
	public void unbind(GL2 gl) {
		gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
	}
	
	public void bindTransformFeedback(GL2 gl) {
		gl.glBindBufferBase(GL2.GL_TRANSFORM_FEEDBACK_BUFFER, 0, bufferHandle);
	}
	
	public void unbindTransformFeedbacl(GL2 gl) {
		gl.glBindBufferBase(GL2.GL_TRANSFORM_FEEDBACK_BUFFER, 0, 0);
	}

	@Override
	public void releaseGPUResources(GL2 gl) {
		int buffers[] = new int[1];
		buffers[0] = bufferHandle;
		gl.glDeleteBuffers(1, buffers, 0);
	}

}
