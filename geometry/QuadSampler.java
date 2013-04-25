package geometry;

import java.util.List;

import javax.vecmath.Point3f;
import javax.vecmath.Tuple3f;

import cs5625.deferred.misc.PerlinNoise;
import cs5625.deferred.misc.Util;

public class QuadSampler extends SuperBlock {

	public QuadSampler(Tuple3f minPoint, float sideLength) {
		super(minPoint, sideLength);
	}
	
	private float evaluate(Point3f point) {
		float fracx = point.x - (float)Math.floor(point.x);
		float fracy = point.y - (float)Math.floor(point.y);
		float fracz = point.z - (float) Math.floor(point.z);
		Point3f multPoint = new Point3f(fracx, fracy, fracz);
		multPoint.scale(64);
		
		int[] val = PerlinNoise.noise[(int)multPoint.x][(int)multPoint.y][(int)multPoint.z];
		
		return (float) (point.y + 5.0 * val[0]/255);
	}
	
	public boolean hasPolygons() {
		List<Point3f> corners = this.getCorners();
		int x = 0;
		int i = 0;
		for (Point3f corner : corners) {
			if (evaluate(corner) > 0) {
				x = x | (1 << i);
			}
			i++;
		}
		if (x == 255 || x == 0) {
			return false;
		} else {
			return true;
		}
	}

}
