package cs5625.deferred.rendering;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.glu.GLU;
import javax.vecmath.AxisAngle4f;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import javax.vecmath.Quat4f;

import cs5625.deferred.materials.Material;
import cs5625.deferred.materials.Texture.Datatype;
import cs5625.deferred.materials.Texture.Format;
import cs5625.deferred.materials.UnshadedMaterial;
import cs5625.deferred.misc.OpenGLException;
import cs5625.deferred.misc.PerlinNoise;
import cs5625.deferred.misc.ScenegraphException;
import cs5625.deferred.misc.Util;
import cs5625.deferred.particles.SmokeSystem;
import cs5625.deferred.scenegraph.Geometry;
import cs5625.deferred.scenegraph.Light;
import cs5625.deferred.scenegraph.Mesh;
import cs5625.deferred.scenegraph.PointLight;
import cs5625.deferred.scenegraph.SceneObject;
import cs5625.deferred.scenegraph.TerrainBlockRenderer;
import cs5625.deferred.scenegraph.TerrainRenderer;

/**
 * Renderer.java
 * 
 * The Renderer class is in charge of rendering a scene using deferred shading. This happens in 4 stages, 
 * described below. In this description, numbers in {curly braces} indicate g-buffer texture indices.
 * 
 * 1. Render into gbuffer {0 = diffuse/normal.x, 1 = position/normal.y, 2&3 = material info} of each fragment.
 * 2. Render into gbuffer {4 = gradients} based on the positions and normals in 0&1, for edge detection.
 * 3. Render into gbuffer {5 = shaded scene} the final opaque scene, using all previous buffers.
 * 4. Output {5} to window.
 * 
 * Note that the eyespace normal is stored in a compressed form. The alpha values of the diffuse and position
 * buffers are the x and y values of the compressed normal. The algorithm used to do this is from Cry Engine 3.
 * Source: Mittring, M. "A bit more deferred - CryEngine3." Triangle Game Conference 2009.
 * 
 * Written for Cornell CS 5625 (Interactive Computer Graphics).
 * Copyright (c) 2012, Computer Science Department, Cornell University.
 * 
 * @author Asher Dunn (ad488), Sean Ryan (ser99), Ivaylo Boyadzhiev (iib2), John DeCorato (jd537)
 * @date 2013-01-29
 */
public class Renderer
{
	/* Viewport attributes. */
	protected float mViewportWidth, mViewportHeight;
	
	/* The GBuffer FBO. */
	protected FramebufferObject mGBufferFBO;
	
	/* Name the indices in the GBuffer so code is easier to read. */
	protected final int GBuffer_DiffuseIndex = 0;
	protected final int GBuffer_PositionIndex = 1;
	protected final int GBuffer_MaterialIndex1 = 2;
	protected final int GBuffer_MaterialIndex2 = 3;
	protected final int GBuffer_GradientsIndex = 4;
	protected final int GBuffer_SSAOIndex = 5;
	protected final int GBuffer_ParticleIndex = 6;
	protected final int GBuffer_FinalSceneIndex = 7;
	protected final int GBuffer_Count = 8;
	
	/* The index of the texture to preview in GBufferFBO, or -1 for no preview. */
	protected int mPreviewIndex = -1;
	
	/* List of lights in the scene, assembled every frame. */
	private ArrayList<Light> mLights = new ArrayList<Light>();
	
	/* Cache of shaders used by all the materials in the scene. Storing the shaders here instead of in 
	 * the Material classes themselves allows the shaders to be local to the renderer and the OpenGL 
	 * context, which is appropriate. */
	private HashMap<Class<? extends Material>, ShaderProgram> mShaderCache = new HashMap<Class<? extends Material>, ShaderProgram>();

	/* The "ubershader" used for performing deferred shading on the gbuffer, 
	 * and the silhouette shader to compute edges for toon rendering. */
	private ShaderProgram mUberShader;
	
	/* Material for rendering generic wireframes and crease edges, and flag to enable/disable that. */
	private Material mWireframeMaterial, mWireframeMarkedEdgeMaterial;
	private boolean mRenderWireframes = false;

	
	/* Used to control gbuffer data vizualization. */
	private ShaderProgram mVisShader = null;
	
	/* Locations of uniforms in the ubershader. */
	private int mLightPositionsUniformLocation = -1;
	private int mLightColorsUniformLocation = -1;
	private int mLightAttenuationsUniformLocation = -1;
	private int mNumLightsUniformLocation = -1;
	
	
	/* The size of the light uniform arrays in the ubershader. 
	 * This is queried directly from the shader file. */
	private int mMaxLightsInUberShader;
	
	
	/*
	 * Shaders and parameters for the different particle effects.  
	 * It was not straightforward to abstract these away to some other
	 * class- there is too much overlap, so here it is, hardcoded in the 
	 * renderer.
	 */
	ShaderProgram mSmokeShader;
	private int mSmokeEnableSoftParticlesLocation = -1, mSmokeNearPlaneLocation = -1, mSmokeTauLocation = -1;
	private int mSmokeEnableSoftParticles = 1;
	
	
	boolean mRenderingOpaque = true;
	
	
	
