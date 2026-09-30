package nl.tue.sysadminpanic.attack;

/**
 * Stage 3: the attacker takes over its entry device.
 *
 * <p>This is a momentary stage: all its work happens in {@link #enter}, which
 * compromises the entry device and moves straight on to
 * {@link LateralMovementStage} in the same tick. That way the device is
 * compromised on exactly the tick the break-in succeeds.
 */
public class CompromiseStage implements AttackStage {

    @Override
    public void enter(AttackCampaign campaign, AttackContext context) {
        campaign.takeControl(campaign.getEntryDevice(), context);
        campaign.setStage(new LateralMovementStage(), context);
    }

    @Override
    public void tick(AttackCampaign campaign, AttackContext context) {
        // Never called: enter() always switches to the next stage immediately.
    }
}
