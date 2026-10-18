package nl.tue.sysadminpanic.ui;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * A sound that loops until {@link #stop()} is called.
 *
 * <p>Playback problems (missing file, no audio device, unsupported format) never
 * throw: they are reported on stderr and the returned object simply does nothing,
 * so the game keeps working without sound.
 */
public final class LoopingSound {

    /** The playing clip, or null if nothing is playing. */
    private Clip clip;

    private LoopingSound(Clip clip) {
        this.clip = clip;
    }

    /**
     * Starts looping a WAV file from the classpath.
     *
     * @param resource classpath location of the WAV file, e.g. {@code "/audio/theme.wav"}
     * @param volume   linear volume from 0.0 (silent) to 1.0 (the file's own level)
     * @return a handle to stop the sound; never null
     */
    public static LoopingSound play(String resource, double volume) {
        Clip clip = null;
        try (InputStream raw = LoopingSound.class.getResourceAsStream(resource)) {
            if (raw == null) {
                System.err.println("Sound not found: " + resource);
                return new LoopingSound(null);
            }
            // BufferedInputStream is required: AudioSystem needs mark/reset support.
            try (AudioInputStream audio = AudioSystem.getAudioInputStream(new BufferedInputStream(raw))) {
                clip = AudioSystem.getClip();
                clip.open(audio);
            }
            setVolume(clip, volume);
            clip.loop(Clip.LOOP_CONTINUOUSLY);
            return new LoopingSound(clip);
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException
                | IllegalArgumentException e) {
            System.err.println("Could not play " + resource + ": " + e);
            if (clip != null) {
                clip.close();
            }
            return new LoopingSound(null);
        }
    }

    /** Stops the sound and frees the audio line. Safe to call more than once. */
    public void stop() {
        if (clip != null) {
            clip.stop();
            clip.close();
            clip = null;
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
