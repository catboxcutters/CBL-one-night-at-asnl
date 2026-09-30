package nl.tue.sysadminpanic.scenario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import nl.tue.sysadminpanic.model.Device;
import nl.tue.sysadminpanic.model.DeviceType;
import nl.tue.sysadminpanic.model.Network;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link NetworkBuilder}.
 */
class NetworkBuilderTest {

    private NetworkBuilder twoDevices() {
        return new NetworkBuilder()
                .device("A", DeviceType.SERVER, 2, 0.1, 0.2)
                .device("B", DeviceType.DATABASE, 5, 0.9, 0.8)
                .link("A", "B");
    }

    @Test
    void buildCreatesDevicesWithGivenProperties() {
        Device a = twoDevices().build().getDevice("A");
        assertEquals("A", a.getName());
        assertEquals(DeviceType.SERVER, a.getType());
        assertEquals(2, a.getCriticality());
        assertEquals(0.1, a.getX());
        assertEquals(0.2, a.getY());
    }

    @Test
    void buildCreatesLinks() {
        Network network = twoDevices().build();
        assertEquals(List.of(network.getDevice("B")), network.neighbors(network.getDevice("A")));
    }

    @Test
    void linksMayBeDeclaredBeforeTheirDevices() {
        Network network = new NetworkBuilder()
                .link("A", "B")
                .device("A", DeviceType.SERVER, 1, 0, 0)
                .device("B", DeviceType.SERVER, 1, 0, 0)
                .build();
        assertTrue(network.isReachable(network.getDevice("A"), network.getDevice("B")));
    }

    @Test
    void eachBuildGivesAnIndependentNetwork() {
        NetworkBuilder builder = twoDevices();
        Network first = builder.build();
        Network second = builder.build();
        first.getDevice("A").setIsolated(true);
        assertNotSame(first.getDevice("A"), second.getDevice("A"));
        assertTrue(second.isReachable(second.getDevice("A"), second.getDevice("B")));
    }

    @Test
    void linkToUnknownDeviceFailsOnBuild() {
        NetworkBuilder builder = twoDevices().link("A", "C");
        assertThrows(IllegalArgumentException.class, builder::build);
    }
}
