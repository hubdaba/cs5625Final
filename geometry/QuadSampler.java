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
	
	

	public static float evaluate(Point3f point, ExplosionHandler handler) {
		//int[] val = PerlinNoise.noise[(int)multPoint.x][(int)multPoint.y][(int)multPoint.z];
		//float val = (float) (Math.pow(10.0 - Math.sqrt(point.x * point.x + point.y * point.y), 2) + point.z * point.z - 9.0);
		List<Explosion> explosions = 
					handler.getExplosions(SuperBlock.midpointDistanceBlock(point, 1));
		
		float explosion_offset = 0;
		
		for (Explosion explosion : explosions) {
			float distance = point.distance(explosion.getPosition());
			float explosionRadius = ExploreSceneController.EXPLOSION_RADIUS;
			if (distance < explosionRadius) {
				explosion_offset += 200.0 * ((explosionRadius - distance)/explosionRadius);
			} 
		}
		
		// TODO: INCLUDE EXPLOSIONS!
		float val = point.y;
		Point3f tmpPnt;
		for (float i = 0.0f; i < 10.0; i+=1.0) {
			float n = 2.0f * i + 1.0f;
			tmpPnt = new Point3f(point);
			tmpPnt.scale(1.0f/n);
			val += 0.5 * PerlinNoise.noise(tmpPnt) * (n);
		}
		return (float) (val) + explosion_offset;
	}



	@Override
	public String toString() {
		return minPoint.toString();
	}
	
	
}
