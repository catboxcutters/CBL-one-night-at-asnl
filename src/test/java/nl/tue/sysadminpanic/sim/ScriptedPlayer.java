package nl.tue.sysadminpanic.sim;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import nl.tue.sysadminpanic.action.ActionResult;
import nl.tue.sysadminpanic.action.PlayerAction;
import nl.tue.sysadminpanic.model.Network;

/**
 * Test helper: a pretend player who performs a fixed list of actions at fixed ticks.
 *
 * <p>An action planned "at tick T" is performed after tick T has been processed,
 * just like a real player reacting to what tick T showed them.
 */
class ScriptedPlayer {

    private final Map<Integer, List<Function<Network, PlayerAction>>> plan = new TreeMap<>();
    private final List<ActionResult> results = new ArrayList<>();

    /**
     * Plans an action. The function receives the game's network so it can look up
     * devices by name, for example {@code net -> new IsolateAction(net.getDevice("PC-02"))}.
     */
    ScriptedPlayer at(int tick, Function<Network, PlayerAction> action) {
        plan.computeIfAbsent(tick, t -> new ArrayList<>()).add(action);
        return this;
    }

    /** Plays the game until it is over, or until maxTicks as a safety net. */
    void play(Simulation sim, int maxTicks) {
        actAt(sim, 0);
        while (!sim.isGameOver() && sim.getCurrentTick() < maxTicks) {
            sim.tick();
            actAt(sim, sim.getCurrentTick());
        }
    }

    private void actAt(Simulation sim, int tick) {
        for (Function<Network, PlayerAction> action : plan.getOrDefault(tick, List.of())) {
            results.add(sim.perform(action.apply(sim.getNetwork())));
        }
    }

    /** @return the result of every planned action, in the order performed */
    List<ActionResult> getResults() {
        return results;
    }
}
