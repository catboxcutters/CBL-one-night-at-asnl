package nl.tue.sysadminpanic.action;

import nl.tue.sysadminpanic.attack.AttackCampaign;
import nl.tue.sysadminpanic.model.Device;
import nl.tue.sysadminpanic.model.GameConstants;

/**
 * Looks closely at a device: reveals its true state and its log so far. Free.
 */
public class InspectAction extends DeviceAction {

    /**
     * @param device the device to inspect
     */
    public InspectAction(Device device) {
        super(device);
    }

    @Override
    public String getName() {
        return "Inspect";
    }

    @Override
    public int getPrice() {
        return GameConstants.PRICE_INSPECT;
    }

    @Override
    public ActionResult execute(ActionContext context) {
        device.setKnownState(device.getTrueState());
        device.markInspected(context.getCurrentTick());
        for (AttackCampaign campaign : context.getCampaigns()) {
            if (campaign.getControlledDevices().contains(device)) {
                campaign.markDetected();
            }
        }
        context.deviceChanged(device);
        return ActionResult.success(device.getName() + " inspected: "
                + device.getTrueState().name().toLowerCase());
    }
}
