package nl.tue.sysadminpanic.scenario;

import nl.tue.sysadminpanic.model.Network;

/**
 * A playable level: a network to defend.
 *
 * <p>The rest of the game only depends on this interface, so a new scenario is a
 * new class and needs no change to the engine.
 */
public interface Scenario {

    /** @return the scenario's display name */
    String getName();

    /**
     * Builds a fresh network. Called once per game, so every game starts clean.
     *
     * @return a new network in its initial state
     */
    Network createNetwork();
}
