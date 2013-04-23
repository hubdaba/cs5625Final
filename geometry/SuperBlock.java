package geometry;

import java.util.LinkedList;
import java.util.List;

import javax.vecmath.Point3f;
import javax.vecmath.Tuple3f;
import javax.vecmath.Vector3f;

import cs5625.deferred.misc.Util;

public class SuperBlock {
	protected Point3f minPoint;
	protected float sideLength;

	public static SuperBlock midpointDistanceBlock(Tuple3f midPoint, float sideLength) {
		Point3f minPoint = new Point3f(midPoint);
		minPoint.sub(new Vector3f(sideLength, sideLength, sideLength));
		return new SuperBlock(minPoint, 2 * sideLength);
	}

	public SuperBlock(Tuple3f minPoint, float sideLength) {
		this.minPoint = new Point3f(minPoint);
		this.sideLength = sideLength;
	}


	public Point3f getMinPoint() {
		return new Point3f(minPoint);
	}

	public Point3f getMaxPoint() {
		Point3f maxPoint = new Point3f(minPoint);
		maxPoint.add(new Point3f(sideLength, sideLength, sideLength));
		return maxPoint;

	}

	public float getSideLength() {
		return sideLength;
	}

	public List<Point3f> getCorners() {
		List<Point3f> corners = new LinkedList<Point3f>();
		Point3f maxPoint = new Point3f(minPoint);
		maxPoint.add(new Point3f(sideLength, sideLength, sideLength));
		corners.add(new Point3f(minPoint));
		corners.add(new Point3f(maxPoint));
		corners.add(new Point3f(minPoint.x, minPoint.y, maxPoint.z));
		corners.add(new Point3f(minPoint.x, maxPoint.y, maxPoint.z));
		corners.add(new Point3f(minPoint.x, maxPoint.y, minPoint.z));
		corners.add(new Point3f(maxPoint.x, minPoint.y, minPoint.z));
		corners.add(new Point3f(maxPoint.x, maxPoint.y, minPoint.z));
		corners.add(new Point3f(maxPoint.x, minPoint.y, maxPoint.z));
		return corners;
	}

	public Point3f getMidPoint() {
		Point3f midPoint = new Point3f(minPoint);
		midPoint.add(new Vector3f(sideLength/2, sideLength/2, sideLength/2));
		return midPoint;
	}

	public boolean containsBlock(SuperBlock block) {
		Point3f maxPoint = new Point3f(minPoint);
		maxPoint.add(new Point3f(sideLength, sideLength, sideLength));
		for (Point3f corner : block.getCorners()) {
			if (corner.x > maxPoint.x || corner.x < minPoint.x) {
				continue;
			}
			if (corner.y > maxPoint.y || corner.y < minPoint.y) {
				continue;
			}
			if (corner.z > maxPoint.z || corner.z < minPoint.z) {
				continue;
			}
			return true;
		}

		return false;
	}

	public boolean completelyContains(SuperBlock block) {
		Point3f maxPoint = new Point3f(minPoint);
		maxPoint.add(new Point3f(sideLength, sideLength, sideLength));
		for (Point3f corner : block.getCorners()) {
			if (corner.x > maxPoint.x || corner.x < minPoint.x) {
				return false;
			}
			if (corner.y > maxPoint.y || corner.y < minPoint.y) {
				return false;
			}
			if (corner.z > maxPoint.z || corner.z < minPoint.z) {
				return false;
			}

		}

		return true;
	}
}