	/**
	 * Renders a single frame of the scene. This is the main method of the Renderer class.
	 * 
	 * @param drawable The drawable to render into.
	 * @param sceneRoot The root node of the scene to render.
	 * @param camera The camera describing the perspective to render from.
	 * @throws IOException 
	 * @throws OpenGLException 
	 */
	public void render(GLAutoDrawable drawable, SceneObject sceneRoot, Camera camera) 
	{
		GL2 gl = drawable.getGL().getGL2();	

	
		
		try
		{
			/* Reset lights array. It will be re-filled as the scene is traversed. */
			mLights.clear();
			
			/* 1. Fill the gbuffer given this scene and camera. */ 
			fillGBuffer(gl, sceneRoot, camera);
			
			/* 2. Fill the particle buffer, utilizing the depth values aquired by filling
			 * the GBuffer */
			fillSmokeBuffer(gl, sceneRoot, camera);
			
			/* 3. Apply deferred lighting to the g-buffer. At this point, the opaque scene has been rendered. */
			lightGBuffer(gl, camera);

			/* 4. If we're supposed to preview one gbuffer texture, do that now. 
			 *    Otherwise, envoke the final render pass (optional post-processing). */
			if (mPreviewIndex >= 0 && mPreviewIndex < GBuffer_FinalSceneIndex)
			{
				Util.renderTextureFullscreen(gl, mGBufferFBO.getColorTexture(mPreviewIndex));
			}
			else
			{			
				finalPass(gl);					 								 
			}
		} catch (Exception err) {
				/* If an error occurs in all that, print it, but don't kill the whole program. */
			err.printStackTrace();
		}
	}
	
	
	/**
	 * All post-processing should be done in this method.
	 * If no post-processing is required it should display the final scene buffer.
	 * 
	 * @param gl The OpenGL state
	 */
	protected void finalPass(GL2 gl) throws OpenGLException
	{
		if (mPreviewIndex >= 6 && mPreviewIndex <= 8)
		{
			/* The keys '7', '8', and '9' correspond to gbuffer data visualization. */
			/* Save state before we disable depth testing for blitting. */
			gl.glPushAttrib(GL2.GL_ENABLE_BIT);
			
			/* Disable depth test and blend, since we just want to replace the contents of the framebuffer.
			 * Since we are rendering an opaque fullscreen quad here, we don't bother clearing the buffer
			 * first. */
			gl.glDisable(GL2.GL_DEPTH_TEST);
			gl.glDisable(GL2.GL_BLEND);
			
			/* Bind the first four sections of the gbuffer. */
			mGBufferFBO.getColorTexture(GBuffer_DiffuseIndex).bind(gl, 0);
			mGBufferFBO.getColorTexture(GBuffer_PositionIndex).bind(gl, 1);
			mGBufferFBO.getColorTexture(GBuffer_MaterialIndex1).bind(gl, 2);
			mGBufferFBO.getColorTexture(GBuffer_MaterialIndex2).bind(gl, 3);
			mGBufferFBO.getColorTexture(GBuffer_ParticleIndex).bind(gl, 4);
			
			/* Set the vis mode using the preview index. */
			mVisShader.bind(gl);
			gl.glUniform1i(mVisShader.getUniformLocation(gl, "VisMode"), mPreviewIndex - 6);
			
			/* Draw a full-screen quad to the framebuffer. */
			Util.drawFullscreenQuad(gl, mViewportWidth, mViewportHeight);
			
			/* Unbind everything. */
			mVisShader.unbind(gl);
			mGBufferFBO.getColorTexture(GBuffer_DiffuseIndex).unbind(gl);
			mGBufferFBO.getColorTexture(GBuffer_PositionIndex).unbind(gl);
			mGBufferFBO.getColorTexture(GBuffer_MaterialIndex1).unbind(gl);
			mGBufferFBO.getColorTexture(GBuffer_MaterialIndex2).unbind(gl);
			mGBufferFBO.getColorTexture(GBuffer_ParticleIndex).unbind(gl);

			/* Restore attributes (blending and depth-testing) to as they were before. */
			gl.glPopAttrib();
			
			/* Make sure nothing went wrong. */
			OpenGLException.checkOpenGLError(gl);
		}
		else
		{
			/* No post-processing is required; just display the unaltered scene. */
			Util.renderTextureFullscreen(gl, mGBufferFBO.getColorTexture(GBuffer_FinalSceneIndex));
		
			
			
		}
	}
	
