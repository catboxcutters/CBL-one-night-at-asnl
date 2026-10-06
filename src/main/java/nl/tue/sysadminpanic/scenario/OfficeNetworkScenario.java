package nl.tue.sysadminpanic.scenario;

import nl.tue.sysadminpanic.model.DeviceType;
import nl.tue.sysadminpanic.model.Network;

/**
 * The one scenario of the game: a small office network.
 *
 * <pre>
 * INTERNET - FIREWALL - ROUTER - PC-01, PC-02, PC-03, SERVER-01
 *                                                     SERVER-01 - DB-01
 * </pre>
 */
public class OfficeNetworkScenario implements Scenario {

    private static final String DATABASE = "DB-01";
    private static final String SERVER = "SERVER-01";

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
}
