package nl.tue.sysadminpanic.attack;

import nl.tue.sysadminpanic.model.Device;
import nl.tue.sysadminpanic.model.DeviceType;
import nl.tue.sysadminpanic.model.GameConstants;
import nl.tue.sysadminpanic.model.Network;

/**
 * Stage 5: the attacker sends the target's data out to the internet.
 *
 * <p>Each tick with a working route to the INTERNET device sends one chunk:
 * it logs a large transfer and drains health. Without a route, the transfer
 * pauses and resumes when the route comes back. After
 * {@link GameConstants#EXFILTRATION_TICKS} chunks the campaign has finished.
 */
public class ExfiltrationStage implements AttackStage {

    private int ticksSent;

    @Override
    public void tick(AttackCampaign campaign, AttackContext context) {
        Network network = context.getNetwork();
        Device source = campaign.getCurrentDevice();
        Device internet = network.getFirstOfType(DeviceType.INTERNET);
        if (!network.isReachable(source, internet)) {
            return;
        }
        context.log(source, LogTemplates.outboundTransfer(campaign.getSourceIp()));
        context.reportExfiltration(source);
        ticksSent++;
        if (ticksSent >= GameConstants.EXFILTRATION_TICKS) {
            campaign.end(CampaignStatus.FINISHED, context);
        }
    }
}
