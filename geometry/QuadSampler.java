package geometry;

import javax.vecmath.Point3f;
import javax.vecmath.Tuple3f;

import cs5625.deferred.misc.PerlinNoise;

public class QuadSampler extends SuperBlock {
	
	

	public QuadSampler(Tuple3f minPoint, float sideLength) {
		super(minPoint, sideLength);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof QuadSampler) {
			QuadSampler quad = (QuadSampler) obj;
			if (quad.minPoint.equals(this.minPoint) && quad.sideLength == this.sideLength){
				return true;
			}
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return minPoint.hashCode();
	}
	
	

	public float evaluate(Point3f point) {
		//int[] val = PerlinNoise.noise[(int)multPoint.x][(int)multPoint.y][(int)multPoint.z];
		//float val = (float) (Math.pow(10.0 - Math.sqrt(point.x * point.x + point.y * point.y), 2) + point.z * point.z - 9.0);
		Point3f point2 = new Point3f(point);
		point2.scale(.2f);
		Point3f point3 = new Point3f(point);
		point3.scale(0.01f);
		float val = point.y + PerlinNoise.noise(point) + 10 * PerlinNoise.noise(point2)
				+ 20 * PerlinNoise.noise(point3);
		return (float) (val);
	}

	
	public boolean hasPolygons() {
		boolean hasNeg = false;
		boolean hasPos = false;
		float voxelSize = sideLength/10;
		for (float x = minPoint.x; x <= minPoint.x + sideLength; x+= voxelSize) {
			for (float y = minPoint.y; y <= minPoint.y + sideLength; y += voxelSize) {
				for (float z = minPoint.z; z <= minPoint.z + sideLength; z += voxelSize) {
					Point3f voxelCorner = new Point3f(x, y, z);
					float val = evaluate(voxelCorner);
					if (val > 0) {
						hasPos = true;
					} else {
						hasNeg = true;
					}
					if (hasPos && hasNeg) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return minPoint.toString();
	}
	
	
}
