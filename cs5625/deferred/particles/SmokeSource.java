package cs5625.deferred.particles;

import java.util.ArrayList;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

public class SmokeSource extends SmokeSystem {
	float lift = 0.3f;
	float dev = 0.1f;
	float friction = 0.01f;
	public float timeBetweenParticles = 0.1f;
	private float nextParticleCreation = 0.0f;
	float radius = 1.0f;
	float startVDev = 0.6f;
	float life = 15.0f;
	public Point3f origin = new Point3f();
	
	public boolean isVisible() {
		return size()!=0 && super.isVisible();
	}
	
	public void animate(float dt) {
		super.animate(dt);
		ArrayList<Particle> toRemove = new ArrayList<Particle>();
		for (Particle p : particleIterator()) {
			if (p.life < 0) {
				toRemove.add(p);
			}
			p.v.add(new Vector3f((float)(Math.random()-0.5)*dev,  lift * dt + (float)(Math.random()-0.5)*dev,(float)(Math.random()-0.5)*dev));
			p.v.scale(1.0f-friction);
			p.x.scaleAdd(dt, p.v, p.x);
			p.life -= dt;
			p.radius *= 0.9f;
		}
		while (nextParticleCreation < 0.0f) {
			//System.out.println(dt);
			nextParticleCreation += timeBetweenParticles;
			Particle newP = new Particle();
			newP.x = new Point3f(origin);
			newP.radius = radius;
			newP.v = new Vector3f((float)(Math.random()-0.5)*startVDev, (float)(Math.random()-0.5)*startVDev, (float)(Math.random()-0.5)*startVDev);
			newP.life = life;
			addParticle(newP);
			System.out.println("new perticle!");
		}
		removeParticles(toRemove);
		
		nextParticleCreation -= dt;
	}
}