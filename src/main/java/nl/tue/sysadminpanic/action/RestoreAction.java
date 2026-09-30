package nl.tue.sysadminpanic.action;

import nl.tue.sysadminpanic.attack.AttackCampaign;
import nl.tue.sysadminpanic.model.Device;
import nl.tue.sysadminpanic.model.DeviceState;
import nl.tue.sysadminpanic.model.GameConstants;

/**
 * Wipes and reinstalls a device from a clean backup.
 *
 * <p>The device is healthy again at once, and every attacker on it loses that
 * foothold. A campaign that loses its last device is stopped.
 */
public class RestoreAction extends DeviceAction {

    /**
     * @param device the device to restore
     */
    public RestoreAction(Device device) {
        super(device);
    }

    @Override
    public String getName() {
        return "Restore";
    }

    @Override
    public int getPrice() {
        return GameConstants.PRICE_RESTORE;
    }

    @Override
    public ActionResult execute(ActionContext context) {
        device.setTrueState(DeviceState.HEALTHY);
        device.setKnownState(DeviceState.HEALTHY);
        for (AttackCampaign campaign : context.getCampaigns()) {
            campaign.loseControl(device, context);
        }
        context.deviceChanged(device);
        return ActionResult.success(device.getName() + " restored from backup");
    }
}
