package cs5625.deferred.scenegraph;

import geometry.Explosion;
import geometry.SuperBlock;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;

import javax.media.opengl.GL2;
import javax.media.opengl.glu.GLU;
import javax.vecmath.Point3f;
import javax.vecmath.Tuple3f;

import cs5625.deferred.apps.ExploreSceneController;
import cs5625.deferred.materials.Texture;
import cs5625.deferred.materials.Texture2D;
import cs5625.deferred.materials.Texture3D;
import cs5625.deferred.misc.OpenGLException;
import cs5625.deferred.misc.PerlinNoise;
import cs5625.deferred.rendering.FramebufferObject3D;
import cs5625.deferred.rendering.McTables;
import cs5625.deferred.rendering.ShaderProgram;
import cs5625.deferred.rendering.VertexBuffer;

public class TerrainBlockRenderer extends SuperBlock {
	public int numVoxels;
	private static ShaderProgram mDensityShader;

	private static ShaderProgram mTerrainShader;

	/* The uniform location for the terrain generation*/
	private static int[] mExplosionPositionUniformLocation;
	private static int mNumExplosionsUniformLocation;
	private static int mLayerDensityUniformLocation;
	private static int mLowerCornerDensityUniformLocation;
	private static int mNumVoxelsDensityUniformLocation;
	private static int mBlockSizeDensityUniformLocation;
	private static int mExplosionRadiusUniformLocation;

	private static int mTerrainLowerCornerUniformLocation;
	private static int mTerrainNumVoxelsUniformLocation;
	private static int mTerrainBlockSizeUniformLocation;
	private FramebufferObject3D blockDensityFunction;

	private static boolean initialized = false;

	private static Texture2D triTableTextureHandle;

	private VertexBuffer buffer;

	public static int NUM_EXPLOSIONS = 50; 
	
	private int num_polygons = -1;
	private boolean density_filled;
	private boolean buffer_filled;

	private List<Explosion> explosions;


	public TerrainBlockRenderer(GL2 gl, Tuple3f minPoint, int numVoxels, float sideLength,
												List<Explosion> explosions) throws OpenGLException, IOException {
		super(minPoint, sideLength);
		this.numVoxels = numVoxels;
		density_filled = false;
		buffer_filled = false;
		setup(gl);
		this.explosions = explosions;
	}
	

	public static void initializeTerrain(GL2 gl) throws OpenGLException, IOException {
		if (!initialized) {
			int index = 0;

			float[] triTableTripled = new float[McTables.triTable.length * 4];
			index = 0;
			for (int i = 0; i < McTables.triTable.length; i++) {
				triTableTripled[index++] = McTables.triTable[i];
				triTableTripled[index++] = McTables.triTable[i];
				triTableTripled[index++] = McTables.triTable[i];
				triTableTripled[index++] = McTables.triTable[i];

			}

			FloatBuffer triTableValues = FloatBuffer.wrap(triTableTripled);
			triTableTextureHandle = new Texture2D(gl, Texture.Format.RGBA, Texture.Datatype.FLOAT16,
					16, 256, triTableValues, false);

			mDensityShader = new ShaderProgram(gl, "shaders/density", true, GL2.GL_TRIANGLES,
					GL2.GL_TRIANGLE_STRIP, 3);
			mTerrainShader = new ShaderProgram(gl, "shaders/terrain_maker", true, GL2.GL_POINTS, GL2.GL_TRIANGLE_STRIP, 15, true);


			mLayerDensityUniformLocation = mDensityShader.getUniformLocation(gl, "Layer");
			mLowerCornerDensityUniformLocation = mDensityShader.getUniformLocation(gl, "LowerCorner");
			mNumVoxelsDensityUniformLocation = mDensityShader.getUniformLocation(gl, "NumVoxels");
			mBlockSizeDensityUniformLocation = mDensityShader.getUniformLocation(gl, "BlockSize");
			mExplosionPositionUniformLocation = new int[NUM_EXPLOSIONS];
			for (int i = 0; i < NUM_EXPLOSIONS; i++) {
				String uniformName = String.format("ExplosionPositions[%d]", i);
				mExplosionPositionUniformLocation[i] =  mDensityShader.getUniformLocation(gl, uniformName);
			}
			mNumExplosionsUniformLocation = mDensityShader.getUniformLocation(gl, "NumExplosions");
			mExplosionRadiusUniformLocation = mDensityShader.getUniformLocation(gl, "ExplosionRadius");
			
			mDensityShader.bind(gl);
			gl.glUniform1i(mDensityShader.getUniformLocation(gl, "Permutation"), 0);
			gl.glUniform1i(mDensityShader.getUniformLocation(gl, "Gradient"), 1);
			mDensityShader.unbind(gl);

			mTerrainShader.bind(gl);
			gl.glUniform1i(mTerrainShader.getUniformLocation(gl, "TriTable"), 1);
			gl.glUniform1i(mTerrainShader.getUniformLocation(gl, "DensityFunction"), 0);
			mTerrainShader.unbind(gl);
			mTerrainLowerCornerUniformLocation = mTerrainShader.getUniformLocation(gl, "LowerCorner");
			mTerrainNumVoxelsUniformLocation = mTerrainShader.getUniformLocation(gl, "NumVoxels");
			mTerrainBlockSizeUniformLocation = mTerrainShader.getUniformLocation(gl, "BlockSize");
			
			OpenGLException.checkOpenGLError(gl);

			initialized = true;
		}
	}

