package nl.tue.sysadminpanic.sim;

/**
 * Receives game events: the <em>observer</em> interface of the Observer pattern.
 *
 * <p>The UI implements this to repaint when the model changes, so the model
 * never has to know that a UI exists.
 */
@FunctionalInterface
public interface GameEventListener {

    /**
     * Called synchronously, on the thread that caused the event, every time
     * something happens in the game.
     *
     * @param event what happened
     */
    void onEvent(GameEvent event);
}
