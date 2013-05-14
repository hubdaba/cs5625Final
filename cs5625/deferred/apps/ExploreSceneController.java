package cs5625.deferred.apps;

import geometry.Explosion;
import geometry.ExplosionHandler;
import geometry.QuadSampler;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.swing.Timer;
import javax.vecmath.AxisAngle4f;
import javax.vecmath.Point3f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;

import cs5625.deferred.misc.OpenGLException;
import cs5625.deferred.misc.ScenegraphException;
import cs5625.deferred.misc.Util;
import cs5625.deferred.particles.SmokeExplosion;
import cs5625.deferred.scenegraph.Geometry;
import cs5625.deferred.scenegraph.PointLight;
import cs5625.deferred.scenegraph.SceneObject;
import cs5625.deferred.scenegraph.TerrainRenderer;

/**
 * DefaultSceneController.java
 * 
 * The default scene controller creates a simple scene and allows the user to orbit the camera 
 * and preview the renderer's gbuffer.
 * 
 * Drag the mouse to orbit the camera, and scroll to zoom. Numbers 1-9 preview individual gbuffer 
 * textures, and 0 views the shaded result.
 * 
 * Written for Cornell CS 5625 (Interactive Computer Graphics).
 * Copyright (c) 2012, Computer Science Department, Cornell University.
 * 
 * @author Asher Dunn (ad488)
 * @date 2012-03-23
 */
public class ExploreSceneController extends SceneController
{
	
	public static float EXPLOSION_RADIUS = 5.0f;
	
	/* Keeps track of camera's orbit position. Latitude and longitude are in degrees. */
	private float mCameraLongitude = 50.0f, mCameraLatitude = -40.0f;
	private Point3f mCameraPosition = new Point3f(12f, 12f, 12f);
	private Vector2f mCameraVelocity = new Vector2f();
	private Vector3f rightVector = new Vector3f();
	private Vector3f forwardVector = new Vector3f();
	
	private float tau = 0.65f;
	private float decay = 0.999f;	// Just for sanity, in case a call is missed (and it helped in diagnostics)
	private Vector2f targetVelocity = new Vector2f();
	private float maxSpeed = 5.0f;
	
	private float rotScale = 0.3f;

	/* Used to calculate mouse deltas to orbit the camera in mouseDragged(). */ 
	private Point mLastMouseDrag;

	private TerrainRenderer terrainRenderer;
	private ExplosionHandler explosionHandler;

	private int millisec = 40;
	
	// GAME PARAMETERS
	private float gStepSize = 0.1f;
	private float gMaxDistance = 250f;		// Maybe the far plane distance?  Maybe arbitrary?

	@Override
	public void initializeScene()
	{
		explosionHandler = new ExplosionHandler();
		QuadSampler quad1 = new QuadSampler(new Point3f(0, 0, 0), TerrainRenderer.BLOCK_SIZE);
		QuadSampler quad2 = new QuadSampler(new Point3f(0, -TerrainRenderer.BLOCK_SIZE, 0), TerrainRenderer.BLOCK_SIZE);
		List<QuadSampler> blocksToRender = new LinkedList<QuadSampler>();
		blocksToRender.add(quad1);
		blocksToRender.add(quad2);
		terrainRenderer = new TerrainRenderer(false, explosionHandler, null);
		try
		{
			mSceneRoot.addChild(terrainRenderer);
			mCamera.addObserver(terrainRenderer);
			explosionHandler.addObserver(terrainRenderer);
			/* Add an unattenuated point light to provide overall illumination. */
			PointLight light = new PointLight();

			light.setConstantAttenuation(1.0f);
			light.setLinearAttenuation(0.0f);
			light.setQuadraticAttenuation(0.0f);

			light.setPosition(new Point3f(50.0f, 180.0f, 100.0f));
			mSceneRoot.addChild(light);	
			
			/*
			SmokeSystem smoke = new SmokeSystem();
			Particle p = new Particle();
			p.x = new Point3d(0.0, 2.0, 0.0);
			p.radius = 4f;
			smoke.addParticle(p);
			p = new Particle();
			p.x = new Point3d(0.0, -2.0, 0.0);
			p.radius = 4f;
			smoke.addParticle(p); */
			
			/*
			SmokeSystem smoke = new SmokeSystem();
			int N = 20;
			float R = 10.0f;
			for (int n=0; n<N; n++) {
				Particle p = new Particle();
				double theta = ((double)n)*Math.PI*2.0/((double)N); 
				p.x = new Point3f((float)(R*Math.cos(theta)), 1.0f+(float)(0.5*Math.cos(15.0*theta)), (float)(R*Math.sin(theta)));
				p.radius = 1.0f;
				smoke.addParticle(p); 
			} */
			//SmokeSource smoke = new SmokeSource();
			//smoke.origin.set(12f, 12f, 12f);
			SmokeExplosion smoke = new SmokeExplosion(explosionHandler);
			mSceneRoot.addChild(smoke);
		}
		catch (Exception err)
		{
			/* If anything goes wrong, just die. */
			err.printStackTrace();
			System.exit(-1);
		}

		/* Initialize camera position. */
		updateCamera();
		new Timer(millisec, new simulator()).start();
	}

