package cs5625.deferred.lighting;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import cs5625.deferred.misc.Observerable;
import cs5625.deferred.scenegraph.Light;
import cs5625.deferred.scenegraph.SceneObject;

public class FlashLight extends ShadowCamera {
	private SceneObject target;
	/** The vector to the target  */
	protected Vector3f relativePosition;
	protected float mCoeff=8.0f, mRadius=0.4545f;

	public FlashLight(Light selfsLight, SceneObject tar) {
		super(selfsLight);
		this.target = tar;
		tar.addObserver(this);
		relativePosition = new Vector3f(tar.getWorldspacePosition());
		relativePosition.sub(selfsLight.getWorldspacePosition());
	}
	
	public float getFlashlightCoefficient() {
		return mCoeff;
	}
	public void setFlashlightCoefficient(float c) {
		mCoeff = c;
	}
	public float getFlashlightRadius() {
		return mRadius;
	}
	public void setFlashlightRadius(float r) {
		mRadius = r;
	}
	public Vector3f getAimDirection() {
		Vector3f dir = new Vector3f(0f, 0f, -1f);
		this.transformVectorToWorldSpace(dir);
		return dir;
	}
	

	
	public void resize(float width, float height) {
		setShadowMapWidth(2.0f*width);
		setShadowMapHeight(2.0f*height);
	}

	
	@Override
	protected void updateOrientation() {
		super.updateOrientation();
		this.setOrientation(target.getOrientation());
	}
	protected void enforceRelativePosition() {
		Point3f myNewPosition = new Point3f(target.getWorldspacePosition());
		myNewPosition.sub(relativePosition);
		this.setPosition(myNewPosition);
		mShadowLight.setPosition(myNewPosition);
	}
	@Override
	public void update(Observerable o) {
		super.update(o);
		if (o.equals(target)) {
			enforceRelativePosition();
			updateOrientation();
		}
	}
	@Override
	public void update(Observerable o, Object obj) {
		super.update(o, obj);
		if (o.equals(target)) {
			enforceRelativePosition();
			updateOrientation();
		}
	}
}
