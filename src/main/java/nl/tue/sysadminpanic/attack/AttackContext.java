package nl.tue.sysadminpanic.attack;

import nl.tue.sysadminpanic.model.AlertType;
import nl.tue.sysadminpanic.model.Device;
import nl.tue.sysadminpanic.model.Network;
import nl.tue.sysadminpanic.model.Severity;

/**
 * Everything an attack is allowed to do to the game world.
 *
 * <p>Stages and strategies only talk to the world through this interface. The
 * simulation implements it, so attack code never depends on the {@code sim}
 * package, and the simulation can publish an event for every change it makes.
 */
public interface AttackContext {

    /** @return the network being attacked */
    Network getNetwork();

    /**
     * Writes a line to a device's log, stamped with the current tick.
     *
     * @param device the device whose log gets the line
     * @param text   the log message
     */
    void log(Device device, String text);

    /**
     * Raises a real (not false-positive) alert.
     *
     * @param type     what kind of activity was detected
     * @param sourceIp where the activity came from
     * @param target   the device the alert is about
     * @param severity how serious the alert is
     * @param summary  one-line description
     */
    void raiseAlert(AlertType type, String sourceIp, Device target, Severity severity,
            String summary);

    /**
     * Marks a device as really compromised. Its known state does not change.
     *
     * @param device the device the attacker now controls
     */
    void compromise(Device device);

    /**
     * Records that data was sent out during this tick, which drains health.
     *
     * @param source the device the data was stolen from
     */
    void reportExfiltration(Device source);

    /**
     * Called once when a campaign ends, whatever the reason.
     *
     * @param campaign the campaign that just ended
     */
    void campaignEnded(AttackCampaign campaign);
}
