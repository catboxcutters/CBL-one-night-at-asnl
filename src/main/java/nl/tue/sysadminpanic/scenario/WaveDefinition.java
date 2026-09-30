package nl.tue.sysadminpanic.scenario;

import java.util.List;

/**
 * One wave of the attack script: a name and its events in time order.
 *
 * @param name   the wave's title, shown in the event feed, for example {@code "Knock knock"}
 * @param events the wave's events, sorted by tick offset
 */
public record WaveDefinition(String name, List<ScriptedEvent> events) {

    /**
     * Copies the event list and checks it is sorted by tick offset, because the
     * wave runner fires events strictly in list order.
     *
     * @throws IllegalArgumentException if the events are out of order
     */
    public WaveDefinition {
        events = List.copyOf(events);
        for (int i = 1; i < events.size(); i++) {
            if (events.get(i).tickOffset() < events.get(i - 1).tickOffset()) {
                throw new IllegalArgumentException("Events of wave '" + name
                        + "' must be sorted by tick offset");
            }
        }
    }
}
