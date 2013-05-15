package cs5625.deferred.lighting;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import cs5625.deferred.misc.Observerable;
import cs5625.deferred.scenegraph.Light;
import cs5625.deferred.scenegraph.SceneObject;

/**
 * A camera and light which follows the specified sceneobject (target)
 * @author jbr99
 *
 */
public class SunLight extends ShadowCamera {
	private SceneObject target;
	/** The vector to the target  */
	protected Vector3f relativePosition;

	public SunLight(Light selfsLight, SceneObject tar) {
		super(selfsLight);
		this.target = tar;
		tar.addObserver(this);
		relativePosition = new Vector3f(tar.getWorldspacePosition());
		relativePosition.sub(selfsLight.getWorldspacePosition());
	}

	
	@Override
	protected void updateOrientation() {
		super.updateOrientation();
		this.setOrientation(getOrientationTowardObject(this.mShadowLight.getPosition(), target.getPosition()));
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
