package cs5625.deferred.particles;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.vecmath.Point3f;

import com.jogamp.common.nio.Buffers;

import cs5625.deferred.scenegraph.Mesh;

/* A straightforward abstraction, just a particle system for smoke objects */
public class SmokeSystem extends Mesh {
	private List<Particle> P = new ArrayList<Particle>();
	private boolean needUpdate = true;
	
	//public HashMap<String, FloatBuffer> vertexAttribData = new HashMap<String, FloatBuffer>();
	public FloatBuffer normals;
	public FloatBuffer vertices;
	public IntBuffer polyData;

	protected boolean mIsOpaque = false;

	private void updateAttribs() {
		if (needUpdate) {
			//update vertex attributes
			normals = Buffers.newDirectFloatBuffer(3 * P.size());
			// Use the normal vector to pass through data
			for (Particle p : P) {
				normals.put(p.radius);
				normals.put(p.radius);
				normals.put(p.radius);
				//normals.put(0.0f);
			}
			normals.rewind();
			
			vertices = Buffers.newDirectFloatBuffer(3 * P.size());
			for (Particle p : P) {
				vertices.put(p.x.x);
				vertices.put(p.x.y);
				vertices.put(p.x.z);
			}
			vertices.rewind();
			
			polyData = Buffers.newDirectIntBuffer(P.size());
			for (int i=0; i<P.size(); i++) {
				polyData.put(i);
			}
			polyData.rewind();
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

	
	public FloatBuffer getVertexData() {
		updateAttribs();
		return vertices;
	}
	public IntBuffer getPolygonData() {
		updateAttribs();
		return polyData;
	}
	public FloatBuffer getNormalData() {
		updateAttribs();
		return normals;
	}

	
	@Override
	public int getVerticesPerPolygon() {
		return 1;
	}

	@Override
	public FloatBuffer calculateTangentVectors() {
		return null;
	}

	@Override
	public Mesh clone() {
		SmokeSystem copyCat = new SmokeSystem();
		for (Particle p : P) 
			copyCat.addParticle(p);
		return copyCat;
	}
	
	public boolean isOpaque() {
		return mIsOpaque;
	}
}
