package cs5625.deferred.rendering;

import java.io.IOException;

import javax.media.opengl.GL2;
import javax.media.opengl.glu.GLU;
import javax.vecmath.Vector3f;

import com.jogamp.opengl.util.gl2.GLUT;

import cs5625.deferred.materials.Texture2D;
import cs5625.deferred.materials.Texture3D;
import cs5625.deferred.misc.OpenGLException;

public class TerrainBlockRenderer {
	private static int NUM_VOXELS = 32;
	private static float BLOCK_SIZE = 1f;
	private ShaderProgram mDensityShader;
	
	private ShaderProgram mTerrainShader;

	/* The uniform location for the terrain generation*/
	private int mLayerTerrainUniformLocation;
	private int mCentroidTerrainUniformLocation;
	private int mVoxelDimTerrainUniformLocation;

	private FramebufferObject3D blockDensityFunction;
	
	Vector3f lowerCorner;

	public TerrainBlockRenderer(GL2 gl, Vector3f lowerCorner) throws OpenGLException, IOException {
		this.lowerCorner = lowerCorner;
		mDensityShader = new ShaderProgram(gl, "shaders/density", true, GL2.GL_TRIANGLES,
				GL2.GL_TRIANGLE_STRIP, 3);
	//	mTerrainShader = new ShaderProgram(gl, "shaders/terrain", true, GL2.GL_POINTS, GL2.GL_TRIANGLES, 15);
		
		
		mLayerTerrainUniformLocation = mDensityShader.getUniformLocation(gl, "Layer");
		mCentroidTerrainUniformLocation = mDensityShader.getUniformLocation(gl, "Centroid");
		mVoxelDimTerrainUniformLocation = mDensityShader.getUniformLocation(gl, "VoxelDim");
		blockDensityFunction = new FramebufferObject3D(gl, Texture3D.Format.RGBA,
				Texture3D.Datatype.FLOAT16, NUM_VOXELS + 1,NUM_VOXELS + 1, NUM_VOXELS + 1);
	
	}

	public void fillTexture3D(GL2 gl) throws OpenGLException {
		gl.glPushAttrib(GL2.GL_TRANSFORM_BIT);
		
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		GLU glu = GLU.createGLU(gl);
		glu.gluPerspective(90, 1.0f, 1.0f, 100f);
		
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		gl.glTranslated(0, 0, -1);
		
		
		gl.glViewport(0, 0, NUM_VOXELS + 1, NUM_VOXELS + 1);
		gl.glEnable(GL2.GL_TEXTURE_3D);

		blockDensityFunction.bind(gl);
		gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
		
		
		for (int i = 0; i <= 32; i++) {
			mDensityShader.bind(gl);
			gl.glUniform1i(mLayerTerrainUniformLocation, i);
			gl.glUniform3f(mCentroidTerrainUniformLocation, lowerCorner.x, lowerCorner.y, lowerCorner.z);
			gl.glUniform1f(mVoxelDimTerrainUniformLocation, BLOCK_SIZE / NUM_VOXELS);
			
			gl.glBegin(GL2.GL_TRIANGLES);
			gl.glVertex3d(1, 1, 0);
			gl.glVertex3d(-1, 1, 0);
			gl.glVertex3d(-1, -1, 0);
			gl.glVertex3d(-1, -1, 0);
			gl.glVertex3d(1, -1, 0);
			gl.glVertex3d(1, 1, 0);
			gl.glEnd();
			mDensityShader.unbind(gl);
		}
		
		blockDensityFunction.unbind(gl);
		
		/*
		mTerrainShader.bind(gl);
		gl.glUniform1i(mTerrainShader.getUniformLocation(gl, "DensityTexture"), 0);
		for (int i = 0; i < 32; i++) {
			for (int j = 0; j < 32; j++) {
				for (int k = 0; k < 32; k++) {
					gl.glBegin(GL2.GL_POINTS);
					gl.glUniform1i(arg0, arg1);
					gl.glUniform3i(mPointTerrainLocation, i, j, k);
					gl.glVertex3d(0, 0, 0);
				}
			}
		}*/
		
		gl.glDisable(GL2.GL_TEXTURE_3D);
		gl.glPopMatrix();

		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glPopMatrix();

		
		/* Restore active matrix. */
		gl.glPopAttrib();
		
		
		
	}
	
	public void testTexture(GL2 gl, float width, float height, Texture2D texture) throws OpenGLException {
		
		double[][] verts = {{-1.0, -1.0, 0.0}, {-1.0, 1.0, 0.0}, {1.0, 1.0, 0.0}, {1.0, -1.0, 0.0}}; 
		
		/* Save which matrix is active. */
		gl.glPushAttrib(GL2.GL_TRANSFORM_BIT);
		
		/* Reset projection and modelview matrices. */
		gl.glMatrixMode(GL2.GL_PROJECTION);
		
		gl.glPushMatrix();
		gl.glLoadIdentity();
		GLU glu = GLU.createGLU(gl);
		glu.gluPerspective(60, width/height, 1.0f, 100f);
		
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		gl.glTranslated(0.0, 0.0, -5.0);
		gl.glRotated(-45, 1.0, 0.0, 0.0);
		
		gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
		
		gl.glEnable(GL2.GL_TEXTURE_3D);
		this.blockDensityFunction.getTexture().bind(gl, 0);
	
		gl.glBegin(GL2.GL_TRIANGLES);
		for (int x = 0; x <= 3; x++) {
			gl.glTexCoord3d(0.0, 0.0, 1.5);
			gl.glVertex3d(0.0, 0.0, 1.5);
			gl.glTexCoord3d(verts[x][0], verts[x][1], verts[x][2]);
			gl.glVertex3d(verts[x][0], verts[x][1], verts[x][2]);
			gl.glTexCoord3d(verts[(x+1)%4][0], verts[(x+1)%4][1], verts[(x+1)%4][2]);
			gl.glVertex3d(verts[(x+1)%4][0], verts[(x+1)%4][1], verts[(x+1)%4][2]);
		}
		gl.glEnd();
		
		this.blockDensityFunction.getTexture().unbind(gl);
		gl.glDisable(GL2.GL_TEXTURE_3D);
		gl.glPopMatrix();
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glPopMatrix();
		
		
		gl.glPopAttrib();
		
		/* Make sure nothing went wrong. */
		OpenGLException.checkOpenGLError(gl);
	
	}
}
