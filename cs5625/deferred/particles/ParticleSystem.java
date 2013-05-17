package cs5625.deferred.particles;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.vecmath.Point3f;

import com.jogamp.common.nio.Buffers;

import cs5625.deferred.scenegraph.Mesh;

/* A straightforward abstraction, just a particle system for smoke objects */
public class ParticleSystem extends Mesh {
	private List<Particle> P = new ArrayList<Particle>();
	private boolean needUpdate = true;
	
	//public HashMap<String, FloatBuffer> vertexAttribData = new HashMap<String, FloatBuffer>();

	protected boolean mIsOpaque = false;
	
	int numParticles = 0;
	
	BlockingQueue<Particle> queue = new LinkedBlockingQueue<Particle>();

	protected void updateAttribs() {
		if (needUpdate) {
			/*	No longer hack and use normals to pass through data!
			//update vertex attributes
			mNormalData = Buffers.newDirectFloatBuffer(3 * P.size());
			// Use the normal vector to pass through data
			for (Particle p : P) {
				mNormalData.put(p.radius);
				mNormalData.put(p.radius);
				mNormalData.put(p.radius);
				//normals.put(0.0f);
			}
			mNormalData.rewind(); */
			
			mVertexData = Buffers.newDirectFloatBuffer(3 * P.size());
			for (Particle p : P) {
				mVertexData.put(p.x.x);
				mVertexData.put(p.x.y);
				mVertexData.put(p.x.z);
			}
			mVertexData.rewind();
			
			mPolygonData = Buffers.newDirectIntBuffer(P.size());
			for (int i=0; i<P.size(); i++) {
				mPolygonData.put(i);
			}
			mPolygonData.rewind();
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
		try {
			queue.put(p);
			needUpdate = true;
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	protected Iterable<Particle> particleIterator() {
		return P;
	}
	
	public void removeParticles(Collection<Particle> toRemove) {
		needUpdate = true;
		P.removeAll(toRemove);
	}
	public int size() {
		return numParticles;
	}

	
	public FloatBuffer getVertexData() {
		return mVertexData;
	}
	public IntBuffer getPolygonData() {
		return mPolygonData;
	}
	public FloatBuffer getNormalData() {
		return mNormalData;
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
		ParticleSystem copyCat = new ParticleSystem();
		for (Particle p : P) 
			copyCat.addParticle(p);
		return copyCat;
	}
	
	public boolean isOpaque() {
		return mIsOpaque;
	}
	
	public void dumpParticles() {
		queue.drainTo(P);
		
		numParticles = P.size();
	}
}
