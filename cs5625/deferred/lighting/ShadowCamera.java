package cs5625.deferred.lighting;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Point3f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import cs5625.deferred.misc.Observer;
import cs5625.deferred.misc.Observerable;
import cs5625.deferred.rendering.Camera;
import cs5625.deferred.scenegraph.Light;

public class ShadowCamera extends Camera implements Observer {
	protected float width=0f, height=0f;
	protected Light mShadowLight;
	
	protected float bias = 2E-4f;
	protected float mShadowSampleWidth = 1f;
	protected float mLightWidth = 10f;
	
	public ShadowCamera(Light l) {
		super();
		this.mShadowLight = l;
		l.addObserver(this);
	}
	

	public float getShadowMapWidth() {
		return width;
	}

	public void setShadowMapWidth(float width) {
		this.width = width;
	}

	public float getShadowMapHeight() {
		return height;
	}

	public void setShadowMapHeight(float height) {
		this.height = height;
	}
	
	public void resize(float width, float height) {
		setShadowMapWidth(width);
		setShadowMapHeight(height);
	}

	public float getBias() {
		// TODO Auto-generated method stub
		return bias;
	}
	public void setBias(float b) {
		bias = b;
	}
	
	public Light getShadowLight() {
		return mShadowLight;
	}


	public float getShadowSampleWidth() {
		return mShadowSampleWidth;
	}
	public void setShadowSampleWidth(float ssw) {
		mShadowSampleWidth = ssw;
	}


	public float getLightWidth() {
		return mLightWidth;
	}
	public void setLightWidth(float lw) {
		mLightWidth = lw;
	}
	
	protected void updatePosition() {
		// Set position to that of the light, primarily
		this.setPosition(mShadowLight.getPosition());
	}
	protected void updateOrientation() { }
	@Override
	public void update(Observerable o) {
		if (o.equals(mShadowLight)) {
			updatePosition();
		}
	}
	@Override
	public void update(Observerable o, Object obj) {
		if (o.equals(mShadowLight)) {
			updatePosition();
		}
	}
	
	public boolean isShadowCamera() {
		return true;
	}
	
	public static Quat4f getOrientationTowardObject(Point3f from, Point3f to) {
		Vector3f toObject = new Vector3f(to);
		toObject.sub(from);
		toObject.normalize();
		float angle = (float)Math.acos(toObject.dot(new Vector3f(0.0f, 0.0f, -1.0f)));
		Vector3f rotAxis = new Vector3f();
		rotAxis.cross(new Vector3f(0.0f, 0.0f, -1.0f), toObject);
		rotAxis.normalize();
		Quat4f orientation = new Quat4f();
		orientation.set(new AxisAngle4f(rotAxis.x, rotAxis.y, rotAxis.z, angle));
		return orientation;
	}
}
