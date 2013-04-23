package geometry;

import java.util.List;

import javax.vecmath.Point3f;
import javax.vecmath.Tuple3f;

public class QuadSampler extends SuperBlock {

	public QuadSampler(Tuple3f minPoint, float sideLength) {
		super(minPoint, sideLength);
	}
	
	private float evaluate(Point3f point) {
		return point.y + (float)Math.sin(point.z);
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
		if (i == 255 || i == 0) {
			return false;
		} else {
			return true;
		}
	}

}
