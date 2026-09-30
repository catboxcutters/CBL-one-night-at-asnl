package nl.tue.sysadminpanic.model;

/**
 * Converts simulation ticks into in-game clock time.
 */
public final class GameTime {

    private static final int SECONDS_PER_MINUTE = 60;
    private static final int SECONDS_PER_HOUR = 60 * 60;
    private static final int HOURS_PER_DAY = 24;

    private GameTime() {
        // Utility class: never instantiated.
    }

    /**
     * Formats a tick as clock time. Tick 0 is 09:00:00 and each tick is one second.
     *
     * @param tick the simulation tick
     * @return the clock time as {@code HH:MM:SS}
     */
    public static String format(int tick) {
        int seconds = GameConstants.CLOCK_START_SECONDS + tick;
        int hours = (seconds / SECONDS_PER_HOUR) % HOURS_PER_DAY;
        int minutes = (seconds / SECONDS_PER_MINUTE) % SECONDS_PER_MINUTE;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds % SECONDS_PER_MINUTE);
    }
}
