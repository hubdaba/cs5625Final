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
	
	

	public static float evaluate(Point3f point) {
		//int[] val = PerlinNoise.noise[(int)multPoint.x][(int)multPoint.y][(int)multPoint.z];
		//float val = (float) (Math.pow(10.0 - Math.sqrt(point.x * point.x + point.y * point.y), 2) + point.z * point.z - 9.0);
		
		// TODO: INCLUDE EXPLOSIONS!
		float val = point.y;
		Point3f tmpPnt;
		for (float i = 0.0f; i < 10.0; i+=1.0) {
			float n = 2.0f * i + 1.0f;
			tmpPnt = new Point3f(point);
			tmpPnt.scale(1.0f/n);
			val += 0.5 * PerlinNoise.noise(tmpPnt) * (n);
		}
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
		//return true;
	}

	@Override
	public String toString() {
		return minPoint.toString();
	}
	
	
}