	/**
	 * Clears the gbuffer and renders scene objects.
	 *
	 * @param gl The OpenGL state
	 * @param sceneRoot The root node of the scene to render.
	 * @param camera The camera describing the perspective to render from.
	 */
	private void fillGBuffer(GL2 gl, SceneObject sceneRoot, Camera camera) throws OpenGLException
	{
		if (mRenderingOpaque) {
			/* First, bind and clear the gbuffer. */
			mGBufferFBO.bindSome(gl, new int[]{GBuffer_DiffuseIndex, GBuffer_PositionIndex, GBuffer_MaterialIndex1, GBuffer_MaterialIndex2});
			
			gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
			gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
		}
		
		/* Update the projection matrix with this camera's projection matrix. */
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		
		GLU glu = GLU.createGLU(gl);
		glu.gluPerspective(camera.getFOV(), mViewportWidth / mViewportHeight, camera.getNear(), camera.getFar());
		
		/* Update the modelview matrix with this camera's eye transform. */
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();
		
		/* Find the inverse of the camera scale, position, and orientation in world space, accounting
		 * for the fact that the camera might be nested inside other objects in the scenegraph.*/
		float cameraScale = 1.0f / camera.transformDistanceToWorldSpace(1.0f);
		Point3f cameraPosition = camera.transformPointToWorldSpace(new Point3f(0.0f, 0.0f, 0.0f));
		AxisAngle4f cameraOrientation = new AxisAngle4f();
		cameraOrientation.set(camera.transformOrientationToWorldSpace(new Quat4f(0.0f, 0.0f, 0.0f, 1.0f)));
		
		/* Apply the camera transform to OpenGL. */
		gl.glScalef(cameraScale, cameraScale, cameraScale);
		gl.glRotatef(cameraOrientation.angle * 180.0f / (float)Math.PI, -cameraOrientation.x, -cameraOrientation.y, -cameraOrientation.z);
		gl.glTranslatef(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
		
		/* Check for errors before rendering, to help isolate. */
		OpenGLException.checkOpenGLError(gl);
		
		/* Render the scene. */
		try {
			renderObject(gl, camera, sceneRoot);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("ERROR");
		}

		/* GBuffer is filled, so unbind it. */
		mGBufferFBO.unbind(gl);
		
		
		/* Check for errors after rendering, to help isolate. */
		OpenGLException.checkOpenGLError(gl);
	}
	
	public void fillSmokeBuffer(GL2 gl, SceneObject sceneRoot, Camera camera) throws OpenGLException {
		// Bind the target particle buffer
		// Pass through camera parameters
		// Render the particle systems in the scene graph
		gl.glPushAttrib(GL2.GL_ENABLE_BIT);
		
		/* Bind the SSAO buffer as output. */
		mGBufferFBO.bindOne(gl, GBuffer_ParticleIndex);

		gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		gl.glClear(GL2.GL_COLOR_BUFFER_BIT);
		
		/* Save state before we disable depth testing for blitting. */
		
		/* Expect pre-multiplied alpha from the shader. This allows us to support both
         * several types of blending in a single pass:
         *
         *       no blending: gl_FragColor = vec4(color, 1.0);
         *    alpha blending: gl_FragColor = vec4(color * alpha, alpha);
         * additive blending: gl_FragColor = vec4(color, 0.0);
         *
         * The default particle shader uses alpha blending, but you can subclass ParticleMaterial
         * and write your own shader; if it follows this alpha convention it will "just work".
         */
        gl.glBlendFunc(GL2.GL_ONE, GL2.GL_ONE_MINUS_SRC_ALPHA);
        gl.glEnable(GL2.GL_BLEND);

        /* Disable writing of depth values by these particles. They will still clip against the
         * opaque scene geometry, but not against other particles. */
        gl.glDepthMask(false);
		
		mGBufferFBO.getColorTexture(GBuffer_DiffuseIndex).bind(gl, 0);
		mGBufferFBO.getColorTexture(GBuffer_PositionIndex).bind(gl, 1);
		
		/* We need to disable interpolation on the g-buffer, otherwise we get ringing artifacts!
		 * This won't reduce the quality of our ssao because the g-buffer is full resolution. */
		//mGBufferFBO.getColorTexture(GBuffer_DiffuseIndex).enableInterpolation(gl, false);
		//mGBufferFBO.getColorTexture(GBuffer_PositionIndex).enableInterpolation(gl, false);
		
		/* Bind the SSAO shader and update the uniforms. */
		mSmokeShader.bind(gl);
		
		gl.glUniform1i(mSmokeEnableSoftParticlesLocation, mSmokeEnableSoftParticles);		// TODO: FIND THESE VALUES
		
		
		/* Render. */
		drawSmokeSystem(gl, sceneRoot, camera);
		
		/* Unbind everything. */
		mSmokeShader.unbind(gl);
		
		/* Re-enable interpolation. */
		mGBufferFBO.getColorTexture(GBuffer_DiffuseIndex).enableInterpolation(gl, true);
		mGBufferFBO.getColorTexture(GBuffer_PositionIndex).enableInterpolation(gl, true);
		
		mGBufferFBO.getColorTexture(GBuffer_DiffuseIndex).unbind(gl);
		mGBufferFBO.getColorTexture(GBuffer_PositionIndex).unbind(gl);

		mGBufferFBO.unbind(gl);

		/* Restore attributes (blending and depth-testing) to as they were before. */
		gl.glPopAttrib();
	}
	public void drawSmokeSystem(GL2 gl, SceneObject obj, Camera camera) throws OpenGLException {
		/* If the object is not visible, we skip the rendition of it and all its children */
		if (!obj.isVisible()) {
			return;
		}
		gl.glPushAttrib(GL2.GL_ALL_ATTRIB_BITS);
		gl.glPushClientAttrib((int)GL2.GL_CLIENT_ALL_ATTRIB_BITS);
		
		/* Save matrix before applying this object's transformation. */
		gl.glPushMatrix();
		
		/* Get this object's transformation. */
		float scale = obj.getScale();
		Point3f position = obj.getPosition();
		AxisAngle4f orientation = new AxisAngle4f();
		orientation.set(obj.getOrientation());
		
		/* Apply this object's transformation. */
		gl.glTranslatef(position.x, position.y, position.z);
		gl.glRotatef(orientation.angle * 180.0f / (float)Math.PI, orientation.x, orientation.y, orientation.z);
		gl.glScalef(scale, scale, scale);
		
		/* Render this object as appropriate for its type. */
		if (obj instanceof SmokeSystem)
		{
			// Render smoke particles			
			//gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
			//gl.glVertexPointer(3, GL2.GL_FLOAT, 0, ((SmokeSystem)obj).getVertexData());
			//gl.glEnableClientState(GL2.GL_COLOR_ARRAY);
			//gl.glColorPointer(3, GL2.GL_FLOAT, 0, ((SmokeSystem)obj).getNormalData());
			OpenGLException.checkOpenGLError(gl);
			
			
			gl.glBegin(GL2.GL_POINTS);
			for (Point3f r : ((SmokeSystem)obj).getParticlePositions()) {
				gl.glVertex3d(r.x, r.y, r.z);
			}
			gl.glEnd(); 
			
			//gl.glDrawElements(GL2.GL_POINTS, 
			//		  ((SmokeSystem)obj).getVertexCount(),	//mesh.getVerticesPerPolygon() * mesh.getPolygonCount() 
			//		  GL2.GL_UNSIGNED_INT, 
			//		  ((SmokeSystem)obj).getPolygonData());
			//gl.glDrawArrays(GL2.GL_POINTS, 0, ((SmokeSystem)obj).getVertexCount());
			
			OpenGLException.checkOpenGLError(gl);
			//gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
			//gl.glDisableClientState(GL2.GL_COLOR_ARRAY);
		}
		
		/* Render this object's children. */
		for (SceneObject child : obj.getChildren())
		{
			drawSmokeSystem(gl, child, camera);
		}
		
		/* Restore transformation matrix and check for errors. */
		gl.glPopMatrix();
		
		gl.glPopClientAttrib();
		gl.glPopAttrib();
		
		OpenGLException.checkOpenGLError(gl);
	}
	
	/**
	 * Applies lighting to an already-filled gbuffer to produce the final scene. Output is sent 
	 * to the main framebuffer of the view/window.
	 * 
	 * @param gl The OpenGL state.
	 * @param camera Camera from whose perspective we are rendering.
	 */
	private void lightGBuffer(GL2 gl, Camera camera) throws OpenGLException, ScenegraphException
	{
		/* Need some lights, otherwise it will just be black! */
		if (mLights.size() == 0)
		{
			throw new ScenegraphException("Must have at least one light in the scene!");
		}
		
		/* Can't have more lights than the shader supports. */
		if (mLights.size() > mMaxLightsInUberShader)
		{
			throw new ScenegraphException(mLights.size() + " is too many lights; ubershader only supports " + mMaxLightsInUberShader + ".");
		}
		
		/* Bind final scene buffer as output target for this pass. */
		mGBufferFBO.bindOne(gl, GBuffer_FinalSceneIndex);
		
		/* Save state before we disable depth testing for blitting. */
		gl.glPushAttrib(GL2.GL_ENABLE_BIT);
		
		/* Disable depth test and blend, since we just want to replace the contents of the framebuffer.
		 * Since we are rendering an opaque fullscreen quad here, we don't bother clearing the buffer
		 * first. */
		gl.glDisable(GL2.GL_DEPTH_TEST);
		gl.glDisable(GL2.GL_BLEND);
		
		/* Bind all GBuffer source textures so the ubershader can read them. */
		for (int i = 0; i < GBuffer_FinalSceneIndex; ++i)
		{
			mGBufferFBO.getColorTexture(i).bind(gl, i);
		}
		
		/* Bind ubershader. */
		mUberShader.bind(gl);

		/* Update all the ubershader uniforms with up-to-date light information. */
		for (int i = 0; i < mLights.size(); ++i)
		{
			/* Transform each light position to eye space. */
			Light light = mLights.get(i);
			Point3f eyespacePosition = camera.transformPointFromWorldSpace(light.transformPointToWorldSpace(new Point3f()));
			
			/* Send light color and eyespace position to the ubershader. */
			gl.glUniform3f(mLightPositionsUniformLocation + i, eyespacePosition.x, eyespacePosition.y, eyespacePosition.z);
			gl.glUniform3f(mLightColorsUniformLocation + i, light.getColor().x, light.getColor().y, light.getColor().z);
			
			if (light instanceof PointLight)
			{
				gl.glUniform3f(mLightAttenuationsUniformLocation + i, 
						((PointLight)light).getConstantAttenuation(), 
						((PointLight)light).getLinearAttenuation(), 
						((PointLight)light).getQuadraticAttenuation());
			}
			else
			{
				gl.glUniform3f(mLightAttenuationsUniformLocation + i, 1.0f, 0.0f, 0.0f);
			}
		}
		
		/* Ubershader needs to know how many lights. */
		gl.glUniform1i(mNumLightsUniformLocation, mLights.size());
	
		/* Let there be light! */
		Util.drawFullscreenQuad(gl, mViewportWidth, mViewportHeight);
		
		/* Unbind everything. */
		mUberShader.unbind(gl);
		
		for (int i = 0; i < GBuffer_FinalSceneIndex; ++i)
		{
			mGBufferFBO.getColorTexture(i).unbind(gl);
		}

		/* Unbind rendering target. */
		mGBufferFBO.unbind(gl);

		/* Restore attributes (blending and depth-testing) to as they were before. */
		gl.glPopAttrib();
	}
	
	/**
	 * Renders a scenegraph node and its children.
	 * 
	 * @param gl The OpenGL state.
	 * @param camera The camera rendering the scene.
	 * @param obj The object to render. If this is a Geometry object, its meshes are rendered.
	 *        If this is a Light object, it is added to the list of lights. Other objects are ignored.
	 * @param dcm The dynamic cub map that we are rendering to. If it is not equal to null, we check to
	 *        see if the object being rendered is the same as the one stored in the DCM. If it is, then
	 *        we don't render it.
	 * @throws IOException 
	 */
	private void renderObject(GL2 gl, Camera camera, SceneObject obj) throws OpenGLException, IOException
	{
		/* If the object is not visible, we skip the rendition of it and all its children */
		if (!obj.isVisible()) {
			return;
		}
		
		/* Save matrix before applying this object's transformation. */
		gl.glPushMatrix();
		
		/* Get this object's transformation. */
		float scale = obj.getScale();
		Point3f position = obj.getPosition();
		AxisAngle4f orientation = new AxisAngle4f();
		orientation.set(obj.getOrientation());
		
		/* Apply this object's transformation. */
		gl.glTranslatef(position.x, position.y, position.z);
		gl.glRotatef(orientation.angle * 180.0f / (float)Math.PI, orientation.x, orientation.y, orientation.z);;
		if (obj instanceof TerrainRenderer && mRenderingOpaque) {
			((TerrainRenderer) obj).setupPosition(gl, camera);
			((TerrainRenderer) obj).renderPolygons(gl, camera);
			((TerrainRenderer) obj).getMaterial().retrieveShader(gl, mShaderCache);
			((TerrainRenderer) obj).renderTerrain(gl);
		}
		
		/* Render this object as appropriate for its type. */
		else if (obj instanceof Geometry)
		{
			for (Mesh mesh : ((Geometry)obj).getMeshes())
			{
				if (mRenderingOpaque==mesh.isOpaque())
					renderMesh(gl, mesh, camera);
			}
		}
		else if (obj instanceof Light && mRenderingOpaque)
		{
			mLights.add((Light)obj);
		}
		
		/* Render this object's children. */
		for (SceneObject child : obj.getChildren())
		{
			renderObject(gl, camera, child);
		}
		
		/* Restore transformation matrix and check for errors. */
		gl.glPopMatrix();
		OpenGLException.checkOpenGLError(gl);
	}

	/**
	 * Renders a single trimesh.
	 * 
	 * @param gl The OpenGL state.
	 * @param mesh The mesh to render.
	 */
	private void renderMesh(GL2 gl, Mesh mesh, Camera camera) throws OpenGLException
	{
		/* Save all state to isolate any changes made by this mesh's material. */
		gl.glPushAttrib(GL2.GL_ALL_ATTRIB_BITS);
		gl.glPushClientAttrib((int)GL2.GL_CLIENT_ALL_ATTRIB_BITS);
		
		/* Activate the material. */
		mesh.getMaterial().retrieveShader(gl, mShaderCache);
		mesh.getMaterial().bind(gl, camera);
		
			
		/* Enable the required vertex arrays and send data. */
		if (mesh.getVertexData() == null)
		{
			throw new OpenGLException("Mesh must have non-null vertex data to render!");
		}
		else
		{
			gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
			gl.glVertexPointer(3, GL2.GL_FLOAT, 0, mesh.getVertexData());
		}

		if (mesh.getNormalData() == null)
		{
			gl.glDisableClientState(GL2.GL_NORMAL_ARRAY);
		}
		else
		{
			gl.glEnableClientState(GL2.GL_NORMAL_ARRAY);
			gl.glNormalPointer(GL2.GL_FLOAT, 0, mesh.getNormalData());
		}
		
		if (mesh.getTexCoordData() == null)
		{
			gl.glDisableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
		}
		else
		{
			gl.glEnableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
			gl.glTexCoordPointer(2, GL2.GL_FLOAT, 0, mesh.getTexCoordData());
		}

		/* Send custom vertex attributes (if any) to OpenGL. */
		bindRequiredMeshAttributes(gl, mesh);
		bindRequiredMeshFBOs(gl, mesh);
		
		/* Render polygons. */
		gl.glDrawElements(getOpenGLPrimitiveType(mesh.getVerticesPerPolygon()), 
						  mesh.getVerticesPerPolygon() * mesh.getPolygonCount(), 
						  GL2.GL_UNSIGNED_INT, 
						  mesh.getPolygonData());
		
		unbindRequiredMeshFBOs(gl, mesh);
		
		/* Deactivate material and restore state. */
		mesh.getMaterial().unbind(gl);

		
		/* Render mesh wireframe if we're supposed to. */
		if (mRenderWireframes && mesh.getVerticesPerPolygon() > 2)
		{
			mWireframeMaterial.retrieveShader(gl, mShaderCache);
			mWireframeMaterial.bind(gl);

			gl.glLineWidth(1.0f);
			gl.glPolygonOffset(0.0f, 1.0f);
			gl.glEnable(GL2.GL_POLYGON_OFFSET_LINE);
			gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_LINE);

			/* Render polygons. */
			gl.glDrawElements(getOpenGLPrimitiveType(mesh.getVerticesPerPolygon()), 
					  mesh.getVerticesPerPolygon() * mesh.getPolygonCount(), 
					  GL2.GL_UNSIGNED_INT, 
					  mesh.getPolygonData());					
			
			mWireframeMaterial.unbind(gl);
		}

		/* Render marked edges (e.g. for subdiv creases), if we're supposed to and if they exist. */
		if (mRenderWireframes && mesh.getEdgeData() != null)
		{
			mWireframeMarkedEdgeMaterial.retrieveShader(gl, mShaderCache);
			mWireframeMarkedEdgeMaterial.bind(gl);

			gl.glLineWidth(5.0f);
			gl.glPolygonOffset(0.0f, 1.0f);
			gl.glEnable(GL2.GL_POLYGON_OFFSET_LINE);
			gl.glDrawElements(GL2.GL_LINES, mesh.getEdgeData().capacity(), GL2.GL_UNSIGNED_INT, mesh.getEdgeData());
			
			mWireframeMarkedEdgeMaterial.unbind(gl);
		}
		
		gl.glPopClientAttrib();
		gl.glPopAttrib();
		
		/* Check for errors. */
		OpenGLException.checkOpenGLError(gl);
	}
	
