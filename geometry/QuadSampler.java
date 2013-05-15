package geometry;

import java.util.List;

import javax.vecmath.Point3f;
import javax.vecmath.Tuple3f;

import cs5625.deferred.apps.ExploreSceneController;
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
	
	


	@Override
	public String toString() {
		return minPoint.toString();
	}
	
	
}
