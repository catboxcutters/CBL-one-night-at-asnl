package nl.tue.sysadminpanic.model;

/**
 * An undirected network connection between two devices: an edge of the network graph.
 *
 * <p>A link can be blocked by a firewall rule. A blocked link still exists
 * (so it can be drawn and unblocked), but traffic cannot pass through it.
 */
public class Link {

    private final Device first;
    private final Device second;
    private boolean blocked;

    /**
     * Creates an unblocked link between two different devices.
     *
     * @param first  one end of the link
     * @param second the other end of the link
     * @throws IllegalArgumentException if both ends are the same device
     */
    public Link(Device first, Device second) {
        if (first == second) {
            throw new IllegalArgumentException("A link needs two different devices: " + first);
        }
        this.first = first;
        this.second = second;
    }

    /** @return one end of the link */
    public Device getFirst() {
        return first;
    }

    /** @return the other end of the link */
    public Device getSecond() {
        return second;
    }

    /**
     * Returns the device at the opposite end of this link.
     *
     * @param device one of the two ends of this link
     * @return the other end
     * @throws IllegalArgumentException if the device is not an end of this link
     */
    public Device other(Device device) {
        if (device == first) {
            return second;
        }
        if (device == second) {
            return first;
        }
        throw new IllegalArgumentException(device + " is not an end of this link");
    }

    /**
     * Checks whether this link has the given device as one of its ends.
     *
     * @param device the device to check
     * @return true if the device is an end of this link
     */
    public boolean connects(Device device) {
        return device == first || device == second;
    }

    /** @return whether a firewall rule blocks this link */
    public boolean isBlocked() {
        return blocked;
    }

    /**
     * Sets whether a firewall rule blocks this link.
     *
     * @param blocked the new blocked flag
     */
    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    @Override
    public String toString() {
        return first + " - " + second;
    }
}