	/**
	 * Binds all custom vertex attributes required by a mesh's material to buffers provided
	 * by that mesh.
	 * 
	 * @param gl The OpenGL state.
	 * @param mesh All custom vertex attributes required by mesh's material and shader are bound to the 
	 *        correspondingly-named buffers in the mesh's `vertexAttribData` map.
	 *        
	 * @throws OpenGLException If a required attribute isn't supplied by the mesh.
	 */
	void bindRequiredMeshAttributes(GL2 gl, Mesh mesh) throws OpenGLException {
		ShaderProgram shader = mesh.getMaterial().getShaderProgram();
		bindAttributes(gl, shader, mesh);
	}
	void bindAttributes(GL2 gl, ShaderProgram shader, Attributable att) throws OpenGLException
	{
		HashMap<String, FloatBuffer> attribs = att.getVertexAttribData();
		for (String attrib : att.getRequiredVertexAttributes())
		{
			/* Ignore attributes which aren't actually used in the shader. */
			int location = shader.getAttribLocation(gl, attrib);
			if (location < 0)
			{
				continue;
			}
			
			/* Get data for this attribute from the mesh. */
			FloatBuffer attribData = attribs.get(attrib);
			
			/* This attribute is required, so throw an exception if the mesh doesn't supply it. */
			if (attribData == null)
			{
				throw new OpenGLException("Material requires vertex attribute '" + attrib + "' which is not present in mesh's vertexAttribData.");
			}
			else
			{
				gl.glEnableVertexAttribArray(location);
				System.out.println(attribData.capacity());
				gl.glVertexAttribPointer(location, attribData.capacity() / att.getVertexCount(), GL2.GL_FLOAT, false, 0, attribData);
			}
		}
	}
	void bindRequiredMeshFBOs(GL2 gl, Mesh mesh) throws OpenGLException {
		HashMap<String, Integer> fbos = mesh.getMaterial().getRequiredFBOs();
		for (String fbo : fbos.keySet()) {
			if (fbo.equals("Diffuse")) {
				mGBufferFBO.getColorTexture(GBuffer_DiffuseIndex).bind(gl, fbos.get(fbo));
			} else if (fbo.equals("Position")){
				mGBufferFBO.getColorTexture(GBuffer_PositionIndex).bind(gl, fbos.get(fbo));
			} else if (fbo.equals("Material1")){
				mGBufferFBO.getColorTexture(GBuffer_MaterialIndex1).bind(gl, fbos.get(fbo));
			} else if (fbo.equals("Material2")){
				mGBufferFBO.getColorTexture(GBuffer_MaterialIndex2).bind(gl, fbos.get(fbo));
			} else if (fbo.equals("Gradients")) {
				mGBufferFBO.getColorTexture(GBuffer_GradientsIndex).bind(gl, fbos.get(fbo));
			}
		}
	}
	void unbindRequiredMeshFBOs(GL2 gl, Mesh mesh) throws OpenGLException {
		HashMap<String, Integer> fbos = mesh.getMaterial().getRequiredFBOs();
		for (String fbo : fbos.keySet()) {
			if (fbo.equals("Diffuse")) {
				mGBufferFBO.getColorTexture(GBuffer_DiffuseIndex).unbind(gl);
			} else if (fbo.equals("Position")){
				mGBufferFBO.getColorTexture(GBuffer_PositionIndex).unbind(gl);
			} else if (fbo.equals("Material1")){
				mGBufferFBO.getColorTexture(GBuffer_MaterialIndex1).unbind(gl);
			} else if (fbo.equals("Material2")){
				mGBufferFBO.getColorTexture(GBuffer_MaterialIndex2).unbind(gl);
			} else if (fbo.equals("Gradients")) {
				mGBufferFBO.getColorTexture(GBuffer_GradientsIndex).unbind(gl);
			}
		}
	}

