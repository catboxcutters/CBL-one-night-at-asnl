package nl.tue.sysadminpanic.attack;

import java.util.List;
import nl.tue.sysadminpanic.model.Device;
import nl.tue.sysadminpanic.model.GameConstants;

/**
 * Stage 4: the attacker hops through the network towards its target.
 *
 * <p>Every tick it asks the network for a shortest path from its current device
 * to the target. Asking again every tick means that isolating a device or adding
 * a firewall rule is noticed immediately: the attacker takes a detour if there
 * is one, and the campaign is contained if there is none.
 */
public class LateralMovementStage implements AttackStage {

    private int ticksSinceHop;

    @Override
    public void enter(AttackCampaign campaign, AttackContext context) {
        // The entry device may itself be the target: then there is nowhere to move.
        if (campaign.getCurrentDevice() == campaign.getTarget()) {
            campaign.setStage(new ExfiltrationStage(), context);
        }
    }

    @Override
    public void tick(AttackCampaign campaign, AttackContext context) {
        Device current = campaign.getCurrentDevice();
        List<Device> path = context.getNetwork().shortestPath(current, campaign.getTarget());
        if (path.isEmpty()) {
            campaign.end(CampaignStatus.CONTAINED, context);
            return;
        }
        // path.get(0) is the current device, so the next hop is at index 1.
        Device next = path.get(1);
        ticksSinceHop++;
        if (ticksSinceHop >= hopTicks(next)) {
            hop(campaign, current, next, context);
        }
    }

    private void hop(AttackCampaign campaign, Device from, Device to, AttackContext context) {
        context.log(to, LogTemplates.inboundConnection(from.getName()));
        campaign.takeControl(to, context);
        ticksSinceHop = 0;
        if (to == campaign.getTarget()) {
            campaign.setStage(new ExfiltrationStage(), context);
        }
    }

    private int hopTicks(Device next) {
        return next.isPatched() ? GameConstants.PATCHED_HOP_TICKS : GameConstants.HOP_TICKS;
    }
}
