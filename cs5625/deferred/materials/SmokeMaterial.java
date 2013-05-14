package cs5625.deferred.materials;

import java.io.IOException;
import java.util.HashMap;

import javax.media.opengl.GL2;

import cs5625.deferred.misc.OpenGLException;
import cs5625.deferred.rendering.Camera;
import cs5625.deferred.rendering.ShaderProgram;

public class SmokeMaterial extends Material {
	protected int mSmokeEnableSoftParticlesLocation=-1, mSmokeNearPlaneLocation=-1, mSmokeTauLocation=-1;
	
	public float mTau = 0.4f;
	public int mSmokeEnableSoftParticles = 1;
	
	Texture smokeTexture;

	public void bind(GL2 gl, Camera camera) throws OpenGLException {
		getShaderProgram().bind(gl);
		
		gl.glUniform1i(mSmokeEnableSoftParticlesLocation, mSmokeEnableSoftParticles);
		gl.glUniform1f(mSmokeNearPlaneLocation, camera.getNear());
		gl.glUniform1f(mSmokeTauLocation, mTau);
		smokeTexture.bind(gl, 2);
	}
	public void bind(GL2 gl) throws OpenGLException { 
		getShaderProgram().bind(gl);
	}

	public void unbind(GL2 gl) {
		getShaderProgram().unbind(gl);
		smokeTexture.unbind(gl);
	}

	public String getShaderIdentifier() {
		return "shaders/soft_particles";
	}
	
	@Override
	protected ShaderProgram createShader(GL2 gl) throws OpenGLException, IOException {
		return new ShaderProgram(gl, getShaderIdentifier(), true, GL2.GL_POINTS, GL2.GL_TRIANGLE_STRIP, 4);
	}
	
	@Override
	protected void initializeShader(GL2 gl, ShaderProgram shader)
	{
		shader.bind(gl);
		gl.glUniform1i(shader.getUniformLocation(gl, "DiffuseBuffer"), 0);
		gl.glUniform1i(shader.getUniformLocation(gl, "PositionBuffer"), 1);
		gl.glUniform1i(shader.getUniformLocation(gl, "SmokeTexture"), 2);
		shader.unbind(gl);
		
		mSmokeEnableSoftParticlesLocation = shader.getUniformLocation(gl, "EnableSoftParticles");
		mSmokeNearPlaneLocation = shader.getUniformLocation(gl, "NearPlane");
		mSmokeTauLocation = shader.getUniformLocation(gl, "Tau");
		
		try {
			smokeTexture = Texture2D.load(gl, "textures/smoke_texture.jpg", false);
		} catch (OpenGLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
	
	HashMap<String, Integer> fbos;
	public HashMap<String, Integer> getRequiredFBOs() {
		if (fbos==null) {
			fbos = new HashMap<String, Integer>();
			fbos.put("Diffuse", 0);
			fbos.put("Position", 1);
		}
		return fbos;
	}
	
	public String[] requiredAttributes = {};
	public String[] getRequiredVertexAttributes() {
		return requiredAttributes;
	}
}
