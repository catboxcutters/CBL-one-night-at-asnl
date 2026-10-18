package nl.tue.sysadminpanic.ui;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * A WAV sound from the classpath that either loops until {@link #stop()} is
 * called ({@link #loop}) or plays through once ({@link #playOnce}).
 *
 * <p>Playback problems (missing file, no audio device, unsupported format) never
 * throw: they are reported on stderr and the returned object simply does nothing,
 * so the game keeps working without sound.
 */
public final class GameSound {

    /** The open clip, or null if nothing is playing. */
    private Clip clip;

    private GameSound(Clip clip) {
        this.clip = clip;
    }

    /**
     * Starts a sound that repeats until {@link #stop()}.
     *
     * @param resource classpath location of the WAV file, e.g. {@code "/audio/theme.wav"}
     * @param volume   linear volume from 0.0 (silent) to 1.0 (the file's own level)
     * @return a handle to stop the sound; never null
     */
    public static GameSound loop(String resource, double volume) {
        return start(resource, volume, true);
    }

    /**
     * Plays a sound once. It frees its audio line by itself when it ends, and
     * can be cut short with {@link #stop()}.
     *
     * @param resource classpath location of the WAV file
     * @param volume   linear volume from 0.0 (silent) to 1.0 (the file's own level)
     * @return a handle to stop the sound; never null
     */
    public static GameSound playOnce(String resource, double volume) {
        return start(resource, volume, false);
    }

    /** Stops the sound and frees the audio line. Safe to call more than once. */
    public void stop() {
        if (clip != null) {
            clip.stop();
            clip.close();
            clip = null;
        }
    }

    private static GameSound start(String resource, double volume, boolean repeat) {
        Clip clip = null;
        try (InputStream raw = GameSound.class.getResourceAsStream(resource)) {
            if (raw == null) {
                System.err.println("Sound not found: " + resource);
                return new GameSound(null);
            }
            // BufferedInputStream is required: AudioSystem needs mark/reset support.
            try (AudioInputStream audio = AudioSystem.getAudioInputStream(new BufferedInputStream(raw))) {
                clip = AudioSystem.getClip();
                clip.open(audio);
            }
            setVolume(clip, volume);
            if (repeat) {
                clip.loop(Clip.LOOP_CONTINUOUSLY);
            } else {
                Clip finished = clip;
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        finished.close();
                    }
                });
                clip.start();
            }
            return new GameSound(clip);
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException
                | IllegalArgumentException e) {
            System.err.println("Could not play " + resource + ": " + e);
            if (clip != null) {
                clip.close();
            }
            return new GameSound(null);
        }
    }

    /** Scales the clip's amplitude; skipped silently if the device has no gain control. */
    private static void setVolume(Clip clip, double volume) {
        if (!clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            return;
        }
        FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        double decibels = 20.0 * Math.log10(Math.max(volume, 0.0001));
        gain.setValue((float) Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), decibels)));
    }
}
