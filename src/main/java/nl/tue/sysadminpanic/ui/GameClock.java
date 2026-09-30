package nl.tue.sysadminpanic.ui;

import javax.swing.Timer;
import nl.tue.sysadminpanic.model.GameConstants;

/**
 * Drives the simulation in real time: one tick every second, with pause.
 *
 * <p>It wraps a {@link javax.swing.Timer}, which fires on the Event Dispatch Thread.
 * Every simulation tick, and every UI update it causes, therefore happens on the
 * same thread as button clicks, so no locking or multithreading is needed.
 */
public class GameClock {

    private final Timer timer;

    /**
     * @param onTick what to run on every tick, normally {@code simulation::tick}
     */
    public GameClock(Runnable onTick) {
        timer = new Timer(GameConstants.TICK_MILLIS, event -> onTick.run());
    }

    /** Starts or resumes ticking. */
    public void start() {
        timer.start();
    }

    /** Pauses ticking; {@link #start()} resumes. */
    public void pause() {
        timer.stop();
    }

    /** @return true while the clock is ticking */
    public boolean isRunning() {
        return timer.isRunning();
    }
}
