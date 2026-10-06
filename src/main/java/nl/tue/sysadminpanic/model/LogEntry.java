package nl.tue.sysadminpanic.model;

/**
 * One immutable line in a device's log.
 *
 * @param tick     the simulation tick at which the line was written
 * @param deviceId the id of the device that wrote the line
 * @param text     the log message, for example {@code "Failed login: admin from 203.0.113.42"}
 */
public record LogEntry(int tick, String deviceId, String text) {

    /**
     * Formats the line for display, for example
     * {@code "00:15:00  Failed login: admin from 203.0.113.42"}.
     *
     * @return the clock time followed by the text
     */
    public String formatted() {
        return GameTime.format(tick) + "  " + text;
    }
}
