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
public class SmokeSystem extends SceneObject implements Attributable {
	private List<Particle> P = new ArrayList<Particle>();
	private boolean needUpdate = true;
	private float tau = 0.4f;
	
	public HashMap<String, FloatBuffer> vertexAttribData = new HashMap<String, FloatBuffer>();

	@Override
	public String[] getRequiredVertexAttributes() {
		// TODO Auto-generated method stub
		updateAttribs();
		String[] strs = new String[vertexAttribData.size()];
		int i=0;
		for (String s : vertexAttribData.keySet()) {
			strs[i++] = s;
		}
		return strs;
	}

	@Override
	public HashMap<String, FloatBuffer> getVertexAttribData() {
		// TODO If an update is needed, do it.
		updateAttribs();
		
		return vertexAttribData;
	}

	private void updateAttribs() {
		if (needUpdate) {
			//update vertex attributes
			FloatBuffer radii = Buffers.newDirectFloatBuffer(P.size());
			
			for (Particle p : P) {
				radii.put(p.radius);
			}
			radii.rewind();
			vertexAttribData = new HashMap<>();
			vertexAttribData.put("radius", radii);
			needUpdate = false;
		}
	}

	@Override
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
