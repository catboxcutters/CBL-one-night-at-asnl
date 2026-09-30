package nl.tue.sysadminpanic.action;

import java.util.Optional;
import nl.tue.sysadminpanic.model.Device;
import nl.tue.sysadminpanic.model.GameConstants;

/**
 * Undoes {@link IsolateAction}: plugs an isolated device back in. Free.
 */
public class ReconnectAction extends DeviceAction {

    /**
     * @param device the isolated device to reconnect
     */
    public ReconnectAction(Device device) {
        super(device);
    }

    @Override
    public String getName() {
        return "Reconnect";
    }

    @Override
    public int getPrice() {
        return GameConstants.PRICE_RECONNECT;
    }

    @Override
    protected Optional<String> checkDevice(ActionContext context) {
        if (!device.isIsolated()) {
            return Optional.of(device.getName() + " is not isolated");
        }
        return Optional.empty();
    }

    @Override
    public ActionResult execute(ActionContext context) {
        device.setIsolated(false);
        context.deviceChanged(device);
        return ActionResult.success(device.getName() + " reconnected to the network");
    }
}
