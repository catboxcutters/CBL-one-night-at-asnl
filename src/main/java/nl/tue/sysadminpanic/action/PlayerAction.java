package nl.tue.sysadminpanic.action;

import java.util.Optional;

/**
 * Something the player can do: the <em>command</em> interface of the Command pattern.
 *
 * <p>Each action is an object that bundles its target (chosen when it is created),
 * its price, a validity check and the work itself. The UI only creates actions and
 * hands them to the simulation, which checks them, charges the price and runs them
 * the same way for every kind of action.
 */
public interface PlayerAction {

    /** @return the short name for buttons, for example {@code "Isolate"} */
    String getName();

    /** @return the price in dollars; 0 for free actions */
    int getPrice();

    /**
     * Checks whether the action makes sense right now. Money is checked separately
     * by the simulation, because the check is the same for every action.
     *
     * @param context the game world
     * @return the reason the action cannot be done, or empty if it can
     */
    Optional<String> checkValid(ActionContext context);

    /**
     * Carries out the action. Only called after {@link #checkValid} returned empty
     * and the price was paid.
     *
     * @param context the game world
     * @return what happened, for UI feedback
     */
    ActionResult execute(ActionContext context);
}
