package nl.tue.sysadminpanic.action;

import java.util.Optional;
import nl.tue.sysadminpanic.model.Device;
import nl.tue.sysadminpanic.model.GameConstants;

/**
 * Cuts a device off from all its links. Attackers cannot pass through it or
 * leave it, but it drains health while isolated because staff cannot work.
 */
public class IsolateAction extends DeviceAction {

    /**
     * @param device the device to isolate
     */
    public IsolateAction(Device device) {
        super(device);
    }

    @Override
    public String getName() {
        return "Isolate";
    }

    @Override
    public int getPrice() {
        return GameConstants.PRICE_ISOLATE;
    }

    @Override
    protected Optional<String> checkDevice(ActionContext context) {
        if (device.isIsolated()) {
            return Optional.of(device.getName() + " is already isolated");
        }
        return Optional.empty();
    }

    @Override
    public ActionResult execute(ActionContext context) {
        device.setIsolated(true);
        context.deviceChanged(device);
        return ActionResult.success(device.getName() + " isolated from the network");
    }
}
