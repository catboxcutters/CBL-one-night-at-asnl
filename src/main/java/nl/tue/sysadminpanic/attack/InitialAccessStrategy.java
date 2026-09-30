package nl.tue.sysadminpanic.attack;

import nl.tue.sysadminpanic.model.Device;

/**
 * How an attacker breaks into its entry device: the Strategy pattern.
 *
 * <p>{@link InitialAccessStage} owns the timing (counting attempts) and asks the
 * strategy for everything that differs between attack methods: how long it
 * takes, what it logs, whether it raises an alert, and whether patching helps.
 * A new attack method is a new class, with no change to the stage.
 */
public interface InitialAccessStrategy {

    /** @return how many ticks the break-in takes before it succeeds or fails */
    int durationTicks();

    /**
     * Runs once when the break-in starts, for example to raise an alert.
     *
     * @param campaign the attacking campaign
     * @param context  the game world
     */
    void begin(AttackCampaign campaign, AttackContext context);

    /**
     * Runs on every tick before the final one, for example to log a failed attempt.
     *
     * @param campaign the attacking campaign
     * @param context  the game world
     */
    void logAttempt(AttackCampaign campaign, AttackContext context);

    /**
     * Decides, on the final tick, whether the break-in works.
     *
     * @param device the entry device
     * @return true if this method can get into the device
     */
    boolean canBreakInto(Device device);

    /**
     * Runs on the final tick when the break-in works.
     *
     * @param campaign the attacking campaign
     * @param context  the game world
     */
    void logSuccess(AttackCampaign campaign, AttackContext context);

    /**
     * Runs on the final tick when the break-in fails.
     *
     * @param campaign the attacking campaign
     * @param context  the game world
     */
    void logFailure(AttackCampaign campaign, AttackContext context);
}
