package cs5625.deferred.apps;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import javax.swing.Timer;
import javax.vecmath.*;

import cs5625.deferred.materials.LambertianMaterial;
import cs5625.deferred.misc.Util;
import cs5625.deferred.particles.Particle;
import cs5625.deferred.particles.SmokeSystem;
import cs5625.deferred.scenegraph.Geometry;
import cs5625.deferred.scenegraph.PointLight;
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
	/* Keeps track of camera's orbit position. Latitude and longitude are in degrees. */
	private float mCameraLongitude = 50.0f, mCameraLatitude = -40.0f;
	private Point3f mCameraPosition = new Point3f(0f, 0f, 0f);
	private Vector2f mCameraVelocity = new Vector2f();
	private Vector3f rightVector = new Vector3f();
	private Vector3f forwardVector = new Vector3f();
	
	private float tau = 0.65f;
	private float decay = 0.999f;	// Just for sanity, in case a call is missed (and it helped in diagnostics)
	private Vector2f targetVelocity = new Vector2f();
	private float maxSpeed = 3.0f;
	
	private float rotScale = 0.3f;

	/* Used to calculate mouse deltas to orbit the camera in mouseDragged(). */ 
	private Point mLastMouseDrag;

	private TerrainRenderer terrainRenderer;

	private int millisec = 40;

	@Override
	public void initializeScene()
	{
		terrainRenderer = new TerrainRenderer(false);
		try
		{
			mSceneRoot.addChild(terrainRenderer);
			mCamera.addObserver(terrainRenderer);
			/* Add an unattenuated point light to provide overall illumination. */
			PointLight light = new PointLight();

			light.setConstantAttenuation(1.0f);
			light.setLinearAttenuation(0.0f);
			light.setQuadraticAttenuation(0.0f);

			light.setPosition(new Point3f(50.0f, 180.0f, 100.0f));
			mSceneRoot.addChild(light);	
			
			SmokeSystem smoke = new SmokeSystem();
			Particle p = new Particle();
			p.x = new Point3d(0.0, 2.0, 0.0);
			p.radius = 4f;
			smoke.addParticle(p);
			p = new Particle();
			p.x = new Point3d(0.0, -2.0, 0.0);
			p.radius = 4f;
			smoke.addParticle(p);
			
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

		mCamera.getOrientation().mul(longitudeQuat, latitudeQuat);

		mCamera.getPosition().set(mCameraPosition);
		
		forwardVector = new Vector3f(0.0f, 0.0f, -1.0f);
		rightVector = new Vector3f(1.0f, 0.0f, 0.0f);
		Util.rotateTuple(mCamera.getOrientation(), forwardVector);
		Util.rotateTuple(mCamera.getOrientation(), rightVector);
		forwardVector.normalize();
		rightVector.normalize();
		mCamera.notifyObservers();
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

}