	private class simulator implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			animate((double)millisec/1000.0);
		}
	}

	private void animate(double dt) {
		targetVelocity.scale(decay);
		mCameraVelocity.scale(tau);
		mCameraVelocity.scaleAdd(1.0f-tau, targetVelocity, mCameraVelocity);
		mCameraPosition.scaleAdd((float)dt * mCameraVelocity.x, rightVector, mCameraPosition);
		mCameraPosition.scaleAdd((float)dt * mCameraVelocity.y, forwardVector, mCameraPosition);
		
		mSceneRoot.animate((float)dt);
		
		updateCamera();
		requiresRender();
	}

	/**
	 * Updates the camera position and orientation based on orbit parameters.
	 */
	protected void updateCamera()
	{
		/* Compose the "horizontal" and "vertical" rotations. */
		Quat4f longitudeQuat = new Quat4f();
		longitudeQuat.set(new AxisAngle4f(0.0f, 1.0f, 0.0f, mCameraLongitude * (float)Math.PI / 180.0f));

		Quat4f latitudeQuat = new Quat4f();
		latitudeQuat.set(new AxisAngle4f(1.0f, 0.0f, 0.0f, mCameraLatitude * (float)Math.PI / 180.0f));
		Quat4f mCameraPrevOrientation = new Quat4f(mCamera.getOrientation());
		mCamera.getOrientation().mul(longitudeQuat, latitudeQuat);
		boolean orientationChanged = !mCameraPrevOrientation.equals(mCamera.getOrientation()); 
		Point3f mCameraPositionPrev = new Point3f(mCamera.getPosition());
		mCamera.getPosition().set(mCameraPosition);
		forwardVector = new Vector3f(0.0f, 0.0f, -1.0f);
		rightVector = new Vector3f(1.0f, 0.0f, 0.0f);
		Util.rotateTuple(mCamera.getOrientation(), forwardVector);
		Util.rotateTuple(mCamera.getOrientation(), rightVector);
		forwardVector.normalize();
		rightVector.normalize();
		boolean positionChanged = !mCameraPositionPrev.equals(mCamera.getPosition());
		if (positionChanged || orientationChanged) {
			mCamera.notifyObservers();
		} 
	}

	@Override
	public void mousePressed(MouseEvent mouse)
	{
		/* Remember the starting point of a drag. */
		mLastMouseDrag = mouse.getPoint();
	}

	@Override
	public void mouseDragged(MouseEvent mouse)
	{
		/* Calculate dragged delta. */
		float deltaX = -(mouse.getPoint().x - mLastMouseDrag.x) * rotScale;
		float deltaY = -(mouse.getPoint().y - mLastMouseDrag.y) * rotScale;
		mLastMouseDrag = mouse.getPoint();

		/* Update longitude, wrapping as necessary. */
		mCameraLongitude += deltaX;

		if (mCameraLongitude > 360.0f)
		{
			mCameraLongitude -= 360.0f;
		}
		else if (mCameraLongitude < 0.0f)
		{
			mCameraLongitude += 360.0f;
		}

		/* Update latitude, clamping as necessary. */
		if (Math.abs(mCameraLatitude + deltaY) <= 89.0f)
		{
			mCameraLatitude += deltaY;
		}
		else
		{
			mCameraLatitude = 89.0f * Math.signum(mCameraLatitude);
		}
	}
	
	@Override
	public void mouseClicked(MouseEvent mouse) {
		if (mouse.getButton()==3) {		// Right Click
			forwardVector = new Vector3f(0.0f, 0.0f, -1.0f);
			Util.rotateTuple(mCamera.getOrientation(), forwardVector);
			forwardVector.normalize();
			Point3f newSplosion = findWall(mCamera.getWorldspacePosition(), forwardVector);
			if (newSplosion != null) 
				explosionHandler.addExplosion(new Explosion(newSplosion, EXPLOSION_RADIUS));
			try {
				List<Geometry> cubes = Geometry.load("models/cube.obj", true, true);
				SceneObject cubeObject = new SceneObject();
				cubeObject.setPosition(new Point3f(newSplosion));
				cubeObject.addGeometry(cubes);
				//cubeObject.setScale(100f);
				mSceneRoot.addChild(cubeObject);
			} catch (ScenegraphException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void keyTyped(KeyEvent key) {
		super.keyTyped(key);
		char c = key.getKeyChar();
		if (c == ' ') {
			explosionHandler.addExplosion(
						new Explosion(mCamera.getPosition(), EXPLOSION_RADIUS));
			
			try {
				List<Geometry> cubes = Geometry.load("models/cube.obj", true, true);
				SceneObject cubeObject = new SceneObject();
				cubeObject.setPosition(new Point3f(mCamera.getPosition()));
				cubeObject.addGeometry(cubes);
				//cubeObject.setScale(100f);
				mSceneRoot.addChild(cubeObject);
			} catch (ScenegraphException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	

	public void keyPressed(KeyEvent key)
	{
		super.keyPressed(key);	
		switch (key.getKeyCode()) {
		case KeyEvent.VK_UP:
			targetVelocity.y = maxSpeed;
			break;
		case KeyEvent.VK_DOWN:
			targetVelocity.y = -maxSpeed;
			break;
		case KeyEvent.VK_LEFT:
			targetVelocity.x = -maxSpeed;
			break;
		case KeyEvent.VK_RIGHT:
			targetVelocity.x = +maxSpeed;
			break;
		}
		
	}
	public void keyReleased(KeyEvent key)
	{
		super.keyReleased(key);
		int k = key.getKeyCode();
		if (k==KeyEvent.VK_UP || k==KeyEvent.VK_DOWN) {
			targetVelocity.y = 0.0f;
		} else if (k==KeyEvent.VK_LEFT || k==KeyEvent.VK_RIGHT) {
			targetVelocity.x = 0.0f;
		}
	}
	
	public Point3f findWall(Point3f start, Vector3f dir) {
		Vector3f dr = new Vector3f();
		dr.normalize(dir);
		dr.scale(gStepSize);
		Point3f check = new Point3f(start);
		float distTraveled = 0.0f;
		float val;
		try {
			val = terrainRenderer.evaluate(check);
			while (val > 0.0) {
				distTraveled += gStepSize;
				check.add(dr);
				val = terrainRenderer.evaluate(check);
		//	System.out.println(val);
				if (distTraveled > gMaxDistance) {
					return null;
				}
			}
		} catch (OpenGLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	//	System.out.println("HIT with "+val);
		return check;
	}

}
interface CameraWatcher extends MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {}
class WalkingCamera implements CameraWatcher {
	public void mouseClicked(MouseEvent arg0) {}
	public void mouseEntered(MouseEvent arg0) {}
	public void mouseExited(MouseEvent arg0) {}
	public void mousePressed(MouseEvent arg0) {}
	public void mouseReleased(MouseEvent arg0) {}
	public void mouseDragged(MouseEvent arg0) {}
	public void mouseMoved(MouseEvent arg0) {}
	public void mouseWheelMoved(MouseWheelEvent e) {}
	public void keyPressed(KeyEvent arg0) {}
	public void keyReleased(KeyEvent arg0) {}
	public void keyTyped(KeyEvent arg0) {}
}
class FlyingCamera implements CameraWatcher {
	public void mouseClicked(MouseEvent arg0) {}
	public void mouseEntered(MouseEvent arg0) {}
	public void mouseExited(MouseEvent arg0) {}
	public void mousePressed(MouseEvent arg0) {}
	public void mouseReleased(MouseEvent arg0) {}
	public void mouseDragged(MouseEvent arg0) {}
	public void mouseMoved(MouseEvent arg0) {}
	public void mouseWheelMoved(MouseWheelEvent e) {}
	public void keyPressed(KeyEvent arg0) {}
	public void keyReleased(KeyEvent arg0) {}
	public void keyTyped(KeyEvent arg0) {}
}
