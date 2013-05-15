package cs5625.deferred.sound;

import geometry.ExplosionHandler;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

import cs5625.deferred.misc.Observer;
import cs5625.deferred.misc.Observerable;
import cs5625.deferred.rendering.ShaderProgram;

public class SoundHandler implements Observer {
	
	Thread mainSoundPlayer;
	
	public SoundHandler() {
		mainSoundPlayer = new Thread(new SoundPlayer("sounds/bird.wav", Clip.LOOP_CONTINUOUSLY));
		mainSoundPlayer.start();
	}
	
	

	@Override
	public void update(Observerable o) {
		this.update(o, null);
		
	}

	@Override
	public void update(Observerable o, Object obj) {
		if (o instanceof ExplosionHandler) {
			Thread explosionPlayer = new Thread(new SoundPlayer("sounds/boom.wav", 1));
			explosionPlayer.start();
		}
	}
	
	public static class SoundPlayer implements Runnable {
		String identifier;
		int numLoop;
		public SoundPlayer(String identifier, int numLoop) {
			this.identifier = identifier;
			this.numLoop = numLoop;
		}
		
		public void run() {
			try {
				Clip clip = AudioSystem.getClip();
				AudioInputStream inputStream = AudioSystem.getAudioInputStream(
						 ShaderProgram.class.getClassLoader().getResourceAsStream(identifier));
				clip.open(inputStream);
				clip.loop(numLoop);
				clip.start();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
	}

}
