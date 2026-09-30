package nl.tue.sysadminpanic.attack;

import nl.tue.sysadminpanic.model.AlertType;
import nl.tue.sysadminpanic.model.Device;
import nl.tue.sysadminpanic.model.DeviceType;
import nl.tue.sysadminpanic.model.GameConstants;
import nl.tue.sysadminpanic.model.Severity;

/**
 * Stage 1: the attacker port-scans the network from outside.
 *
 * <p>Raises a LOW alert on the firewall when it starts and logs a few dropped
 * connection attempts there every tick. After {@link GameConstants#RECON_TICKS}
 * ticks it moves on to {@link InitialAccessStage}.
 */
public class ReconStage implements AttackStage {

    /** Ports logged per tick; purely cosmetic, so it is not a balancing constant. */
    private static final int PROBES_PER_TICK = 3;

    private int ticksElapsed;
    private int probesLogged;

    @Override
    public void enter(AttackCampaign campaign, AttackContext context) {
        String ip = campaign.getSourceIp();
        context.raiseAlert(AlertType.PORT_SCAN, ip, firewall(context), Severity.LOW,
                LogTemplates.portScanAlert(ip));
    }

    @Override
    public void tick(AttackCampaign campaign, AttackContext context) {
        Device firewall = firewall(context);
        for (int i = 0; i < PROBES_PER_TICK; i++) {
            context.log(firewall, LogTemplates.portProbe(campaign.getSourceIp(), probesLogged));
            probesLogged++;
        }
        ticksElapsed++;
        if (ticksElapsed >= GameConstants.RECON_TICKS) {
            campaign.setStage(new InitialAccessStage(), context);
        }
    }

    private Device firewall(AttackContext context) {
        return context.getNetwork().getFirstOfType(DeviceType.FIREWALL);
    }
}
