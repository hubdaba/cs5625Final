package cs5625.deferred.materials;

import java.io.IOException;

import javax.media.opengl.GL2;
import javax.vecmath.Point3f;
import javax.vecmath.Tuple3f;

import cs5625.deferred.misc.OpenGLException;
import cs5625.deferred.rendering.ShaderProgram;

public class TerrainMaterial extends Material {

	private int mDiffuseUniformLocation;
	private int mLowerCornerUniformLocation;
	private int mNumVoxelsUniformLocation;
	private int mBlockSizeUniformLocation;
	private Texture3D densityFunction;
	private Point3f lowerCorner;
	private int numVoxels;
	private float blockSize;
	
	private Texture2D grassTexture;
	private Texture2D mossTexture;
	private Texture2D rockTexture;
	
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
		grassTexture.bind(gl, 1);
		mossTexture.bind(gl, 2);
		rockTexture.bind(gl, 3);
		gl.glUniform3f(mDiffuseUniformLocation, 0.5f, 0.25f, 0.15f);
		gl.glUniform3f(mLowerCornerUniformLocation, lowerCorner.x, lowerCorner.y, lowerCorner.z);
		gl.glUniform1f(mNumVoxelsUniformLocation, numVoxels);
		gl.glUniform1f(mBlockSizeUniformLocation, blockSize);
		// TODO Auto-generated method stub
	}

	@Override
	public void unbind(GL2 gl) {
		getShaderProgram().unbind(gl);
		densityFunction.unbind(gl);
		grassTexture.unbind(gl);
		mossTexture.unbind(gl);
		rockTexture.unbind(gl);
	}

	@Override
	public String getShaderIdentifier() {
		return "shaders/material_terrain";
	}

	public void setLowerCorner(Tuple3f lowerCorner) {
		this.lowerCorner = new Point3f(lowerCorner);
	}
	
	public void setNumVoxels(int numVoxels) {
		this.numVoxels = numVoxels;
	}
	
	public void setBlockSize(float blockSize) {
		this.blockSize = blockSize;
	}
	
	@Override
	protected void initializeShader(GL2 gl, ShaderProgram shader)
	{
		/* Get locations of uniforms in this shader. */
		mDiffuseUniformLocation = shader.getUniformLocation(gl, "DiffuseColor");
		mLowerCornerUniformLocation = shader.getUniformLocation(gl,  "LowerCorner");
		mNumVoxelsUniformLocation = shader.getUniformLocation(gl,  "NumVoxels");
		mBlockSizeUniformLocation = shader.getUniformLocation(gl, "BlockSize");
		
		try {
			grassTexture = Texture2D.load(gl, "textures/grass_texture.jpg", false);
			mossTexture = Texture2D.load(gl, "textures/mossy_texture.jpg", false);
			rockTexture = Texture2D.load(gl, "textures/rock_texture.jpg", false);
		} catch (OpenGLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		/* This uniform won't ever change, so just set it here. */
		shader.bind(gl);
		gl.glUniform1i(shader.getUniformLocation(gl, "DensityFunction"), 0);
		gl.glUniform1i(shader.getUniformLocation(gl, "GrassTexture"), 1);
		gl.glUniform1i(shader.getUniformLocation(gl, "MossTexture"), 2);
		gl.glUniform1i(shader.getUniformLocation(gl, "RockTexture"), 3);
		shader.unbind(gl);
	}
}
