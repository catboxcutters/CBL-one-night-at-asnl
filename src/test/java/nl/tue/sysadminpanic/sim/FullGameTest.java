package nl.tue.sysadminpanic.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import nl.tue.sysadminpanic.action.ActionResult;
import nl.tue.sysadminpanic.action.BlockIpAction;
import nl.tue.sysadminpanic.action.FirewallRuleAction;
import nl.tue.sysadminpanic.action.InspectAction;
import nl.tue.sysadminpanic.scenario.OfficeNetworkScenario;
import org.junit.jupiter.api.Test;

/**
 * Plays the complete Office Network game headless, from start screen to summary.
 *
 * <p>Because the game has no randomness, the same actions always give exactly
 * the same result, so these tests can check the final numbers precisely.
 */
class FullGameTest {

    private Simulation newGame() {
        return Simulation.forScenario(new OfficeNetworkScenario());
    }

    /**
     * A sensible defence, reacting only to what the alerts show.
     *
     * <pre>
     * Wave 1 (tick 10)  port scan from .42 raises a LOW alert  -> Block IP at once.
     * Wave 2 (tick 26)  silent phishing on PC-02; the hint alert comes at tick 41,
     *                   when the attacker is already on ROUTER  -> inspect both, then a
     *                   firewall rule ROUTER-SERVER-01 cuts its only way to the database.
     * Wave 3 (tick 57)  false positive on SERVER-01 -> inspect: healthy, spend nothing.
     *                   Real port scan from .23 at tick 67 -> Block IP.
     * Wave 4 (tick 83)  brute force from .77 -> Block IP. The phishing on PC-03 is
     *                   contained by the firewall rule from wave 2 without the player noticing.
     * Wave 5 (tick 102) same again: phishing on PC-01 contained by the rule,
     *                   brute force from .99 at tick 107 -> Block IP. Won at tick 112.
     * </pre>
     */
    private ScriptedPlayer sensibleDefender() {
        return new ScriptedPlayer()
                .at(10, net -> new BlockIpAction("203.0.113.42"))
                .at(41, net -> new InspectAction(net.getDevice("PC-02")))
                .at(41, net -> new InspectAction(net.getDevice("ROUTER")))
                .at(41, net -> new FirewallRuleAction(net.getDevice("ROUTER"),
                        net.getDevice("SERVER-01")))
                .at(57, net -> new InspectAction(net.getDevice("SERVER-01")))
                .at(67, net -> new BlockIpAction("198.51.100.23"))
                .at(83, net -> new BlockIpAction("203.0.113.77"))
                .at(107, net -> new BlockIpAction("198.51.100.99"));
    }

    @Test
    void sensibleDefenderWinsTheGame() {
        Simulation sim = newGame();
        ScriptedPlayer player = sensibleDefender();
        player.play(sim, 1000);

        assertTrue(player.getResults().stream().allMatch(ActionResult::success));
        assertTrue(sim.isGameOver());
        assertTrue(sim.isWon());
        assertEquals(112, sim.getCurrentTick());
    }

    @Test
    void sensibleDefenderGetsExpectedSummary() {
        Simulation sim = newGame();
        sensibleDefender().play(sim, 1000);

        // Drain: 0.1 on ticks 29-38 (PC-02), 0.4 on 39-85 (+ROUTER),
        // 0.5 on 86-104 (+PC-03), 0.6 on 105-112 (+PC-01) = 34.1 in total.
        GameStats stats = sim.getStats();
        assertEquals(65.9, stats.finalHealth(), 1e-9);
        assertEquals(5, stats.wavesSurvived());
        assertEquals(5, stats.totalWaves());
        assertEquals(7, stats.totalThreats());
        // The phishing attacks of waves 4 and 5 were never seen by the player.
        assertEquals(5, stats.threatsDetected());
        assertEquals(7, stats.threatsContained());
        // PC-02, ROUTER, PC-03 and PC-01.
        assertEquals(4, stats.systemsCompromised());
        // 4 x Block IP ($200) + 1 firewall rule ($500).
        assertEquals(1300, stats.moneySpent());
    }

    @Test
    void doingNothingLosesTheGameDuringWaveTwo() {
        Simulation sim = newGame();
        new ScriptedPlayer().play(sim, 1000);

        // Wave 1's attacker steals the database (ticks 41-60). Wave 2 starts at 75,
        // and during its exfiltration health runs out on tick 110.
        assertTrue(sim.isGameOver());
        assertEquals(false, sim.isWon());
        assertEquals(110, sim.getCurrentTick());
        GameStats stats = sim.getStats();
        assertEquals(0.0, stats.finalHealth());
        assertEquals(1, stats.wavesSurvived());
        assertEquals(2, stats.totalThreats());
        assertEquals(2, stats.threatsDetected());
        assertEquals(0, stats.threatsContained());
        // SERVER-01, DB-01, PC-02 and ROUTER.
        assertEquals(4, stats.systemsCompromised());
        assertEquals(0, stats.moneySpent());
    }

    @Test
    void playAgainStartsFromACleanNetwork() {
        OfficeNetworkScenario scenario = new OfficeNetworkScenario();
        Simulation first = Simulation.forScenario(scenario);
        new ScriptedPlayer().play(first, 1000);
        Simulation second = Simulation.forScenario(scenario);
        assertEquals(100.0, second.getHealth());
        assertTrue(second.getNetwork().getDevices().stream()
                .allMatch(d -> d.getTrueState().name().equals("HEALTHY")));
    }
}
