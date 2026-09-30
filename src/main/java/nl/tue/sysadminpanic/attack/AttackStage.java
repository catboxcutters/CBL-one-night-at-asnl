package nl.tue.sysadminpanic.attack;

/**
 * One stage of an attack: the <em>state</em> interface of the State pattern.
 *
 * <p>Each implementation decides what the attacker does on every tick of that
 * stage and when to move on, by calling {@code campaign.setStage(...)} or
 * {@code campaign.end(...)}. Adding a new kind of stage never requires
 * changing {@link AttackCampaign}.
 */
public interface AttackStage {

    /**
     * Runs once, immediately when the campaign switches to this stage.
     * By default it does nothing.
     *
     * @param campaign the campaign entering this stage
     * @param context  the game world
     */
    default void enter(AttackCampaign campaign, AttackContext context) {
    }

    /**
     * Runs once per tick while the campaign is in this stage.
     *
     * @param campaign the campaign in this stage
     * @param context  the game world
     */
    void tick(AttackCampaign campaign, AttackContext context);
}
