package nl.tue.sysadminpanic.attack;

import nl.tue.sysadminpanic.model.AlertType;
import nl.tue.sysadminpanic.model.Device;
import nl.tue.sysadminpanic.model.GameConstants;
import nl.tue.sysadminpanic.model.Severity;

/**
 * Password guessing: loud and slow, and useless against a patched device.
 *
 * <p>It raises a HIGH alert when it starts and logs a failed login every tick.
 * On a patched device it gives up at the moment it would have succeeded, so the
 * campaign ends as contained instead of trying forever.
 */
public class BruteForceStrategy implements InitialAccessStrategy {

    @Override
    public int durationTicks() {
        return GameConstants.BRUTE_FORCE_TICKS;
    }

    @Override
    public void begin(AttackCampaign campaign, AttackContext context) {
        Device device = campaign.getEntryDevice();
        String ip = campaign.getSourceIp();
        context.raiseAlert(AlertType.BRUTE_FORCE, ip, device, Severity.HIGH,
                LogTemplates.bruteForceAlert(device.getName(), ip));
    }

    @Override
    public void logAttempt(AttackCampaign campaign, AttackContext context) {
        context.log(campaign.getEntryDevice(), LogTemplates.failedLogin(campaign.getSourceIp()));
    }

    @Override
    public boolean canBreakInto(Device device) {
        return !device.isPatched();
    }

    @Override
    public void logSuccess(AttackCampaign campaign, AttackContext context) {
        context.log(campaign.getEntryDevice(),
                LogTemplates.successfulLogin(campaign.getSourceIp()));
    }

    @Override
    public void logFailure(AttackCampaign campaign, AttackContext context) {
        Device device = campaign.getEntryDevice();
        context.log(device, LogTemplates.failedLogin(campaign.getSourceIp()));
        context.log(device, LogTemplates.loginAttemptsStopped(campaign.getSourceIp()));
    }
}
