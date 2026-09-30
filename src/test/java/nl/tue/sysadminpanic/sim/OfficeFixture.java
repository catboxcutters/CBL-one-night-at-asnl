package nl.tue.sysadminpanic.sim;

import nl.tue.sysadminpanic.model.Device;
import nl.tue.sysadminpanic.model.DeviceType;
import nl.tue.sysadminpanic.model.Network;

/**
 * Test helper: the Office Network from the spec, built by hand, plus a
 * simulation running on it. (Phase 4 replaces this with the NetworkBuilder.)
 *
 * <pre>
 * INTERNET - FIREWALL - ROUTER - PC-01, PC-02, PC-03, SERVER-01
 *                                                     SERVER-01 - DB-01
 * </pre>
 */
class OfficeFixture {

    final Network network = new Network();
    final Device internet = add("INTERNET", DeviceType.INTERNET, 1);
    final Device firewall = add("FIREWALL", DeviceType.FIREWALL, 3);
    final Device router = add("ROUTER", DeviceType.ROUTER, 3);
    final Device pc1 = add("PC-01", DeviceType.WORKSTATION, 1);
    final Device pc2 = add("PC-02", DeviceType.WORKSTATION, 1);
    final Device pc3 = add("PC-03", DeviceType.WORKSTATION, 1);
    final Device server = add("SERVER-01", DeviceType.SERVER, 4);
    final Device db = add("DB-01", DeviceType.DATABASE, 5);
    final Simulation sim;

    OfficeFixture() {
        network.addLink(internet, firewall);
        network.addLink(firewall, router);
        network.addLink(router, pc1);
        network.addLink(router, pc2);
        network.addLink(router, pc3);
        network.addLink(router, server);
        network.addLink(server, db);
        sim = new Simulation(network);
    }

    private Device add(String id, DeviceType type, int criticality) {
        Device device = new Device(id, id, type, criticality, 0.5, 0.5);
        network.addDevice(device);
        return device;
    }

    /**
     * Advances the simulation until it reaches the given tick, or the game ends.
     * (Once the game is over, tick() no longer advances the clock.)
     */
    void runUntil(int tick) {
        while (sim.getCurrentTick() < tick && !sim.isGameOver()) {
            sim.tick();
        }
    }
}
