package geometry;

import java.util.LinkedList;
import java.util.List;

import cs5625.deferred.misc.Observer;
import cs5625.deferred.misc.Observerable;

public class ExplosionHandler implements Observerable {
	private List<Explosion> explosions;
	private List<Observer> observers;
	
	public ExplosionHandler() {
		observers = new LinkedList<Observer>();
		explosions = new LinkedList<Explosion>();
	}
	
	public void addExplosion(Explosion explosion) {
		explosions.add(explosion);
		notifyObservers(explosion);
	}

	@Override
	public void addObserver(Observer o) {
		observers.add(o);
	}

	public void notifyObservers(Explosion explosion) {
		for (Observer o : observers) {
			o.update(this, explosion);
		}
	}
	
	public List<Explosion> getExplosions(SuperBlock b) {
		
		List<Explosion> containedExplosions = new LinkedList<Explosion>();
		for (Explosion explosion : explosions) {
			SuperBlock enclosingBlock =
						SuperBlock.midpointDistanceBlock(explosion.getPosition(), explosion.getRadius());
			if (b.containsBlock(enclosingBlock)) {
				containedExplosions.add(explosion);
			}
			
		}
		return containedExplosions;
	}
	
	public static class ExplosionException extends Exception {
		public ExplosionException() {
			
		}
		public ExplosionException(String message) {
			super(message);
		}
	}
	
}
