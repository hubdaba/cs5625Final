package cs5625.deferred.misc;

public interface Observer {
	public void update(Observerable o);
	
	public void update(Observerable o, Object obj);
}
