package nl.tue.sysadminpanic.action;

import java.util.Optional;
import nl.tue.sysadminpanic.model.Device;
import nl.tue.sysadminpanic.model.GameConstants;

/**
 * Installs security updates: the device becomes immune to brute force and
 * attackers need twice as long to hop onto it. It does not remove an attacker
 * that is already on the device.
 */
public class PatchAction extends DeviceAction {

    /**
     * @param device the device to patch
     */
    public PatchAction(Device device) {
        super(device);
    }

    @Override
    public String getName() {
        return "Patch";
    }

    @Override
    public int getPrice() {
        return GameConstants.PRICE_PATCH;
    }

    @Override
    protected Optional<String> checkDevice(ActionContext context) {
        if (device.isPatched()) {
            return Optional.of(device.getName() + " is already patched");
        }
        return Optional.empty();
    }

    @Override
    public ActionResult execute(ActionContext context) {
        device.setPatched(true);
        context.deviceChanged(device);
        return ActionResult.success(device.getName() + " patched");
    }
}