	/**
	 * Returns the OpenGL primitive type for the given size of polygon (e.g. GL_TRIANGLES for 3).
	 * @throws OpenGLException For values not in {1, 2, 3, 4}.
	 */
	private int getOpenGLPrimitiveType(int verticesPerPolygon) throws OpenGLException
	{
		switch (verticesPerPolygon)
		{
		case 1: return GL2.GL_POINTS;
		case 2: return GL2.GL_LINES;
		case 3: return GL2.GL_TRIANGLES;
		case 4: return GL2.GL_QUADS;
		default: throw new OpenGLException("Don't know how to render mesh with " + verticesPerPolygon + " vertices per polygon.");
		}
	}
	
	/**
	 * Requests that the renderer should render a preview of the indicated gbuffer texture, instead of the final shaded scene.
	 * 
	 * @param bufferIndex The index of the texture to preview. If `bufferIndex` is out of range (less than 0 or greater than
	 *        the index of the last gbuffer texture), the preview request will be ignored, and the renderer will render a 
	 *        shaded scene.
	 */
	public void previewGBuffer(int bufferIndex)
	{
		mPreviewIndex = bufferIndex;
	}

	/**
	 * Cancels a preview request made with `previewGBuffer()`, causing the renderer to render the final shaded scene when it renders.
	 */
	public void unpreviewGBuffer()
	{
		mPreviewIndex = -1;
	}
	
