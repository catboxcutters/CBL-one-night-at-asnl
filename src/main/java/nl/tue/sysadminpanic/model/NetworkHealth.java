package nl.tue.sysadminpanic.model;

import java.util.Collection;

/**
 * The network health bar, from 0 to 100 percent.
 *
 * <p>Health only ever goes down: there is no natural recovery. The player limits
 * the damage by stopping whatever is causing the drain.
 */
public class NetworkHealth {

    private double value = GameConstants.START_HEALTH;

    /**
     * Calculates how much health one tick costs in the current situation.
     *
     * <p>A device that is both compromised and isolated pays both drains.
     *
     * @param devices             all devices in the network
     * @param activeExfiltrations how many campaigns sent data out during this tick
     * @return the drain for one tick, never negative
     */
    public static double drainPerTick(Collection<Device> devices, int activeExfiltrations) {
        double drain = activeExfiltrations * GameConstants.EXFILTRATION_DRAIN;
        for (Device device : devices) {
            if (device.getTrueState() == DeviceState.COMPROMISED) {
                drain += GameConstants.COMPROMISED_DRAIN_PER_CRITICALITY * device.getCriticality();
            }
            if (device.isIsolated()) {
                drain += GameConstants.ISOLATED_DRAIN_PER_CRITICALITY * device.getCriticality();
            }
        }
        return drain;
    }

    /**
     * Lowers health by the given amount, stopping at 0.
     *
     * @param amount how much health to remove
     */
    public void drain(double amount) {
        value = Math.max(0, value - amount);
    }

    /** @return the current health, from 0 to 100 */
    public double getValue() {
        return value;
    }

    /** @return true when health has reached 0 and the game is lost */
    public boolean isDepleted() {
        return value <= 0;
    }
}
