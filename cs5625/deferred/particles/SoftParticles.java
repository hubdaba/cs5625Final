package cs5625.deferred.particles;

import javax.media.opengl.GL2;

import cs5625.deferred.rendering.Camera;
import cs5625.deferred.scenegraph.SceneObject;

// A particle system of soft particles, which can be bound and has 
// a list of its constituent particles.  It does not update their
// locations.
public class SoftParticles extends SceneObject {
	
	public void bind(GL2 gl, Camera camera) {
		// Bind appropriate
	}
	public void unbind(GL2 gl) {
		
	}
}
