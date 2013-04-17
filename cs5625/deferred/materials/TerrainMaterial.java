package cs5625.deferred.materials;

import javax.media.opengl.GL2;
import javax.vecmath.Vector3f;

import cs5625.deferred.misc.OpenGLException;
import cs5625.deferred.rendering.ShaderProgram;

public class TerrainMaterial extends Material {

	private int mDiffuseUniformLocation;
	private int mLowerCornerUniformLocation;
	private Texture3D densityFunction;
	private Vector3f lowerCorner;
	
	public void setDensityFunction(Texture3D densityFunction) {
		this.densityFunction = densityFunction;
	}
	
	@Override
	public void bind(GL2 gl) throws OpenGLException {
		getShaderProgram().bind(gl);
		if (densityFunction == null) {
			throw new OpenGLException("no 3d texture");
		}
		densityFunction.bind(gl, 0);
		gl.glUniform3f(mDiffuseUniformLocation, 0.5f, 0.25f, 0.15f);
		gl.glUniform3f(mLowerCornerUniformLocation, lowerCorner.x, lowerCorner.y, lowerCorner.z);
		// TODO Auto-generated method stub
	}

	@Override
	public void unbind(GL2 gl) {
		getShaderProgram().unbind(gl);
		densityFunction.unbind(gl);
	}

	@Override
	public String getShaderIdentifier() {
		return "shaders/material_terrain";
	}

	public void setLowerCorner(Vector3f lowerCorner) {
		this.lowerCorner = lowerCorner;
	}
	
	@Override
	protected void initializeShader(GL2 gl, ShaderProgram shader)
	{
		System.out.println("hello");
		/* Get locations of uniforms in this shader. */
		mDiffuseUniformLocation = shader.getUniformLocation(gl, "DiffuseColor");
		mLowerCornerUniformLocation = shader.getUniformLocation(gl,  "LowerCorner");
		/* This uniform won't ever change, so just set it here. */
		shader.bind(gl);
		gl.glUniform1i(shader.getUniformLocation(gl, "DensityFunction"), 0);
		shader.unbind(gl);
	}
}
