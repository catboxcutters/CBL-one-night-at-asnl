package nl.tue.sysadminpanic.action;

import java.util.Optional;
import nl.tue.sysadminpanic.attack.AttackCampaign;
import nl.tue.sysadminpanic.attack.CampaignStatus;
import nl.tue.sysadminpanic.model.GameConstants;

/**
 * Blocks an IP address at the network edge.
 *
 * <p>This stops every campaign from that IP that is still outside the network
 * (in recon or initial access). An attacker that is already inside no longer
 * needs its original IP, so it is not affected.
 */
public class BlockIpAction implements PlayerAction {

    private final String ip;

    /**
     * @param ip the IP address to block
     */
    public BlockIpAction(String ip) {
        this.ip = ip;
    }

    @Override
    public String getName() {
        return "Block IP";
    }

    @Override
    public int getPrice() {
        return GameConstants.PRICE_BLOCK_IP;
    }

    @Override
    public Optional<String> checkValid(ActionContext context) {
        if (context.isIpBlocked(ip)) {
            return Optional.of(ip + " is already blocked");
        }
        return Optional.empty();
    }

    @Override
    public ActionResult execute(ActionContext context) {
        context.blockIp(ip);
        int stopped = 0;
        for (AttackCampaign campaign : context.getCampaigns()) {
            if (campaign.isActive() && campaign.isOutside() && campaign.getSourceIp().equals(ip)) {
                campaign.end(CampaignStatus.STOPPED, context);
                stopped++;
            }
        }
        // Say only what the SOC can observe: whether traffic from this IP was cut off.
        String effect = stopped > 0 ? "incoming attack cut off" : "no active connections from it";
        return ActionResult.success("Blocked " + ip + ": " + effect);
    }

    /** @return the IP address this action blocks */
    public String getIp() {
        return ip;
    }
}
