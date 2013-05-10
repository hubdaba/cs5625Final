package cs5625.deferred.particles;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import javax.vecmath.Point3f;

import com.jogamp.common.nio.Buffers;

import cs5625.deferred.rendering.Attributable;
import cs5625.deferred.scenegraph.SceneObject;

/* A straightforward abstraction, just a particle system for smoke objects */
public class SmokeSystem extends SceneObject {
	private List<Particle> P = new ArrayList<Particle>();
	private boolean needUpdate = true;
	private float tau = 0.4f;
	
	//public HashMap<String, FloatBuffer> vertexAttribData = new HashMap<String, FloatBuffer>();
	public FloatBuffer normals;

	public FloatBuffer getNormalData() {
		updateAttribs();
		return normals;
	}

	private void updateAttribs() {
		if (needUpdate) {
			//update vertex attributes
			normals = Buffers.newDirectFloatBuffer(4 * P.size());
			
			// Use the normal vector to pass through data
			for (Particle p : P) {
				normals.put(p.radius);
				normals.put(0.0f);
				normals.put(0.0f);
				normals.put(0.0f);
			}
			normals.rewind();
			needUpdate = false;
		}
	}

	public int getVertexCount() {
		// TODO Auto-generated method stub
		return P.size();
	}
	
	public List<Point3f> getParticlePositions() {
		List<Point3f> locs = new ArrayList<Point3f>(P.size());
		for (Particle p : P) {
			locs.add(p.x);
		}
		return locs;
	}
	
	public void addParticle(Particle p) {
		needUpdate = true;
		P.add(p);
	}
	
	public float getTau() {
		return tau;
	}
	
	public Iterable<Particle> particleIterator() {
		return P;
	}
	
	public void removeParticles(Collection<Particle> toRemove) {
		needUpdate = true;
		P.removeAll(toRemove);
	}
	public int size() {
		return P.size();
	}
}
