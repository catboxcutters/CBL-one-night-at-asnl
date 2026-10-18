package nl.tue.sysadminpanic.model;

/**
 * Every balancing number of the game in one place.
 *
 * <p>Keeping prices, drain rates and timings here means the game can be tuned
 * without touching any logic. All durations are in ticks (1 tick = 1 second).
 */
public final class GameConstants {

    // --- Time ---

    /** In-game clock value at tick 0, in seconds since midnight (00:00:00, 12 AM). */
    public static final int CLOCK_START_SECONDS = 0;

    /**
     * In-game seconds that pass per tick. One tick is three in-game minutes, so
     * 12 AM to 6 AM takes 120 ticks: about as long as all five waves last.
     */
    public static final int CLOCK_SECONDS_PER_TICK = 180;

    /** Real-time length of one tick, in milliseconds. */
    public static final int TICK_MILLIS = 1000;

    // --- Money ---

    /** Money the player starts with, in dollars. */
    public static final int START_MONEY = 1500;

    /** Dollars added every {@link #INCOME_INTERVAL_TICKS} ticks. */
    public static final int INCOME_AMOUNT = 50;

    /** Number of ticks between two income payments. */
    public static final int INCOME_INTERVAL_TICKS = 5;

    // --- Action prices (dollars) ---

    /** Price of the Inspect action. */
    public static final int PRICE_INSPECT = 0;

    /** Price of the Block IP action. */
    public static final int PRICE_BLOCK_IP = 200;

    /** Price of the Isolate action. */
    public static final int PRICE_ISOLATE = 300;

    /** Price of reconnecting an isolated device. */
    public static final int PRICE_RECONNECT = 0;

    /** Price of the Patch action. */
    public static final int PRICE_PATCH = 800;

    /** Price of the Firewall rule action. */
    public static final int PRICE_FIREWALL_RULE = 500;

    /** Price of the Restore action. */
    public static final int PRICE_RESTORE = 1500;

    // --- Network health ---

    /** Health at the start of the game, in percent. */
    public static final double START_HEALTH = 100.0;

    /** Health drained per tick per compromised device, multiplied by its criticality. */
    public static final double COMPROMISED_DRAIN_PER_CRITICALITY = 0.1;

    /** Health drained per tick per isolated device, multiplied by its criticality. */
    public static final double ISOLATED_DRAIN_PER_CRITICALITY = 0.05;

    /** Health drained per tick while data exfiltration is active. */
    public static final double EXFILTRATION_DRAIN = 1.0;

    // --- Attack timings ---

    /** Duration of the port-scan (recon) stage. */
    public static final int RECON_TICKS = 5;

    /** Ticks a brute-force attack needs to succeed on an unpatched device. */
    public static final int BRUTE_FORCE_TICKS = 15;

    /** Ticks a phishing attack needs to succeed. */
    public static final int PHISHING_TICKS = 3;

    /** Ticks per lateral-movement hop onto an unpatched device. */
    public static final int HOP_TICKS = 10;

    /** Ticks per lateral-movement hop onto a patched device. */
    public static final int PATCHED_HOP_TICKS = 20;

    /** Duration of the exfiltration stage. */
    public static final int EXFILTRATION_TICKS = 20;

    // --- Waves ---

    /** Ticks between the game start and the first wave. */
    public static final int FIRST_WAVE_DELAY_TICKS = 10;

    /** Ticks between the end of one wave and the start of the next. */
    public static final int WAVE_GAP_TICKS = 15;

    // --- Devices ---

    /** Lowest allowed device criticality. */
    public static final int MIN_CRITICALITY = 1;

    /** Highest allowed device criticality. */
    public static final int MAX_CRITICALITY = 5;

    /** Maximum number of log entries a device keeps; older entries are dropped. */
    public static final int MAX_LOG_ENTRIES = 100;

    private GameConstants() {
        // Constants holder: never instantiated.
    }
}
