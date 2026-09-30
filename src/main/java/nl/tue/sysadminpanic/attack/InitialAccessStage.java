package nl.tue.sysadminpanic.attack;

/**
 * Stage 2: the attacker tries to break into its entry device.
 *
 * <p>This stage only counts ticks. Everything that depends on the attack method
 * is delegated to the campaign's {@link InitialAccessStrategy}. On the final tick
 * the break-in either works (on to {@link CompromiseStage}) or fails (the
 * campaign ends as contained).
 */
public class InitialAccessStage implements AttackStage {

    private int attempts;

    @Override
    public void enter(AttackCampaign campaign, AttackContext context) {
        campaign.getAccessStrategy().begin(campaign, context);
    }

    @Override
    public void tick(AttackCampaign campaign, AttackContext context) {
        InitialAccessStrategy strategy = campaign.getAccessStrategy();
        attempts++;
        if (attempts < strategy.durationTicks()) {
            strategy.logAttempt(campaign, context);
        } else if (strategy.canBreakInto(campaign.getEntryDevice())) {
            strategy.logSuccess(campaign, context);
            campaign.setStage(new CompromiseStage(), context);
        } else {
            strategy.logFailure(campaign, context);
            campaign.end(CampaignStatus.CONTAINED, context);
        }
    }
}
