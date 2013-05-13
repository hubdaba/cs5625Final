package cs5625.deferred.particles;

import java.nio.FloatBuffer;
import java.util.ArrayList;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import com.jogamp.common.nio.Buffers;

import cs5625.deferred.materials.SmokeMaterial;
import cs5625.deferred.scenegraph.Geometry;

public class SmokeSource extends Geometry {
	float lift = 0.3f;
	float dev = 0.1f;
	float friction = 0.01f;
	public float timeBetweenParticles = 0.1f;
	private float nextParticleCreation = 0.0f;
	float radius = 1.0f;
	float startVDev = 0.6f;
	float maxLife = 15.0f;
	public Point3f origin = new Point3f();
	
	float maxTau = 0.4f;
	
	ParticleSystem PS;
	
	FloatBuffer radii, taus;
	
	public SmokeSource() {
		PS = new ParticleSystem();
		SmokeMaterial mat = new SmokeMaterial();
		mat.requiredAttributes = new String[2];
		mat.requiredAttributes[0] = "radius";
		mat.requiredAttributes[1] = "T";
		PS.setMaterial(mat);
		addMesh(PS);
	}
	
	public boolean isVisible() {
		return PS.size()!=0 && super.isVisible();
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
		while (nextParticleCreation < 0.0f) {
			//System.out.println(dt);
			nextParticleCreation += timeBetweenParticles;
			Particle newP = new SmokeParticle();
			newP.x = new Point3f(origin);
			newP.radius = radius;
			newP.v = new Vector3f((float)(Math.random()-0.5)*startVDev, (float)(Math.random()-0.5)*startVDev, (float)(Math.random()-0.5)*startVDev);
			newP.life = maxLife;
			PS.addParticle(newP);
		}
		PS.removeParticles(toRemove);
		
		nextParticleCreation -= dt;
		
		// Add radii to the vertexAttributes of PS
		if (radii==null || radii.capacity() < PS.size()) {
			radii = Buffers.newDirectFloatBuffer(PS.size());
		}
		int i=0;
		for (Particle p : PS.particleIterator()) {
			radii.put(i++, p.radius);
		}
		radii.rewind();
		PS.vertexAttribData.put("radius", radii);
		
		// Add taus
		if (taus==null || taus.capacity() < PS.size()) {
			taus = Buffers.newDirectFloatBuffer(PS.size());
		}
		i=0;
		for (Particle p : PS.particleIterator()) {
			taus.put(i++, ((SmokeParticle)p).tau);
		}
		taus.rewind();
		PS.vertexAttribData.put("T", taus);
	}
	
	class SmokeParticle extends Particle {
		public float tau = maxTau;
	}
}
