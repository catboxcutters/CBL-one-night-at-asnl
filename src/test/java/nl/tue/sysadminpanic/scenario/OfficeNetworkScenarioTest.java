package nl.tue.sysadminpanic.scenario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import nl.tue.sysadminpanic.model.Device;
import nl.tue.sysadminpanic.model.DeviceType;
import nl.tue.sysadminpanic.model.Network;
import org.junit.jupiter.api.Test;

/**
 * Tests that the Office Network scenario matches the spec.
 */
class OfficeNetworkScenarioTest {

    private final OfficeNetworkScenario scenario = new OfficeNetworkScenario();
    private final Network network = scenario.createNetwork();

    private Set<String> neighborNames(String device) {
        return network.neighbors(network.getDevice(device)).stream()
                .map(Device::getName).collect(Collectors.toSet());
    }

    @Test
    void networkHasTheEightDevicesFromTheSpec() {
        List<String> names = network.getDevices().stream().map(Device::getName).toList();
        assertEquals(List.of("INTERNET", "FIREWALL", "ROUTER", "PC-01", "PC-02", "PC-03",
                "SERVER-01", "DB-01"), names);
    }

    @Test
    void networkIsWiredAsInTheSpec() {
        assertEquals(Set.of("FIREWALL"), neighborNames("INTERNET"));
        assertEquals(Set.of("INTERNET", "ROUTER"), neighborNames("FIREWALL"));
        assertEquals(Set.of("FIREWALL", "PC-01", "PC-02", "PC-03", "SERVER-01"),
                neighborNames("ROUTER"));
        assertEquals(Set.of("ROUTER", "DB-01"), neighborNames("SERVER-01"));
        assertEquals(7, network.getLinks().size());
    }

    @Test
    void criticalitiesMatchTheSpec() {
        assertEquals(1, network.getDevice("PC-01").getCriticality());
        assertEquals(3, network.getDevice("ROUTER").getCriticality());
        assertEquals(3, network.getDevice("FIREWALL").getCriticality());
        assertEquals(4, network.getDevice("SERVER-01").getCriticality());
        assertEquals(5, network.getDevice("DB-01").getCriticality());
        assertEquals(DeviceType.INTERNET, network.getDevice("INTERNET").getType());
    }

    @Test
    void everyGameGetsAFreshNetwork() {
        assertNotSame(network.getDevice("DB-01"), scenario.createNetwork().getDevice("DB-01"));
    }

    @Test
    void thereAreFiveWavesWithTheSpecNames() {
        List<String> names = scenario.getWaves().stream().map(WaveDefinition::name).toList();
        assertEquals(List.of("Knock knock", "Special delivery", "Crying wolf", "Two fronts",
                "The big one"), names);
    }

    @Test
    void everyEventRefersToExistingDevices() {
        for (WaveDefinition wave : scenario.getWaves()) {
            for (ScriptedEvent event : wave.events()) {
                switch (event) {
                    case ScriptedEvent.StartCampaign s -> {
                        network.getDevice(s.entryDeviceId());
                        network.getDevice(s.targetDeviceId());
                    }
                    case ScriptedEvent.ScriptedAlert a -> network.getDevice(a.deviceId());
                }
            }
        }
    }

    @Test
    void attackersUseDocumentationIpRanges() {
        for (WaveDefinition wave : scenario.getWaves()) {
            for (ScriptedEvent event : wave.events()) {
                if (event instanceof ScriptedEvent.StartCampaign start) {
                    String ip = start.sourceIp();
                    assertTrue(ip.startsWith("203.0.113.") || ip.startsWith("198.51.100."), ip);
                }
            }
        }
    }

    @Test
    void everyAttackTargetsTheDatabase() {
        for (WaveDefinition wave : scenario.getWaves()) {
            for (ScriptedEvent event : wave.events()) {
                if (event instanceof ScriptedEvent.StartCampaign start) {
                    assertEquals("DB-01", start.targetDeviceId());
                }
            }
        }
    }
}
