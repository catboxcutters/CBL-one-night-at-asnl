package nl.tue.sysadminpanic.action;

import java.util.Optional;
import nl.tue.sysadminpanic.model.Device;
import nl.tue.sysadminpanic.model.GameConstants;
import nl.tue.sysadminpanic.model.Link;

/**
 * Blocks the link between the selected device and one of its neighbors.
 * Unlike isolation, both devices keep working, so there is no health drain.
 */
public class FirewallRuleAction extends DeviceAction {

    private final Device neighbor;

    /**
     * @param device   the selected device
     * @param neighbor the directly linked device to cut the link to
     */
    public FirewallRuleAction(Device device, Device neighbor) {
        super(device);
        this.neighbor = neighbor;
    }

    @Override
    public String getName() {
        return "Firewall rule";
    }

    @Override
    public int getPrice() {
        return GameConstants.PRICE_FIREWALL_RULE;
    }

    @Override
    protected Optional<String> checkDevice(ActionContext context) {
        Optional<Link> link = context.getNetwork().findLink(device, neighbor);
        if (link.isEmpty()) {
            return Optional.of(device.getName() + " is not linked to " + neighbor.getName());
        }
        if (link.get().isBlocked()) {
            return Optional.of("That link is already blocked");
        }
        return Optional.empty();
    }

    @Override
    public ActionResult execute(ActionContext context) {
        Link link = context.getNetwork().findLink(device, neighbor).orElseThrow();
        link.setBlocked(true);
        context.deviceChanged(device);
        return ActionResult.success("Firewall rule added: " + link + " blocked");
    }

    /** @return the device at the other end of the link to block */
    public Device getNeighbor() {
        return neighbor;
    }
}