	public void fillTexture3D(GL2 gl) throws OpenGLException {
		if (density_filled) {
			return;
		}
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


		//gl.glViewport(0, 0, NUM_VOXELS + 1, NUM_VOXELS + 1);

		gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

		blockDensityFunction.bind(gl);

		for (int i = 0; i <= numVoxels + 1; i++) {
			mDensityShader.bind(gl);
			PerlinNoise.bind(gl, 0, 1);
			gl.glUniform1i(mLayerDensityUniformLocation, i);
			gl.glUniform3f(mLowerCornerDensityUniformLocation, minPoint.x, minPoint.y, minPoint.z);
			gl.glUniform1f(mNumVoxelsDensityUniformLocation, numVoxels);
			gl.glUniform1f(mBlockSizeDensityUniformLocation, sideLength);
			gl.glUniform1i(mNumExplosionsUniformLocation, explosions.size());
			gl.glUniform1f(mExplosionRadiusUniformLocation, ExploreSceneController.EXPLOSION_RADIUS);
			for (int j = 0; j < explosions.size(); j++) {
				Point3f explosionPosition = explosions.get(j).getPosition();
				gl.glUniform3f(mExplosionPositionUniformLocation[j], 
						explosionPosition.x, explosionPosition.y, explosionPosition.z);
			}

			gl.glBegin(GL2.GL_TRIANGLES);
			gl.glVertex3d(1, 1, 0);
			gl.glVertex3d(-1, 1, 0);
			gl.glVertex3d(-1, -1, 0);
			gl.glVertex3d(-1, -1, 0);
			gl.glVertex3d(1, -1, 0);
			gl.glVertex3d(1, 1, 0);
			gl.glEnd();
			PerlinNoise.unbind(gl);
			mDensityShader.unbind(gl);
		}

		blockDensityFunction.unbind(gl);


		gl.glPopMatrix();

		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glPopMatrix();


		/* Restore active matrix. */
		gl.glPopAttrib();

		density_filled = true;
		blockDensityFunction.getTexture().getPixelData(gl);

	}

	public void renderPolygons(GL2 gl) throws OpenGLException {
		if (buffer_filled) {
			return;
		}
		if (num_polygons >= 0) {
			return; 
		}

		int queries[] = new int[1];
		int queryHandle;

		gl.glGenQueries(1, queries, 0);
		queryHandle = queries[0];

		mTerrainShader.bind(gl);
		OpenGLException.checkOpenGLError(gl);
		triTableTextureHandle.bind(gl,  1);
		OpenGLException.checkOpenGLError(gl);
		this.blockDensityFunction.getTexture().bind(gl, 0);
		OpenGLException.checkOpenGLError(gl);
		gl.glUniform3f(mTerrainLowerCornerUniformLocation, 
				this.minPoint.x, this.minPoint.y, this.minPoint.z);
		gl.glUniform1f(mTerrainNumVoxelsUniformLocation, numVoxels);
		gl.glUniform1f(mTerrainBlockSizeUniformLocation, sideLength);

		gl.glEnable(GL2.GL_RASTERIZER_DISCARD);
		buffer.bindTransformFeedback(gl);
		gl.glBeginQuery(GL2.GL_TRANSFORM_FEEDBACK_PRIMITIVES_WRITTEN, queryHandle);
		gl.glBeginTransformFeedback(GL2.GL_TRIANGLES);	

		gl.glBegin(GL2.GL_POINTS);

		for (int i = 0; i < numVoxels; i++) {
			for (int j = 0; j < numVoxels; j++) {
				for (int k = 0; k < numVoxels; k++) {
						gl.glVertex4i((k), (j), (i), 0);
				}
			}
		}


		gl.glEnd();

		gl.glEndTransformFeedback();
		OpenGLException.checkOpenGLError(gl);
		gl.glEndQuery(GL2.GL_TRANSFORM_FEEDBACK_PRIMITIVES_WRITTEN);
		buffer.bindTransformFeedback(gl);
		gl.glDisable(GL2.GL_RASTERIZER_DISCARD);
		triTableTextureHandle.unbind(gl);
		this.blockDensityFunction.getTexture().unbind(gl);
		mTerrainShader.unbind(gl);
		IntBuffer result = IntBuffer.allocate(1);
		gl.glGetQueryObjectuiv(queryHandle, GL2.GL_QUERY_RESULT, result);

		this.num_polygons = result.get(0);

		if (num_polygons == 0) {
			releaseGPUResources(gl);
		} else {
			buffer_filled = true;
		}

		/* Make sure nothing went wrong. */
		OpenGLException.checkOpenGLError(gl);


	}

