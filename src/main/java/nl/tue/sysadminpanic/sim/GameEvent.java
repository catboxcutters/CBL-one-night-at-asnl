package nl.tue.sysadminpanic.sim;

import nl.tue.sysadminpanic.action.ActionResult;
import nl.tue.sysadminpanic.attack.CampaignStatus;
import nl.tue.sysadminpanic.model.Alert;
import nl.tue.sysadminpanic.model.Device;

/**
 * Something that happened in the game, published to every {@link GameEventListener}.
 *
 * <p>The interface is {@code sealed}: the records nested below are its only
 * implementations. A listener can therefore handle events with a {@code switch}
 * over the record types, and the compiler knows the full list of cases.
 */
public sealed interface GameEvent {

    /**
     * The simulation advanced by one tick.
     *
     * @param tick the new tick number
     */
    record TickAdvanced(int tick) implements GameEvent {
    }

    /**
     * A new alert was raised.
     *
     * @param alert the alert
     */
    record AlertRaised(Alert alert) implements GameEvent {
    }

    /**
     * A device's true or known state changed.
     *
     * @param device the device that changed; read its current state from it
     */
    record DeviceStateChanged(Device device) implements GameEvent {
    }

    /**
     * Network health changed.
     *
     * @param health the new health, from 0 to 100
     */
    record HealthChanged(double health) implements GameEvent {
    }

    /**
     * The player's balance changed, through income or spending.
     *
     * @param balance the new balance in dollars
     */
    record MoneyChanged(int balance) implements GameEvent {
    }

    /**
     * The player tried an action; published whether it worked or not.
     *
     * @param result what happened, for the event feed
     */
    record ActionPerformed(ActionResult result) implements GameEvent {
    }

    /**
     * An attack campaign ended.
     *
     * @param sourceIp the attacker's IP
     * @param status   how it ended
     */
    record CampaignEnded(String sourceIp, CampaignStatus status) implements GameEvent {
    }

    /**
     * A new wave of attacks begins.
     *
     * @param number     the wave number, starting at 1
     * @param totalWaves how many waves the scenario has
     * @param name       the wave's title
     */
    record WaveStarted(int number, int totalWaves, String name) implements GameEvent {
    }

    /**
     * Every campaign of a wave has ended.
     *
     * @param number the wave number, starting at 1
     */
    record WaveEnded(int number) implements GameEvent {
    }

    /**
     * The game is over.
     *
     * @param won true if the player won, false if network health reached 0
     */
    record GameOver(boolean won) implements GameEvent {
    }
}
