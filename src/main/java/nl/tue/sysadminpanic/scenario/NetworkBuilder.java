package nl.tue.sysadminpanic.scenario;

import java.util.ArrayList;
import java.util.List;
import nl.tue.sysadminpanic.model.Device;
import nl.tue.sysadminpanic.model.DeviceType;
import nl.tue.sysadminpanic.model.Network;

/**
 * Builds a {@link Network} step by step: the Builder pattern.
 *
 * <p>Calls can be chained so that a scenario's network reads like a description:
 * <pre>{@code
 * Network network = new NetworkBuilder()
 *         .device("INTERNET", DeviceType.INTERNET, 1, 0.07, 0.50)
 *         .device("FIREWALL", DeviceType.FIREWALL, 3, 0.25, 0.50)
 *         .link("INTERNET", "FIREWALL")
 *         .build();
 * }</pre>
 * Links refer to devices by name, so the order of the calls does not matter.
 * Every call to {@link #build()} creates a fresh, independent network, which is
 * what "Play again" needs.
 */
public class NetworkBuilder {

    /** The recorded device definition, turned into a real Device by build(). */
    private record DeviceSpec(String name, DeviceType type, int criticality, double x, double y) {
    }

    /** The recorded link definition between two device names. */
    private record LinkSpec(String first, String second) {
    }

    private final List<DeviceSpec> devices = new ArrayList<>();
    private final List<LinkSpec> links = new ArrayList<>();

    /**
     * Adds a device. Its name is also its id.
     *
     * @param name        unique name, for example {@code "PC-01"}
     * @param type        the kind of device
     * @param criticality 1 (low) to 5 (high)
     * @param x           horizontal map position, 0.0 to 1.0
     * @param y           vertical map position, 0.0 to 1.0
     * @return this builder, for chaining
     */
    public NetworkBuilder device(String name, DeviceType type, int criticality,
            double x, double y) {
        devices.add(new DeviceSpec(name, type, criticality, x, y));
        return this;
    }

    /**
     * Connects two devices by name.
     *
     * @param first  name of one device
     * @param second name of the other device
     * @return this builder, for chaining
     */
    public NetworkBuilder link(String first, String second) {
        links.add(new LinkSpec(first, second));
        return this;
    }

    /**
     * Creates a new network from everything added so far.
     *
     * @return the network
     * @throws IllegalArgumentException if a name is duplicated, a link names an
     *                                  unknown device, or a definition is invalid
     */
    public Network build() {
        Network network = new Network();
        for (DeviceSpec spec : devices) {
            network.addDevice(new Device(spec.name(), spec.name(), spec.type(),
                    spec.criticality(), spec.x(), spec.y()));
        }
        for (LinkSpec spec : links) {
            network.addLink(network.getDevice(spec.first()), network.getDevice(spec.second()));
        }
        return network;
    }
}
