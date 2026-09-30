package nl.tue.sysadminpanic.scenario;

import java.util.List;
import nl.tue.sysadminpanic.attack.InitialAccessStrategy;
import nl.tue.sysadminpanic.model.AlertType;
import nl.tue.sysadminpanic.model.Severity;

/**
 * One thing that happens at a fixed moment during a wave.
 *
 * <p>Events are plain data: they describe <em>what</em> should happen, and the
 * simulation's wave runner carries them out. The interface is {@code sealed},
 * so the runner can handle every kind of event with an exhaustive {@code switch}.
 */
public sealed interface ScriptedEvent {

    /** @return the number of ticks after the wave start at which this event happens */
    int tickOffset();

    /**
     * An attacker starts a campaign.
     *
     * @param tickOffset     ticks after the wave start
     * @param sourceIp       the attacker's IP
     * @param entryDeviceId  the device the attacker breaks into first
     * @param targetDeviceId the device the attacker wants to steal data from
     * @param strategy       how the attacker breaks in; must hold no state, because it
     *                       is shared by every game played with this scenario object
     * @param withRecon      true to start with a port scan
     */
    record StartCampaign(int tickOffset, String sourceIp, String entryDeviceId,
            String targetDeviceId, InitialAccessStrategy strategy, boolean withRecon)
            implements ScriptedEvent {
    }

    /**
     * An alert that is not produced by an attack stage: a hint about an attack,
     * or a harmless false positive. Its log lines are what the player finds when
     * inspecting the device.
     *
     * @param tickOffset    ticks after the wave start
     * @param type          what kind of activity the alert reports
     * @param sourceIp      where the activity came from
     * @param deviceId      the device the alert is about
     * @param severity      how serious the alert looks
     * @param summary       one-line description
     * @param falsePositive true if the activity is harmless
     * @param logLines      lines written to the device's log when the alert fires
     */
    record ScriptedAlert(int tickOffset, AlertType type, String sourceIp, String deviceId,
            Severity severity, String summary, boolean falsePositive, List<String> logLines)
            implements ScriptedEvent {

        /** Copies the list so the record stays immutable. */
        public ScriptedAlert {
            logLines = List.copyOf(logLines);
        }
    }
}
