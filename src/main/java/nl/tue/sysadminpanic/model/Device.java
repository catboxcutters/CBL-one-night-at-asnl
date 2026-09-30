package nl.tue.sysadminpanic.model;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * A single machine in the network: a node of the network graph.
 *
 * <p>A device has two states. {@code trueState} is what is really happening;
 * {@code knownState} is what the security team believes. The map shows only the
 * known state, which is how the game models incomplete information.
 */
public class Device {

    private static final int NEVER_INSPECTED = -1;

    private final String id;
    private final String name;
    private final DeviceType type;
    private final int criticality;
    private final double x;
    private final double y;
    private final Deque<LogEntry> log = new ArrayDeque<>();

    private boolean patched;
    private boolean isolated;
    private DeviceState trueState = DeviceState.HEALTHY;
    private DeviceState knownState = DeviceState.HEALTHY;
    private int lastInspectedTick = NEVER_INSPECTED;

    /**
     * Creates a healthy, unpatched, connected device.
     *
     * @param id          unique id used to look the device up in the network
     * @param name        display name, for example {@code "PC-01"}
     * @param type        the kind of device
     * @param criticality how important the device is, from 1 (low) to 5 (high)
     * @param x           horizontal map position, from 0.0 (left) to 1.0 (right)
     * @param y           vertical map position, from 0.0 (top) to 1.0 (bottom)
     * @throws IllegalArgumentException if criticality or position is out of range
     */
    public Device(String id, String name, DeviceType type, int criticality, double x, double y) {
        if (criticality < GameConstants.MIN_CRITICALITY
                || criticality > GameConstants.MAX_CRITICALITY) {
            throw new IllegalArgumentException("Criticality out of range: " + criticality);
        }
        // Positions are relative (0..1) so the map can scale them to any panel size.
        if (x < 0 || x > 1 || y < 0 || y > 1) {
            throw new IllegalArgumentException("Position must be within 0..1: " + x + ", " + y);
        }
        this.id = id;
        this.name = name;
        this.type = type;
        this.criticality = criticality;
        this.x = x;
        this.y = y;
    }

    /** @return the unique id of this device */
    public String getId() {
        return id;
    }

    /** @return the display name of this device */
    public String getName() {
        return name;
    }

    /** @return the kind of this device */
    public DeviceType getType() {
        return type;
    }

    /** @return the criticality, from 1 to 5 */
    public int getCriticality() {
        return criticality;
    }

    /** @return the relative horizontal map position, from 0.0 to 1.0 */
    public double getX() {
        return x;
    }

    /** @return the relative vertical map position, from 0.0 to 1.0 */
    public double getY() {
        return y;
    }

    /** @return whether this device is patched (immune to brute force) */
    public boolean isPatched() {
        return patched;
    }

    /**
     * Sets whether this device is patched.
     *
     * @param patched the new patched flag
     */
    public void setPatched(boolean patched) {
        this.patched = patched;
    }

    /** @return whether this device is isolated (cut off from all its links) */
    public boolean isIsolated() {
        return isolated;
    }

    /**
     * Sets whether this device is isolated.
     *
     * @param isolated the new isolated flag
     */
    public void setIsolated(boolean isolated) {
        this.isolated = isolated;
    }

    /** @return what is really happening on this device */
    public DeviceState getTrueState() {
        return trueState;
    }

    /**
     * Sets what is really happening on this device.
     *
     * @param trueState the new true state
     */
    public void setTrueState(DeviceState trueState) {
        this.trueState = trueState;
    }

    /** @return what the security team believes about this device */
    public DeviceState getKnownState() {
        return knownState;
    }

    /**
     * Sets what the security team believes about this device.
     *
     * @param knownState the new known state
     */
    public void setKnownState(DeviceState knownState) {
        this.knownState = knownState;
    }

    /**
     * Appends a line to this device's log. When the log is full, the oldest
     * line is dropped so memory use stays bounded during long games.
     *
     * @param entry the log line to add
     */
    public void addLog(LogEntry entry) {
        log.addLast(entry);
        if (log.size() > GameConstants.MAX_LOG_ENTRIES) {
            log.removeFirst();
        }
    }

    /** @return a read-only copy of this device's log, oldest line first */
    public List<LogEntry> getLog() {
        return List.copyOf(log);
    }

    /**
     * Records that the security team inspected this device at the given tick.
     *
     * @param tick the current simulation tick
     */
    public void markInspected(int tick) {
        lastInspectedTick = tick;
    }

    /** @return true if this device has been inspected at least once */
    public boolean isInspected() {
        return lastInspectedTick != NEVER_INSPECTED;
    }

    /**
     * Returns the part of the log the security team has seen: every line written
     * up to the last inspection. Like the known state, it is a snapshot, so newer
     * activity stays hidden until the device is inspected again.
     *
     * @return the revealed lines, oldest first; empty if never inspected
     */
    public List<LogEntry> getRevealedLog() {
        return log.stream().filter(entry -> entry.tick() <= lastInspectedTick).toList();
    }

    @Override
    public String toString() {
        return name;
    }
}
