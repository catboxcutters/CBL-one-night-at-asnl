package nl.tue.sysadminpanic.scenario;

import java.util.List;
import nl.tue.sysadminpanic.attack.BruteForceStrategy;
import nl.tue.sysadminpanic.attack.InitialAccessStrategy;
import nl.tue.sysadminpanic.attack.PhishingStrategy;
import nl.tue.sysadminpanic.model.AlertType;
import nl.tue.sysadminpanic.model.DeviceType;
import nl.tue.sysadminpanic.model.Network;
import nl.tue.sysadminpanic.model.Severity;

/**
 * The one scenario of the game: a small office network hit by five waves.
 *
 * <pre>
 * INTERNET - FIREWALL - ROUTER - PC-01, PC-02, PC-03, SERVER-01
 *                                                     SERVER-01 - DB-01
 * </pre>
 *
 * <p>The wave script below is plain data, so it can be read and tuned without
 * knowing how the engine works. IPs come from the documentation-only ranges
 * 203.0.113.0/24 and 198.51.100.0/24, so none of them belong to a real host.
 */
public class OfficeNetworkScenario implements Scenario {

    private static final String DATABASE = "DB-01";
    private static final String SERVER = "SERVER-01";

    // Stateless, so one instance of each can safely be shared by every campaign.
    private static final InitialAccessStrategy BRUTE_FORCE = new BruteForceStrategy();
    private static final InitialAccessStrategy PHISHING = new PhishingStrategy();

    private static final boolean WITH_RECON = true;
    private static final boolean NO_RECON = false;

    @Override
    public String getName() {
        return "Office Network";
    }

    @Override
    public Network createNetwork() {
        return new NetworkBuilder()
                .device("INTERNET", DeviceType.INTERNET, 1, 0.07, 0.50)
                .device("FIREWALL", DeviceType.FIREWALL, 3, 0.25, 0.50)
                .device("ROUTER", DeviceType.ROUTER, 3, 0.45, 0.50)
                .device("PC-01", DeviceType.WORKSTATION, 1, 0.68, 0.12)
                .device("PC-02", DeviceType.WORKSTATION, 1, 0.68, 0.37)
                .device("PC-03", DeviceType.WORKSTATION, 1, 0.68, 0.62)
                .device(SERVER, DeviceType.SERVER, 4, 0.68, 0.88)
                .device(DATABASE, DeviceType.DATABASE, 5, 0.92, 0.88)
                .link("INTERNET", "FIREWALL")
                .link("FIREWALL", "ROUTER")
                .link("ROUTER", "PC-01")
                .link("ROUTER", "PC-02")
                .link("ROUTER", "PC-03")
                .link("ROUTER", SERVER)
                .link(SERVER, DATABASE)
                .build();
    }

    @Override
    public List<WaveDefinition> getWaves() {
        return List.of(
                new WaveDefinition("Knock knock", List.of(
                        // Recon lasts 5 ticks, so the brute force starts at t+5 by itself.
                        attack(0, "203.0.113.42", SERVER, BRUTE_FORCE, WITH_RECON))),
                new WaveDefinition("Special delivery", List.of(
                        attack(0, "203.0.113.66", "PC-02", PHISHING, NO_RECON),
                        unusualOutbound(15, "203.0.113.66", "PC-02"))),
                new WaveDefinition("Crying wolf", List.of(
                        knownScannerFalsePositive(0, "198.51.100.200", SERVER),
                        attack(10, "198.51.100.23", "PC-01", BRUTE_FORCE, WITH_RECON))),
                new WaveDefinition("Two fronts", List.of(
                        attack(0, "203.0.113.77", SERVER, BRUTE_FORCE, NO_RECON),
                        attack(0, "198.51.100.140", "PC-03", PHISHING, NO_RECON))),
                new WaveDefinition("The big one", List.of(
                        attack(0, "203.0.113.201", "PC-01", PHISHING, NO_RECON),
                        attack(5, "198.51.100.99", SERVER, BRUTE_FORCE, NO_RECON),
                        mistypedPasswordFalsePositive(10, "PC-02"))));
    }

    /** An attack campaign aimed at the database. */
    private static ScriptedEvent attack(int offset, String ip, String entryDevice,
            InitialAccessStrategy strategy, boolean withRecon) {
        return new ScriptedEvent.StartCampaign(offset, ip, entryDevice, DATABASE, strategy,
                withRecon);
    }

    /** The hint that reveals a silent phishing attack. */
    private static ScriptedEvent unusualOutbound(int offset, String ip, String device) {
        return new ScriptedEvent.ScriptedAlert(offset, AlertType.UNUSUAL_OUTBOUND, ip, device,
                Severity.MEDIUM, "Unusual outbound connection from " + device + " to " + ip,
                false, List.of("Outbound connection to " + ip + ":443 (unknown host)"));
    }

    /** A harmless scan by the company's own security scanner. */
    private static ScriptedEvent knownScannerFalsePositive(int offset, String ip, String device) {
        return new ScriptedEvent.ScriptedAlert(offset, AlertType.PORT_SCAN, ip, device,
                Severity.LOW, "Port scan detected on " + device + " from " + ip, true,
                List.of("Connection from " + ip + ": scheduled vulnerability scan"
                        + " (IT security, change ticket #4471)"));
    }

    /** A user who typed their password wrong a couple of times. */
    private static ScriptedEvent mistypedPasswordFalsePositive(int offset, String device) {
        String ip = "10.0.0.12";
        return new ScriptedEvent.ScriptedAlert(offset, AlertType.BRUTE_FORCE, ip, device,
                Severity.LOW, "Failed logins on " + device + " from " + ip, true,
                List.of("Failed login: m.jansen from " + ip,
                        "Failed login: m.jansen from " + ip,
                        "Successful login: m.jansen from " + ip + " (caps lock was on)"));
    }
}
