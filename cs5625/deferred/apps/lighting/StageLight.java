package cs5625.deferred.apps.lighting;

import javax.vecmath.*;

import cs5625.deferred.misc.Observerable;
import cs5625.deferred.rendering.Camera;
import cs5625.deferred.scenegraph.Light;
import cs5625.deferred.scenegraph.SceneObject;

/**
 * Listens to the user camera and 
 * @author jbr99
 *
 */
public class StageLight extends ShadowCamera {
	private SceneObject target;

	public StageLight(Light selfsLight, SceneObject tar) {
		super(selfsLight);
		this.target = tar;
		tar.addObserver(this);
	}

	@Override
	protected void updateOrientation() {
		super.updateOrientation();
		Vector3f toObject = new Vector3f(target.getWorldspacePosition());
		toObject.sub(this.mShadowLight.getWorldspacePosition());
		toObject.normalize();
		float angle = (float)Math.acos(toObject.dot(new Vector3f(0.0f, 0.0f, -1.0f)));
		Vector3f rotAxis = new Vector3f();
		rotAxis.cross(new Vector3f(0.0f, 0.0f, -1.0f), toObject);
		rotAxis.normalize();
		Quat4f orientation = new Quat4f();
		orientation.set(new AxisAngle4f(-angle, rotAxis.x, rotAxis.y, rotAxis.z));
		this.setOrientation(orientation);
		this.setOrientation(target.getOrientation());
	}
	@Override
	public void update(Observerable o) {
		super.update(o);
		if (o.equals(target)) {
			updateOrientation();
		}
	}
	@Override
	public void update(Observerable o, Object obj) {
		super.update(o, obj);
		if (o.equals(target)) {
			updateOrientation();
		}
	}
}
