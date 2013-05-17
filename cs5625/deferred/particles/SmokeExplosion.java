package cs5625.deferred.particles;

import geometry.Explosion;
import geometry.ExplosionHandler;

import java.nio.FloatBuffer;
import java.util.ArrayList;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import com.jogamp.common.nio.Buffers;

import cs5625.deferred.materials.SmokeMaterial;
import cs5625.deferred.misc.Observer;
import cs5625.deferred.misc.Observerable;
import cs5625.deferred.particles.SmokeSource.SmokeParticle;
import cs5625.deferred.scenegraph.Geometry;

public class SmokeExplosion extends Geometry implements Observer {
	public int particlesPerExplosion = 500;
	public float fractionalParticleSize = 0.3f;
	
	ParticleSystem PS;
	FloatBuffer radii, taus;
	ExplosionHandler EH;
	
	float lift = 0.3f;
	float dev = 0.5f;
	float friction = 0.03f;
	float startVDev = 50.0f;
	float maxLife = 0.9f;
	float lifeDev = 0.7f;
	float maxTau = 0.8f;
	
	public boolean isVisible() {
		return super.isVisible() && PS.size()>0;
	}
	
	public SmokeExplosion(ExplosionHandler expHandler) {
		PS = new ParticleSystem();
		SmokeMaterial mat = new SmokeMaterial();
		mat.requiredAttributes = new String[2];
		mat.requiredAttributes[0] = "radius";
		mat.requiredAttributes[1] = "T";
		PS.setMaterial(mat);
		addMesh(PS);
		this.EH = expHandler;
		expHandler.addObserver(this);
	}
	
	public void animate(float dt) {
		super.animate(dt);
		ArrayList<Particle> toRemove = new ArrayList<Particle>();
		for (Particle p : PS.particleIterator()) {
			if (p.life < 0) {
				toRemove.add(p);
			}
			p.v.add(new Vector3f((float)(Math.random()-0.5)*dev,  lift * dt + (float)(Math.random()-0.5)*dev,(float)(Math.random()-0.5)*dev));
			p.v.scale(1.0f-friction);
			p.x.scaleAdd(dt, p.v, p.x);
			p.life -= dt;
			((SmokeParticle)p).tau = (p.life/maxLife) * maxTau;
		}
		PS.removeParticles(toRemove);

		updateRadii();
		updateTaus();
	}

	@Override
	public void update(Observerable o) {
		// Do nothing, I guess..
	}

	@Override
	public void update(Observerable o, Object obj) {
		// TODO Add explosion particles...
		if (!(obj instanceof Explosion)) return;		// Something has gone horribly wrong!
		Explosion exp = (Explosion)obj;
		Point3f origin = exp.getPosition();
		float blastRadius = exp.getRadius();
		
		for (int i=0; i<particlesPerExplosion; i++) {
			SmokeParticle p = new SmokeParticle();
			// Generate p's location in the explosion radius.  Try random at first...
			Vector3f dr = new Vector3f(2f*(float)Math.random()-1f, 2f*(float)Math.random()-1f, 2f*(float)Math.random()-1f);
			while (dr.lengthSquared() > 1.0f) {
				dr = new Vector3f(2f*(float)Math.random()-1f, 2f*(float)Math.random()-1f, 2f*(float)Math.random()-1f);
			}
			dr.scale(blastRadius*1.1f);
			p.x.set(origin);
			p.x.add(dr);
			//dr.scale(arg0)
			dr.normalize();
			dr.scale((float)Math.random() * startVDev);
			p.v.set(dr);
			p.radius = fractionalParticleSize * blastRadius;
			p.tau = maxTau;
			p.life = maxLife + lifeDev*(float)Math.random();
			PS.addParticle(p);
		}
	}

	private void updateRadii() {
		// Add radii to the vertexAttributes of PS
		if (radii==null || radii.capacity() != PS.size()) {
			radii = Buffers.newDirectFloatBuffer(PS.size());
		}
		int i=0;
		for (Particle p : PS.particleIterator()) {
			radii.put(i++, p.radius);
		}
		radii.rewind();
		PS.vertexAttribData.put("radius", radii);
	}
	private void updateTaus() {
		// Add taus
		if (taus==null || taus.capacity() != PS.size()) {
			taus = Buffers.newDirectFloatBuffer(PS.size());
		}
		int i=0;
		for (Particle p : PS.particleIterator()) {
			taus.put(i++, ((SmokeParticle)p).tau);
		}
		taus.rewind();
		PS.vertexAttribData.put("T", taus);
	}
	
	class SmokeParticle extends Particle {
		float tau;
	}
	
	public void prepareRender() {
		PS.dumpParticles();
		updateTaus();
		updateRadii();
		PS.updateAttribs();
	}
}
