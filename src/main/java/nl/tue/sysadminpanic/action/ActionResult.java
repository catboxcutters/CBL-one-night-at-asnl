package nl.tue.sysadminpanic.action;

/**
 * The outcome of a player action, shown as feedback in the UI.
 *
 * @param success true if the action was carried out
 * @param message what happened, or why nothing happened
 */
public record ActionResult(boolean success, String message) {

    /**
     * @param message what happened
     * @return a successful result
     */
    public static ActionResult success(String message) {
        return new ActionResult(true, message);
    }

    /**
     * @param message why the action was not carried out
     * @return a failed result
     */
    public static ActionResult failure(String message) {
        return new ActionResult(false, message);
    }
}
