package nl.tue.sysadminpanic.action;

import java.util.Optional;
import nl.tue.sysadminpanic.model.Device;
import nl.tue.sysadminpanic.model.DeviceType;

/**
 * Shared base for actions aimed at one device.
 *
 * <p>It stores the target device and applies the rule that every such action
 * shares: the INTERNET is not ours to manage. Subclasses add their own checks
 * in {@link #checkDevice}.
 */
public abstract class DeviceAction implements PlayerAction {

    /** The device this action is aimed at. */
    protected final Device device;

    /**
     * @param device the device this action is aimed at
     */
    protected DeviceAction(Device device) {
        this.device = device;
    }

    @Override
    public final Optional<String> checkValid(ActionContext context) {
        if (device.getType() == DeviceType.INTERNET) {
            return Optional.of("The internet is outside our network");
        }
        return checkDevice(context);
    }

    /**
     * Action-specific validity check. By default every non-INTERNET device is valid.
     *
     * @param context the game world
     * @return the reason the action cannot be done, or empty if it can
     */
    protected Optional<String> checkDevice(ActionContext context) {
        return Optional.empty();
    }

    /** @return the device this action is aimed at */
    public Device getDevice() {
        return device;
    }
}
