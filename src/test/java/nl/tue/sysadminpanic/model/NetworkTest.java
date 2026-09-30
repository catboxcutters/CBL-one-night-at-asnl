package nl.tue.sysadminpanic.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for the network graph: wiring, reachability, shortest paths,
 * isolation and firewall blocking.
 *
 * <p>The graph mirrors the Office Network scenario:
 * <pre>
 * INTERNET - FIREWALL - ROUTER - PC-01, PC-02, PC-03, SERVER-01
 *                                                     SERVER-01 - DB-01
 * </pre>
 */
class NetworkTest {

    private Network network;
    private Device internet;
    private Device firewall;
    private Device router;
    private Device pc1;
    private Device pc2;
    private Device pc3;
    private Device server;
    private Device db;

    @BeforeEach
    void buildOfficeNetwork() {
        network = new Network();
        internet = add("INTERNET", DeviceType.INTERNET, 1);
        firewall = add("FIREWALL", DeviceType.FIREWALL, 3);
        router = add("ROUTER", DeviceType.ROUTER, 3);
        pc1 = add("PC-01", DeviceType.WORKSTATION, 1);
        pc2 = add("PC-02", DeviceType.WORKSTATION, 1);
        pc3 = add("PC-03", DeviceType.WORKSTATION, 1);
        server = add("SERVER-01", DeviceType.SERVER, 4);
        db = add("DB-01", DeviceType.DATABASE, 5);
        network.addLink(internet, firewall);
        network.addLink(firewall, router);
        network.addLink(router, pc1);
        network.addLink(router, pc2);
        network.addLink(router, pc3);
        network.addLink(router, server);
        network.addLink(server, db);
    }

    private Device add(String id, DeviceType type, int criticality) {
        Device device = new Device(id, id, type, criticality, 0.5, 0.5);
        network.addDevice(device);
        return device;
    }

    // --- Wiring ---

    @Test
    void neighborsListsAllDirectlyLinkedDevicesInLinkOrder() {
        assertEquals(List.of(firewall, pc1, pc2, pc3, server), network.neighbors(router));
        assertEquals(List.of(server), network.neighbors(db));
    }

    @Test
    void neighborsIncludesBlockedLinksAndIsolatedDevices() {
        network.findLink(router, pc1).orElseThrow().setBlocked(true);
        pc2.setIsolated(true);
        assertEquals(List.of(firewall, pc1, pc2, pc3, server), network.neighbors(router));
    }

    @Test
    void findLinkWorksInBothDirections() {
        Link link = network.findLink(server, db).orElseThrow();
        assertEquals(link, network.findLink(db, server).orElseThrow());
        assertTrue(network.findLink(pc1, db).isEmpty());
    }

    @Test
    void getDeviceFindsDeviceById() {
        assertEquals(db, network.getDevice("DB-01"));
        assertThrows(IllegalArgumentException.class, () -> network.getDevice("NOPE"));
    }

    @Test
    void getFirstOfTypeFindsDeviceByType() {
        assertEquals(internet, network.getFirstOfType(DeviceType.INTERNET));
        assertEquals(pc1, network.getFirstOfType(DeviceType.WORKSTATION));
        assertThrows(IllegalStateException.class, () -> new Network().getFirstOfType(DeviceType.SERVER));
    }

    @Test
    void addLinkRejectsDuplicateLinkInEitherDirection() {
        assertThrows(IllegalArgumentException.class, () -> network.addLink(server, db));
        assertThrows(IllegalArgumentException.class, () -> network.addLink(db, server));
    }

    @Test
    void addLinkRejectsSelfLoop() {
        assertThrows(IllegalArgumentException.class, () -> network.addLink(pc1, pc1));
    }

    @Test
    void addLinkRejectsDeviceOutsideNetwork() {
        Device stranger = new Device("X", "X", DeviceType.SERVER, 1, 0, 0);
        assertThrows(IllegalArgumentException.class, () -> network.addLink(pc1, stranger));
    }

    @Test
    void addDeviceRejectsDuplicateId() {
        Device copy = new Device("PC-01", "PC-01", DeviceType.WORKSTATION, 1, 0, 0);
        assertThrows(IllegalArgumentException.class, () -> network.addDevice(copy));
    }

    // --- Reachability ---

    @Test
    void everyDeviceIsReachableInIntactNetwork() {
        assertTrue(network.isReachable(internet, db));
        assertTrue(network.isReachable(db, internet));
        assertTrue(network.isReachable(pc1, pc3));
    }

