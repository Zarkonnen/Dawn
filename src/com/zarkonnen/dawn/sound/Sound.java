package com.zarkonnen.dawn.sound;

import java.util.HashMap;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;

public class Sound {
	// gun: http://www.freesound.org/people/steveygos93/sounds/91572/
	// window: http://www.freesound.org/people/Connum/sounds/66519/
	// dooropen/doorclose: http://www.freesound.org/people/ERH/sounds/31255/ and http://www.freesound.org/people/TwistedLemon/sounds/374/
	// drawer: http://www.freesound.org/people/adcbicycle/sounds/14141/ and http://www.freesound.org/people/RHumphries/sounds/1012/
	// key: http://www.freesound.org/people/klankbeeld/sounds/137063/
	// jacketget: http://www.freesound.org/people/HerbertBoland/sounds/28301/
	// punch: http://www.freesound.org/people/sagetyrtle/sounds/40158/
	// shift: http://www.freesound.org/people/MaxDemianAGL/sounds/120184/
	static HashMap<String, Clip> cache = new HashMap<String, Clip>();
	public static void play(String sound, float volume) {
		try {
			if (!cache.containsKey(sound)) {
				AudioInputStream stream = AudioSystem.getAudioInputStream(Sound.class.getResource(sound + ".wav"));
				DataLine.Info info = new DataLine.Info(Clip.class, stream.getFormat());
				Clip clip = (Clip) AudioSystem.getLine(info);
				clip.open(stream);
				cache.put(sound, clip);
			}
			Clip s = cache.get(sound);
			s.setFramePosition(0);
			try {
				FloatControl gc = (FloatControl) s.getControl(FloatControl.Type.MASTER_GAIN);
				gc.setValue(volume * 20);
			} catch (Exception e) {}
			s.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
