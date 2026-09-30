package nl.tue.sysadminpanic.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

/**
 * The office network as an undirected graph: devices are nodes, links are edges.
 *
 * <p>The graph is stored as an adjacency list: every device maps to the list of
 * links that touch it. Each {@link Link} object is shared by the lists of both of
 * its ends, so blocking it once blocks it in both directions.
 *
 * <p>Reachability and shortest paths use breadth-first search (BFS). The search
 * never enters an isolated device and never crosses a blocked link, so isolating
 * devices and adding firewall rules directly change where an attacker can go.
 *
 * <p>Insertion-ordered maps are used everywhere so that BFS visits neighbors in
 * the order links were added. That makes every result, including which of two
 * equally short paths is chosen, fully deterministic.
 */
public class Network {

    private final Map<String, Device> devicesById = new LinkedHashMap<>();
    private final Map<Device, List<Link>> adjacency = new LinkedHashMap<>();
    private final List<Link> links = new ArrayList<>();

    /**
     * Adds a device with no links.
     *
     * @param device the device to add
     * @throws IllegalArgumentException if a device with the same id already exists
     */
    public void addDevice(Device device) {
        if (devicesById.containsKey(device.getId())) {
            throw new IllegalArgumentException("Duplicate device id: " + device.getId());
        }
        devicesById.put(device.getId(), device);
        adjacency.put(device, new ArrayList<>());
    }

    /**
     * Connects two devices that are already in the network.
     *
     * @param first  one end of the new link
     * @param second the other end of the new link
     * @return the new link
     * @throws IllegalArgumentException if a device is unknown, both ends are the same
     *                                  device, or the two devices are already linked
     */
    public Link addLink(Device first, Device second) {
        requireKnown(first);
        requireKnown(second);
        if (findLink(first, second).isPresent()) {
            throw new IllegalArgumentException("Already linked: " + first + " - " + second);
        }
        Link link = new Link(first, second);
        adjacency.get(first).add(link);
        adjacency.get(second).add(link);
        links.add(link);
        return link;
    }

    /**
     * Looks up a device by its id.
     *
     * @param id the device id
     * @return the device with that id
     * @throws IllegalArgumentException if no device has that id
     */
    public Device getDevice(String id) {
        Device device = devicesById.get(id);
        if (device == null) {
            throw new IllegalArgumentException("Unknown device id: " + id);
        }
        return device;
    }

    /**
     * Returns the first device of the given type, in the order devices were added.
     * Attacks use this to find the INTERNET (where stolen data goes) and the
     * FIREWALL (where port scans are logged).
     *
     * @param type the device type to look for
     * @return the first device of that type
     * @throws IllegalStateException if the network has no device of that type
     */
    public Device getFirstOfType(DeviceType type) {
        for (Device device : devicesById.values()) {
            if (device.getType() == type) {
                return device;
            }
        }
        throw new IllegalStateException("Network has no device of type " + type);
    }

    /** @return all devices, in the order they were added (read-only) */
    public Collection<Device> getDevices() {
        return Collections.unmodifiableCollection(devicesById.values());
    }

    /** @return all links, in the order they were added (read-only) */
    public List<Link> getLinks() {
        return Collections.unmodifiableList(links);
    }

    /**
     * Returns every link that touches the given device, blocked or not.
     *
     * @param device a device in the network
     * @return the device's links (read-only)
     */
    public List<Link> linksOf(Device device) {
        requireKnown(device);
        return Collections.unmodifiableList(adjacency.get(device));
    }

    /**
     * Finds the link between two devices, if there is one.
     *
     * @param first  one device
     * @param second the other device
     * @return the link between them, or an empty Optional if they are not directly linked
     */
    public Optional<Link> findLink(Device first, Device second) {
        requireKnown(first);
        for (Link link : adjacency.get(first)) {
            if (link.connects(second)) {
                return Optional.of(link);
            }
        }
        return Optional.empty();
    }

    /**
     * Returns every device directly linked to the given device.
     *
     * <p>This is the physical wiring: blocked links and isolated devices are included.
     * Use {@link #isReachable} or {@link #shortestPath} to ask where traffic can go.
     *
     * @param device a device in the network
     * @return the directly linked devices, in link order
     */
    public List<Device> neighbors(Device device) {
        List<Device> result = new ArrayList<>();
        for (Link link : linksOf(device)) {
            result.add(link.other(device));
        }
        return result;
    }

    /**
     * Checks whether traffic can travel from one device to another.
     *
     * @param from the start device
     * @param to   the destination device
     * @return true if a path exists that avoids isolated devices and blocked links
     */
    public boolean isReachable(Device from, Device to) {
        return breadthFirstSearch(from).containsKey(to);
    }

    /**
     * Returns all devices that traffic from the given device can reach, including itself.
     *
     * @param from the start device
     * @return the reachable devices in BFS order, or an empty set if {@code from} is isolated
     */
    public Set<Device> reachableSet(Device from) {
        return Collections.unmodifiableSet(breadthFirstSearch(from).keySet());
    }

    /**
     * Finds a shortest path (fewest hops) between two devices.
     *
     * @param from the start device
     * @param to   the destination device
     * @return the path including both ends, or an empty list if {@code to} is unreachable
     */
    public List<Device> shortestPath(Device from, Device to) {
        Map<Device, Device> parents = breadthFirstSearch(from);
        if (!parents.containsKey(to)) {
            return List.of();
        }
        // Walk back from the destination via the parent pointers, adding each
        // device at the front so the finished list reads from start to end.
        LinkedList<Device> path = new LinkedList<>();
        for (Device step = to; step != null; step = parents.get(step)) {
            path.addFirst(step);
        }
        return List.copyOf(path);
    }

    /**
     * Runs BFS from a start device and records how each device was first reached.
     *
     * <p>The result maps every reachable device to its parent: the device it was
     * discovered from. The start device maps to {@code null}. Because BFS explores
     * devices in order of distance, following parents back gives a shortest path.
     *
     * @param start the device to search from
     * @return reachable device to parent, in visiting order; empty if start is isolated
     */
    private Map<Device, Device> breadthFirstSearch(Device start) {
        requireKnown(start);
        Map<Device, Device> parents = new LinkedHashMap<>();
        if (start.isIsolated()) {
            return parents;
        }
        Queue<Device> queue = new ArrayDeque<>();
        parents.put(start, null);
        queue.add(start);
        while (!queue.isEmpty()) {
            Device current = queue.remove();
            for (Link link : adjacency.get(current)) {
                Device next = link.other(current);
                if (canPass(link, next) && !parents.containsKey(next)) {
                    parents.put(next, current);
                    queue.add(next);
                }
            }
        }
        return parents;
    }

    /**
     * Decides whether BFS may step over a link onto the next device.
     */
    private boolean canPass(Link link, Device next) {
        return !link.isBlocked() && !next.isIsolated();
    }

    private void requireKnown(Device device) {
        if (!adjacency.containsKey(device)) {
            throw new IllegalArgumentException("Device is not in this network: " + device);
        }
    }
}