	public List<Explosion> getExplosionPositions() {
		return explosions;
	}
	
	public Texture3D getTexture3D() {
		return blockDensityFunction.getTexture();
	}

	public void releaseGPUResources(GL2 gl) {
		if (buffer_filled) {
			buffer.releaseGPUResources(gl);
			buffer_filled = false;
		}
		buffer = null;
		if (density_filled) {
			this.blockDensityFunction.releaseGPUResources(gl);
			density_filled = false;
		}
		this.num_polygons = -1;
	}

	public void setup(GL2 gl) throws OpenGLException {
		buffer = new VertexBuffer(gl, this.numVoxels);
		blockDensityFunction = new FramebufferObject3D(gl, Texture3D.Format.RGBA,
				Texture3D.Datatype.FLOAT16, numVoxels + 1, numVoxels + 1, numVoxels + 2);
	}

	public void renderTerrain(GL2 gl) throws OpenGLException {
	
		if (num_polygons > 0) {
			if (!buffer_filled) {
				throw new OpenGLException("rendering empty buffer");
			}
			if (!density_filled) {
				throw new OpenGLException("rendering empty density");
			}
			
			buffer.bind(gl);
			gl.glEnableVertexAttribArray(0);
			gl.glVertexAttribPointer(0, 4, GL2.GL_FLOAT, false, 0, 0);
			gl.glDrawArrays(GL2.GL_TRIANGLES, 0, 12 * num_polygons);
			gl.glDisableVertexAttribArray(0);
			buffer.unbind(gl);

		}
		/* Make sure nothing went wrong. */
		OpenGLException.checkOpenGLError(gl);

	}

	public int getNumPolygons() {
		return num_polygons;
	}

	public int getNumVoxels() {
		return this.numVoxels;
	}


	public void testTexture(GL2 gl) throws OpenGLException {

		gl.glPushMatrix();
		gl.glTranslated(this.minPoint.x, this.minPoint.y, this.minPoint.z);
		double[][] verts = {{0.0, 0.0, 0.0}, {0.0, 1.0, 0.0}, {1.0, 1.0, 0.0}, {1.0, 0.0, 0.0}};
		gl.glBegin(GL2.GL_TRIANGLES);
		for (int x = 0; x <= 3; x++) {
			gl.glTexCoord3d(0.5, 0.5, 1.0);
			gl.glNormal3d(0.0, 0.0, 1.0);
			gl.glVertex3d(0.5 * TerrainRenderer.BLOCK_SIZE, 0.5 * TerrainRenderer.BLOCK_SIZE, 1.0 * TerrainRenderer.BLOCK_SIZE);
			gl.glTexCoord3d(verts[x][0], verts[x][1], verts[x][2]);
			gl.glNormal3d(verts[x][0], verts[x][1], verts[x][2]);
			gl.glVertex3d(verts[x][0] * TerrainRenderer.BLOCK_SIZE, verts[x][1] * TerrainRenderer.BLOCK_SIZE, verts[x][2] * TerrainRenderer.BLOCK_SIZE);
			gl.glTexCoord3d(verts[(x+1)%4][0], verts[(x+1)%4][1], verts[(x+1)%4][2]);
			gl.glNormal3d(verts[(x+1)%4][0], verts[(x+1)%4][1], verts[(x+1)%4][2]);
			gl.glVertex3d(verts[(x+1)%4][0] * TerrainRenderer.BLOCK_SIZE, verts[(x+1)%4][1] * TerrainRenderer.BLOCK_SIZE, verts[(x+1)%4][2] * TerrainRenderer.BLOCK_SIZE);
		}
		gl.glEnd();

		gl.glPopMatrix();

		/* Make sure nothing went wrong. */
		OpenGLException.checkOpenGLError(gl);

	}
	
	

	

}