	/**
	 * Enables or disables rendering of mesh edges.
	 * 
	 * All edges are rendered in thin grey wireframe, and marked edges (e.g. creases) are rendered in thick pink. 
	 */
	public void setRenderWireframes(boolean wireframe)
	{
		mRenderWireframes = wireframe;
	}
	
	/**
	 * Returns true if mesh edges are being rendered.
	 */
	public boolean getRenderWireframes()
	{
		return mRenderWireframes;
	}

	
	/**

	/**
	 * Performs one-time initialization of OpenGL state and shaders used by this renderer.
	 * @param drawable The OpenGL drawable this renderer will be rendering to.
	 */
	public void init(GLAutoDrawable drawable)
	{
		GL2 gl = drawable.getGL().getGL2();
		
		/* Enable depth testing. */
		gl.glEnable(GL2.GL_DEPTH_TEST);
		gl.glDepthFunc(GL2.GL_LEQUAL);

		try
		{
			TerrainBlockRenderer.initializeTerrain(gl);
			PerlinNoise.init(gl);
			
			
			/* Load the ubershader. */
			mUberShader = new ShaderProgram(gl, "shaders/ubershader");

			/* Set material buffer indices once here, since they never have to change. */
			mUberShader.bind(gl);
			gl.glUniform1i(mUberShader.getUniformLocation(gl, "DiffuseBuffer"), 0);
			gl.glUniform1i(mUberShader.getUniformLocation(gl, "PositionBuffer"), 1);
			gl.glUniform1i(mUberShader.getUniformLocation(gl, "MaterialParams1Buffer"), 2);
			gl.glUniform1i(mUberShader.getUniformLocation(gl, "MaterialParams2Buffer"), 3);
			gl.glUniform1i(mUberShader.getUniformLocation(gl, "SilhouetteBuffer"), 4);
			gl.glUniform1i(mUberShader.getUniformLocation(gl, "SSAOBuffer"), 5);
			gl.glUniform1i(mUberShader.getUniformLocation(gl, "ParticleBuffer"), 6);
			gl.glUniform3f(mUberShader.getUniformLocation(gl, "SkyColor"), 0.3f, 0.3f, 0.3f);
			mUberShader.unbind(gl);			
			
			/* Get locations of the lighting uniforms, since these will have to be updated every frame. */
			mLightPositionsUniformLocation = mUberShader.getUniformLocation(gl, "LightPositions");
			mLightColorsUniformLocation = mUberShader.getUniformLocation(gl, "LightColors");
			mLightAttenuationsUniformLocation = mUberShader.getUniformLocation(gl, "LightAttenuations");
			mNumLightsUniformLocation = mUberShader.getUniformLocation(gl, "NumLights");

			/* Get the maximum number of lights the shader supports. */
			mMaxLightsInUberShader = mUberShader.getUniformArraySize(gl, "LightPositions");
			
			
			
			
			/* Load the visualization shader. */
			mVisShader = new ShaderProgram(gl, "shaders/visualize");
			
			mVisShader.bind(gl);
			gl.glUniform1i(mVisShader.getUniformLocation(gl, "DiffuseBuffer"), 0);
			gl.glUniform1i(mVisShader.getUniformLocation(gl, "PositionBuffer"), 1);
			gl.glUniform1i(mVisShader.getUniformLocation(gl, "MaterialParams1Buffer"), 2);
			gl.glUniform1i(mVisShader.getUniformLocation(gl, "MaterialParams2Buffer"), 3);
			mVisShader.unbind(gl);
			
			
		
		    
			/* Load the material used to render mesh edges (e.g. creases for subdivs). */
			mWireframeMaterial = new UnshadedMaterial(new Color3f(0.8f, 0.8f, 0.8f));
			mWireframeMarkedEdgeMaterial = new UnshadedMaterial(new Color3f(1.0f, 0.0f, 1.0f));
			
			
			
			
			mSmokeShader = new ShaderProgram(gl, "shaders/soft_particles", true, GL2.GL_POINTS, GL2.GL_TRIANGLE_STRIP, 4);
			
			mSmokeShader.bind(gl);
			gl.glUniform1i(mSmokeShader.getUniformLocation(gl, "DiffuseBuffer"), 0);
			gl.glUniform1i(mSmokeShader.getUniformLocation(gl, "PositionBuffer"), 1);
			mSmokeShader.unbind(gl);
			mSmokeEnableSoftParticlesLocation = mSmokeShader.getUniformLocation(gl, "EnableSoftParticles");
			mSmokeNearPlaneLocation = mSmokeShader.getUniformLocation(gl, "NearPlane");
			mSmokeTauLocation = mSmokeShader.getUniformLocation(gl, "Tau");
			
			/* Make sure nothing went wrong. */
			OpenGLException.checkOpenGLError(gl);
		}
		catch (Exception err)
		{
			/* If something did go wrong, we can't render - so just die. */
			err.printStackTrace();
			System.exit(-1);
		}
	}

