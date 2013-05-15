package geometry;

import java.util.LinkedList;
import java.util.List;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

public class Explosion {
	private float radius;
	private Point3f center;
	
	public Explosion(Point3f center, float radius) {
		this.center = new Point3f(); 
		this.center.set(center);
		this.radius = radius;
	}
	
	public float getRadius() {
		return radius;
	}
	
	public Point3f getPosition() {
		return new Point3f(center);
	}
	
	public List<QuadSampler> getAffectedBlocks(float blockSize) {
		List<QuadSampler> blocksAffected = new LinkedList<QuadSampler>();
		Point3f minCorner = new Point3f(center);
		minCorner.sub(new Vector3f(radius, radius , radius ));
		Point3f maxCorner = new Point3f(center);
		maxCorner.add(new Vector3f(radius, radius, radius));
		minCorner.x = (float) (Math.floor(minCorner.x/blockSize) * blockSize);
		minCorner.y = (float) (Math.floor(minCorner.y/blockSize) * blockSize);
		minCorner.z = (float) (Math.floor(minCorner.z/blockSize) * blockSize);
		
		maxCorner.x = (float) (Math.ceil(maxCorner.x/blockSize) * blockSize);
		maxCorner.y = (float) (Math.ceil(maxCorner.y/blockSize) * blockSize);
		maxCorner.z = (float) (Math.ceil(maxCorner.z/blockSize) * blockSize);
		
		for (float i = minCorner.x; i < maxCorner.x; i+=blockSize) {
			for (float j = minCorner.y; j < maxCorner.y; j+=blockSize) {
				for (float k = minCorner.z; k < maxCorner.z; k+=blockSize) {
					QuadSampler affectedBlock =
							new QuadSampler(new Point3f(i, j, k), blockSize);
					blocksAffected.add(affectedBlock);
				}
			}
		}
		
		return blocksAffected;
	}
	
}