    @Test
    void deviceReachesItself() {
        assertTrue(network.isReachable(pc1, pc1));
        assertEquals(List.of(pc1), network.shortestPath(pc1, pc1));
    }

    @Test
    void reachableSetOfIntactNetworkContainsEveryDevice() {
        assertEquals(Set.copyOf(network.getDevices()), network.reachableSet(internet));
    }

    @Test
    void reachableSetIsInBreadthFirstOrder() {
        List<Device> expected = List.of(internet, firewall, router, pc1, pc2, pc3, server, db);
        assertEquals(expected, List.copyOf(network.reachableSet(internet)));
    }

    // --- Shortest path ---

    @Test
    void shortestPathFromInternetToDatabase() {
        List<Device> expected = List.of(internet, firewall, router, server, db);
        assertEquals(expected, network.shortestPath(internet, db));
    }

    @Test
    void shortestPathFromWorkstationToDatabase() {
        assertEquals(List.of(pc2, router, server, db), network.shortestPath(pc2, db));
    }

    @Test
    void shortestPathPicksFewerHopsWhenThereIsAShortcut() {
        // A direct cable from PC-01 to the server skips the router.
        network.addLink(pc1, server);
        assertEquals(List.of(pc1, server, db), network.shortestPath(pc1, db));
    }

    @Test
    void shortestPathIsEmptyWhenUnreachable() {
        server.setIsolated(true);
        assertEquals(List.of(), network.shortestPath(pc1, db));
    }

    // --- Isolation ---

    @Test
    void isolatingIntermediateDeviceCutsOffEverythingBehindIt() {
        server.setIsolated(true);
        assertFalse(network.isReachable(internet, db));
        assertFalse(network.isReachable(internet, server));
        assertTrue(network.isReachable(internet, pc1));
    }

    @Test
    void isolatedDeviceCannotReachAnything() {
        pc1.setIsolated(true);
        assertFalse(network.isReachable(pc1, router));
        assertFalse(network.isReachable(pc1, pc1));
        assertTrue(network.reachableSet(pc1).isEmpty());
    }

    @Test
    void isolatedDeviceIsMissingFromOthersReachableSet() {
        pc3.setIsolated(true);
        Set<Device> reachable = network.reachableSet(internet);
        assertFalse(reachable.contains(pc3));
        assertEquals(network.getDevices().size() - 1, reachable.size());
    }

    @Test
    void reconnectingIsolatedDeviceRestoresReachability() {
        router.setIsolated(true);
        assertFalse(network.isReachable(pc1, db));
        router.setIsolated(false);
        assertTrue(network.isReachable(pc1, db));
    }

    @Test
    void isolationForcesPathAroundDeviceWhenAlternativeExists() {
        network.addLink(pc1, server);
        router.setIsolated(true);
        assertEquals(List.of(pc1, server, db), network.shortestPath(pc1, db));
    }

    // --- Firewall blocking ---

    @Test
    void blockedLinkBreaksReachabilityInBothDirections() {
        network.findLink(server, db).orElseThrow().setBlocked(true);
        assertFalse(network.isReachable(internet, db));
        assertFalse(network.isReachable(db, internet));
    }

    @Test
    void blockedLinkKeepsBothEndsWorkingOtherwise() {
        network.findLink(router, pc1).orElseThrow().setBlocked(true);
        assertFalse(network.isReachable(router, pc1));
        assertTrue(network.isReachable(router, pc2));
        assertTrue(network.isReachable(internet, router));
    }

    @Test
    void blockedLinkForcesDetourWhenAlternativeExists() {
        network.addLink(pc1, server);
        network.findLink(pc1, server).orElseThrow().setBlocked(true);
        assertEquals(List.of(pc1, router, server, db), network.shortestPath(pc1, db));
    }

    @Test
    void unblockingLinkRestoresReachability() {
        Link link = network.findLink(firewall, router).orElseThrow();
        link.setBlocked(true);
        assertFalse(network.isReachable(internet, db));
        link.setBlocked(false);
        assertTrue(network.isReachable(internet, db));
    }

    @Test
    void blockedLinkShrinksReachableSet() {
        network.findLink(router, server).orElseThrow().setBlocked(true);
        assertEquals(Set.of(internet, firewall, router, pc1, pc2, pc3),
                network.reachableSet(internet));
    }
}