	/**
	 * Called whenever the OpenGL context changes size. This renderer resizes the gbuffer 
	 * so it's always the same size as the viewport.
	 * 
	 * @param drawable The drawable being rendered to.
	 * @param width The new viewport width.
	 * @param height The new viewport height.
	 */
	public void resize(GLAutoDrawable drawable, int width, int height)
	{
		GL2 gl = drawable.getGL().getGL2();
		
		/* Store viewport size. */
		mViewportWidth = width;
		mViewportHeight = height;
		
		/* If we already had a gbuffer, release it. */
		if (mGBufferFBO != null)
		{
			mGBufferFBO.releaseGPUResources(gl);
		}
		
		/* Make a new gbuffer with the new size. */
		try
		{
			mGBufferFBO = new FramebufferObject(gl, Format.RGBA, Datatype.FLOAT16, width, height, GBuffer_Count, true, true);
		}
		catch (OpenGLException err)
		{
			/* If that fails, we can't render - so just die. */
			err.printStackTrace();
			System.exit(-1);
		}
	}

	/**
	 * Releases all OpenGL resources (shaders and FBOs) owned by this renderer.
	 */
	public void releaseGPUResources(GL2 gl)
	{
		mGBufferFBO.releaseGPUResources(gl);
		mUberShader.releaseGPUResources(gl);
		mVisShader.releaseGPUResources(gl);
	}
}
