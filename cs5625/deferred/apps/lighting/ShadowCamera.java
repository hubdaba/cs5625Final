package cs5625.deferred.apps.lighting;

import cs5625.deferred.misc.Observer;
import cs5625.deferred.misc.Observerable;
import cs5625.deferred.rendering.Camera;
import cs5625.deferred.scenegraph.Light;

public class ShadowCamera extends Camera implements Observer {
	protected float width=0f, height=0f;
	protected Light mShadowLight;
	
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
		return 0;
	}
	
	public Light getShadowLight() {
		return mShadowLight;
	}


	public float getShadowSampleWidth() {
		// TODO Auto-generated method stub
		return 0;
	}


	public float getLightWidth() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	protected void updateOrientation() {
		// Set position to that of the light, primarily
		this.setPosition(mShadowLight.getPosition());
	}


	@Override
	public void update(Observerable o) {
		if (o.equals(mShadowLight)) {
			updateOrientation();
		}
	}


	@Override
	public void update(Observerable o, Object obj) {
		if (o.equals(mShadowLight)) {
			updateOrientation();
		}
	}
	
	public boolean isShadowCamera() {
		return true;
	}
}
