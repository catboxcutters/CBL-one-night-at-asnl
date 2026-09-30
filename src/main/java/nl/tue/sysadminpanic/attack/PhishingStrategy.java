package nl.tue.sysadminpanic.attack;

import nl.tue.sysadminpanic.model.Device;
import nl.tue.sysadminpanic.model.GameConstants;

/**
 * A user opens a malicious attachment: quick and silent.
 *
 * <p>It raises no alert. The only trace is one suspicious process line in the
 * workstation's log, which the player only sees by inspecting the device.
 * Patching does not help, because the user runs the malware themselves.
 */
public class PhishingStrategy implements InitialAccessStrategy {

    @Override
    public int durationTicks() {
        return GameConstants.PHISHING_TICKS;
    }

    @Override
    public void begin(AttackCampaign campaign, AttackContext context) {
        // Silent on purpose: no alert.
    }

    @Override
    public void logAttempt(AttackCampaign campaign, AttackContext context) {
        // The email is just waiting to be opened; nothing shows up in the logs yet.
    }

    @Override
    public boolean canBreakInto(Device device) {
        return true;
    }

    @Override
    public void logSuccess(AttackCampaign campaign, AttackContext context) {
        context.log(campaign.getEntryDevice(), LogTemplates.suspiciousProcess());
    }

    @Override
    public void logFailure(AttackCampaign campaign, AttackContext context) {
        // Unreachable: canBreakInto always returns true.
    }
}
