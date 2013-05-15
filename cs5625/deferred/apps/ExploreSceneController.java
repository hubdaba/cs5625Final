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
import java.util.LinkedList;
import java.util.List;

import javax.swing.Timer;
import javax.vecmath.AxisAngle4f;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;

import cs5625.deferred.lighting.FlashLight;
import cs5625.deferred.lighting.SunLight;
import cs5625.deferred.misc.OpenGLException;
import cs5625.deferred.misc.Util;
import cs5625.deferred.particles.Particle;
import cs5625.deferred.particles.SmokeExplosion;
import cs5625.deferred.particles.SmokeSource;
import cs5625.deferred.scenegraph.PointLight;
import cs5625.deferred.scenegraph.TerrainRenderer;
import cs5625.deferred.sound.SoundHandler;

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
	
	public static float EXPLOSION_RADIUS = 10.0f;
	
	/* Keeps track of camera's orbit position. Latitude and longitude are in degrees. */
	private float mCameraLongitude = 50.0f, mCameraLatitude = -40.0f;
	private Point3f mCameraPosition = new Point3f(0f,50f, 0f);
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
	private SoundHandler soundHandler;

	private int millisec = 40;
	
	// GAME PARAMETERS
	private float gStepSize = 0.1f;
	private float gMaxDistance = 250f;		// Maybe the far plane distance?  Maybe arbitrary?
	
	protected FlashLight flashlight;
	protected PointLight sun;
	
	private long prevTime;
	
	private Vector3f gUserVelocity = new Vector3f();
	private Vector3f gGravity = new Vector3f(0f, -6f, 0f);
	private Vector3f gUserFeet = new Vector3f(0f, -4f, 0f);
	private float gRestoringForce = 1000f;
	private float gDamping = 0.98f;
	private float gRocketSpeed = 5f;
	private boolean gJetPack = false;
	private float gTopSpeed = 10.0f;
	private float gEscapeSpeed = 0.3f;
	private float gFriction = 0.1f;

	@Override
	public void initializeScene()
	{
		soundHandler = new SoundHandler();
		mCamera.setFar(100);
		explosionHandler = new ExplosionHandler();
		explosionHandler.addObserver(soundHandler);
		QuadSampler quad1 = new QuadSampler(new Point3f(0, 0, 0), TerrainRenderer.BLOCK_SIZE);
		QuadSampler quad2 = new QuadSampler(new Point3f(0, -TerrainRenderer.BLOCK_SIZE, 0), TerrainRenderer.BLOCK_SIZE);
		List<QuadSampler> blocksToRender = new LinkedList<QuadSampler>();
		blocksToRender.add(quad1);
		blocksToRender.add(quad2);
		terrainRenderer = new TerrainRenderer(false, explosionHandler, null);
		try
		{
			mCamera.setPosition(new Point3f(0.0f, 100.0f, 0.0f));
			
			mSceneRoot.addChild(terrainRenderer);
			mCamera.addObserver(terrainRenderer);
			explosionHandler.addObserver(terrainRenderer);
			/* Add an unattenuated point light to provide overall illumination. */
			PointLight light = new PointLight();
			light.setColor(new Color3f(1f, 1f, 1f));

			light.setConstantAttenuation(1.0f);
			light.setLinearAttenuation(0.01f);
			light.setQuadraticAttenuation(0.002f);

			Point3f lightPosition = new Point3f(0f, -2f, 0f);
			lightPosition.add(mCamera.getPosition());
			light.setPosition(lightPosition);
			//mSceneRoot.addChild(light);
			flashlight = new FlashLight(light, mCamera);
			flashlight.setFOV(75);
			mRenderer.addShadowCamera(flashlight);
			
			
			sun = new PointLight();
			sun.setColor(new Color3f(.1f, .1f, .1f));

			sun.setConstantAttenuation(1.0f);
			sun.setLinearAttenuation(0.0f);
			sun.setQuadraticAttenuation(0.0f);

			lightPosition = new Point3f(1000f, 1800f, 1000f);
			lightPosition.add(mCamera.getPosition());
			light.setPosition(lightPosition);
			mSceneRoot.addChild(sun);

			
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
		
		prevTime = System.currentTimeMillis();

		/* Initialize camera position. */
		updateCamera();
		new Timer(millisec, new simulator()).start();
	}

	private class simulator implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			long newTime = System.currentTimeMillis();
			double dt = (double)(newTime-prevTime)/1000.0;
			prevTime = newTime;
			animate(dt);
		}
	}

	private void animate(double dt) {
		mSceneRoot.animate((float)dt);

		try {
			advanceUser(dt);
		} catch (OpenGLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
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
	
	private void advanceUser(double dt) throws OpenGLException {
		// Make sure nothing crazy is going on with the timing...
		targetVelocity.scale(decay);
		mCameraVelocity.scale(tau);
		mCameraVelocity.scaleAdd(1.0f-tau, targetVelocity, mCameraVelocity);
		gUserVelocity.scaleAdd((float)dt, getCameraVelocity(), gUserVelocity);
		gUserVelocity.scaleAdd((float)dt, gGravity, gUserVelocity);

		if (gJetPack) {
			gUserVelocity.y = gRocketSpeed;
		}
		
		gUserVelocity.scaleAdd((float)dt, getRestoringForce(mCameraPosition, gUserVelocity, (float)dt), gUserVelocity);
		gUserVelocity.scaleAdd((float)dt, getRestoringForce(flashlight.getPosition(), gUserVelocity, (float)dt), gUserVelocity);
		Point3f feet = new Point3f();
		feet.add(mCameraPosition, gUserFeet); 
		gUserVelocity.scaleAdd((float)dt, getRestoringForce(feet, gUserVelocity, (float)dt), gUserVelocity);
		
		gUserVelocity.scale(gDamping);
		
		gUserVelocity.clamp(-gTopSpeed, gTopSpeed);
		
		mCameraPosition.scaleAdd((float)dt, gUserVelocity, mCameraPosition);
		
		// Detect collisions. Note- collision detection approach does not work.  Trying restoring forces...
		/*
		Vector3f dir = new Vector3f();
		dir.normalize(gUserVelocity);
		float dist = findDistanceToWall(mCameraPosition, dir, gUserVelocity.length()*((float)dt));
		dist = Math.min(dist, findDistanceToWall(flashlight.getPosition(), dir, gUserVelocity.length()*((float)dt)));
		Point3f feet = new Point3f();
		feet.add(mCameraPosition, gUserFeet); 
		dist = Math.min(dist, findDistanceToWall(feet, dir, gUserVelocity.length()*((float)dt)));
		
		// We hit something!  Let's back off a little
		if (dist<gUserVelocity.length()*((float)dt)) {
			dist -= gStepSize;
		}
		
		// Move that distance!
		mCameraPosition.scaleAdd(0.9f*dist, dir, mCameraPosition); */
	}
	private float findDistanceToWall(Point3f p, Vector3f dir, float max) throws OpenGLException {
		Point3f hit = terrainRenderer.findWall(p, dir, gStepSize, max);
		if (hit==null) {
			return max;
		}
		return p.distance(hit);
	}
	private Vector3f getCameraVelocity() {
		Vector3f cameraVelocity = new Vector3f();
		cameraVelocity.scaleAdd(mCameraVelocity.x, rightVector, cameraVelocity);
		cameraVelocity.scaleAdd(mCameraVelocity.y, forwardVector, cameraVelocity);
		return cameraVelocity;
	}
	private Vector3f getRestoringForce(Point3f p, Vector3f v, float dt) throws OpenGLException {
		if (terrainRenderer.evaluate(p) > 0.0f) {
			return new Vector3f();
		}
		Vector3f normal = terrainRenderer.getNormal(p);
		float vIn = normal.dot(v);
		Vector3f parallel = new Vector3f();
		parallel.scaleAdd(-normal.dot(v), normal, v);
		Point3f exit = terrainRenderer.findWall(p, normal, gStepSize, 5f, false);
		float perp;
		if (exit==null) {
			perp = gRestoringForce*5f*dt;
		} else {
			perp = p.distance(exit)*gRestoringForce*dt;
		}
		if (vIn+perp > gEscapeSpeed) {
			perp = gEscapeSpeed-vIn;
		}
		perp = perp < 0.0f ? 0.0f : perp;
		parallel.scale(-gFriction);
		parallel.scaleAdd(perp, normal, parallel);
		parallel.scale(1.0f/dt);
		return parallel;
	}
	
	@Override
	public void mouseClicked(MouseEvent mouse) {
		if (mouse.getButton()==3) {		// Right Click
			forwardVector = new Vector3f(0.0f, 0.0f, -1.0f);
			Util.rotateTuple(mCamera.getOrientation(), forwardVector);
			forwardVector.normalize();

			try {
				Point3f newSplosion = terrainRenderer.findWall(mCamera.getWorldspacePosition(), forwardVector, gStepSize, gMaxDistance);
				if (newSplosion != null) 
					explosionHandler.addExplosion(new Explosion(newSplosion, EXPLOSION_RADIUS));
			} catch (OpenGLException e) {
				// Do nothing if it crashes- might just mean it missed all the walls.
				// It's not really important, but we definitely don't want to disturb the user
			} 
		}
	}
	
	public void keyTyped(KeyEvent key) {
		super.keyTyped(key);
		char c = key.getKeyChar();if (c == 'h') {
			flashlight.setBias(flashlight.getBias() - 0.000001f);
			System.out.println("Flashlight Bias: " + flashlight.getBias());
			requiresRender();
		}
		else if (c == 'H') {
			flashlight.setBias(flashlight.getBias() + 0.000001f);
			System.out.println("Flashlight Bias: " + flashlight.getBias());
		}
		else if (c == 'j') {
			mRenderer.incrementShadowMode();
			int mode = mRenderer.getShadowMode();
			String modeString = (mode == 0 ? "DEFAULT SHADOWMAP" : (mode == 1 ? "PCF SHADOWMAP" : "PCSS SHADOWMAP"));
			System.out.println("Shadow Map mode: " + modeString);
			requiresRender();
		}
		else if (c == 'k') {
			if (mRenderer.getShadowMode() == 1) {
				flashlight.setShadowSampleWidth(flashlight.getShadowSampleWidth()-1);
				System.out.println("Flashlight Sample Width: " + flashlight.getShadowSampleWidth());
				requiresRender();
			}
			if (mRenderer.getShadowMode() == 2) {
				flashlight.setLightWidth(flashlight.getLightWidth() - 1);
				System.out.println("Flashlight Light Width: " + flashlight.getLightWidth());
				requiresRender();
			}
		}
		else if (c == 'K') {
			if (mRenderer.getShadowMode() == 1) {
				flashlight.setShadowSampleWidth(flashlight.getShadowSampleWidth()+1);
				System.out.println("Flashlight Sample Width: " + flashlight.getShadowSampleWidth());
				requiresRender();
			}
			if (mRenderer.getShadowMode() == 2) {
				flashlight.setLightWidth(flashlight.getLightWidth() + 1);
				System.out.println("Flashlight Light Width: " + flashlight.getLightWidth());
				requiresRender();
			}
		}
	}
	
	
	
	public void keyPressed(KeyEvent key)
	{
		super.keyPressed(key);	
		switch (key.getKeyCode()) {
		case KeyEvent.VK_UP:
		case KeyEvent.VK_W:
			targetVelocity.y = maxSpeed;
			break;
		case KeyEvent.VK_DOWN:
		case KeyEvent.VK_S:
			targetVelocity.y = -maxSpeed;
			break;
		case KeyEvent.VK_LEFT:
		case KeyEvent.VK_A:
			targetVelocity.x = -maxSpeed;
			break;
		case KeyEvent.VK_RIGHT:
		case KeyEvent.VK_D:
			targetVelocity.x = +maxSpeed;
			break;
		case KeyEvent.VK_SPACE:
			gJetPack = true;
			break;
		}
		
	}
	public void keyReleased(KeyEvent key)
	{
		super.keyReleased(key);
		int k = key.getKeyCode();
		if (k==KeyEvent.VK_UP || k==KeyEvent.VK_DOWN || k==KeyEvent.VK_W || k==KeyEvent.VK_S) {
			targetVelocity.y = 0.0f;
		} else if (k==KeyEvent.VK_LEFT || k==KeyEvent.VK_RIGHT || k==KeyEvent.VK_A || k==KeyEvent.VK_D) {
			targetVelocity.x = 0.0f;
		} else if (k==KeyEvent.VK_SPACE) {
			gJetPack = false;
		}
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
