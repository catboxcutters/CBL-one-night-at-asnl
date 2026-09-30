package nl.tue.sysadminpanic.scenario;

import java.util.List;
import nl.tue.sysadminpanic.model.Network;

/**
 * A playable level: a network to defend and the waves of attacks against it.
 *
 * <p>The simulation only depends on this interface, so a new scenario is a new
 * class and needs no change to the game engine.
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

    /** @return the waves in the order they are played */
    List<WaveDefinition> getWaves();
}
