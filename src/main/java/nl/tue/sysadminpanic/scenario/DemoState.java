package nl.tue.sysadminpanic.scenario;

import nl.tue.sysadminpanic.model.Device;
import nl.tue.sysadminpanic.model.DeviceState;
import nl.tue.sysadminpanic.model.LogEntry;
import nl.tue.sysadminpanic.model.Network;

/**
 * A fixed snapshot of an attack in progress, used to demonstrate the map and
 * the Inspect action before the attack simulation exists.
 *
 * <p>Only the <em>true</em> states and logs are set. The known states stay
 * HEALTHY, so the map looks fine until the player inspects a device: that is the
 * hidden-information idea the full game is built on.
 */
public final class DemoState {

    /** The moment the snapshot is taken; every demo log line is at or before it. */
    public static final int SNAPSHOT_TICK = 30;

    private static final String ATTACKER_IP = "203.0.113.42";

    private DemoState() {
        // Utility class: never instantiated.
    }

    /**
     * Writes the demo logs and true states into a freshly built office network.
     *
     * @param network a network created by {@link OfficeNetworkScenario}
     */
    public static void apply(Network network) {
        Device firewall = network.getDevice("FIREWALL");
        log(firewall, 2, "Connection attempt on port 22 from " + ATTACKER_IP);
        log(firewall, 3, "Connection attempt on port 80 from " + ATTACKER_IP);
        log(firewall, 4, "Connection attempt on port 3306 from " + ATTACKER_IP);

        Device server = network.getDevice("SERVER-01");
        log(server, 10, "Failed login: admin from " + ATTACKER_IP);
        log(server, 11, "Failed login: admin from " + ATTACKER_IP);
        log(server, 12, "Failed login: root from " + ATTACKER_IP);
        server.setTrueState(DeviceState.SUSPICIOUS);

        Device workstation = network.getDevice("PC-02");
        log(workstation, 18, "Opened attachment: invoice_october.pdf.exe");
        log(workstation, 19, "New process: svch0st.exe (unsigned)");
        log(workstation, 25, "Outbound connection to 198.51.100.7:443 (unknown host)");
        workstation.setTrueState(DeviceState.COMPROMISED);
    }

    private static void log(Device device, int tick, String text) {
        device.addLog(new LogEntry(tick, device.getId(), text));
    }
}
