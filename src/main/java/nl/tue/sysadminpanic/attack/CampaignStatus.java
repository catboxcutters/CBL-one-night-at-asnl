package nl.tue.sysadminpanic.attack;

/**
 * Whether a campaign is still running, and if not, how it ended.
 */
public enum CampaignStatus {
    /** The attack is still in progress. */
    ACTIVE,
    /** The player blocked the attacker's IP before it got in. */
    STOPPED,
    /** The attacker got stuck: no route to its target, or its break-in failed. */
    CONTAINED,
    /** The attacker finished stealing data: the damage is done. */
    FINISHED;

    /** @return true if the defence ended the campaign before it finished (STOPPED or CONTAINED) */
    public boolean isDefeated() {
        return this == STOPPED || this == CONTAINED;
    }
}
