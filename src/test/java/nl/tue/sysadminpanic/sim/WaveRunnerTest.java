package nl.tue.sysadminpanic.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import nl.tue.sysadminpanic.attack.BruteForceStrategy;
import nl.tue.sysadminpanic.attack.PhishingStrategy;
import nl.tue.sysadminpanic.model.AlertType;
import nl.tue.sysadminpanic.model.Severity;
import nl.tue.sysadminpanic.scenario.ScriptedEvent;
import nl.tue.sysadminpanic.scenario.WaveDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for the wave script rules, using small hand-made waves on the office network.
 */
class WaveRunnerTest {

    /** An event together with the tick at which it was published. */
    private record Timed(int tick, GameEvent event) {
    }

    private OfficeFixture office;
    private Simulation sim;
    private final List<Timed> timeline = new ArrayList<>();

    @BeforeEach
    void setUp() {
        office = new OfficeFixture();
    }

    private void start(WaveDefinition... waves) {
        sim = new Simulation(office.network, List.of(waves));
        sim.addListener(event -> timeline.add(new Timed(sim.getCurrentTick(), event)));
    }

    private void runUntil(int tick) {
        while (sim.getCurrentTick() < tick && !sim.isGameOver()) {
            sim.tick();
        }
    }

    private List<Timed> timed(Class<? extends GameEvent> type) {
        return timeline.stream().filter(t -> type.isInstance(t.event())).toList();
    }

    private static ScriptedEvent alertOnPc1(int offset) {
        return new ScriptedEvent.ScriptedAlert(offset, AlertType.PORT_SCAN, "198.51.100.1",
                "PC-01", Severity.LOW, "scan", true, List.of("harmless"));
    }

    private static ScriptedEvent bruteForceOnServer(int offset) {
        return new ScriptedEvent.StartCampaign(offset, "203.0.113.5", "SERVER-01", "DB-01",
                new BruteForceStrategy(), false);
    }

    private static WaveDefinition wave(String name, ScriptedEvent... events) {
        return new WaveDefinition(name, List.of(events));
    }

    @Test
    void firstWaveStartsAtTickTen() {
        start(wave("One", alertOnPc1(0)), wave("Two", alertOnPc1(0)));
        runUntil(9);
        assertTrue(timed(GameEvent.WaveStarted.class).isEmpty());
        runUntil(10);
        assertEquals(List.of(new Timed(10, new GameEvent.WaveStarted(1, 2, "One"))),
                timed(GameEvent.WaveStarted.class));
    }

    @Test
    void eventsFireAtTheirOffsetFromWaveStart() {
        start(wave("One", alertOnPc1(0), alertOnPc1(3)));
        runUntil(20);
        assertEquals(10, sim.getAlerts().get(0).tick());
        assertEquals(13, sim.getAlerts().get(1).tick());
    }

    @Test
    void scriptedAlertWritesItsLogLinesOnTheDevice() {
        start(wave("One", alertOnPc1(0)));
        runUntil(10);
        assertEquals("harmless", office.pc1.getLog().get(0).text());
        assertTrue(sim.getAlerts().get(0).falsePositive());
    }

    @Test
    void campaignStartedByWaveTakesFirstStepNextTick() {
        start(wave("One", bruteForceOnServer(0)), wave("Two", alertOnPc1(0)));
        // Started at tick 10, so the 15th attempt succeeds at tick 25.
        runUntil(24);
        assertTrue(office.server.getLog().stream().noneMatch(e -> e.text().startsWith("Successful")));
        runUntil(25);
        assertEquals("COMPROMISED", office.server.getTrueState().name());
    }

    @Test
    void waveEndsWhenItsCampaignsEndAndNextWaveStartsFifteenTicksLater() {
        office.server.setPatched(true);
        start(wave("One", bruteForceOnServer(0)), wave("Two", alertOnPc1(0)));
        // Patched: the brute force gives up at tick 10 + 15 = 25.
        runUntil(40);
        assertEquals(List.of(new Timed(25, new GameEvent.WaveEnded(1))),
                timed(GameEvent.WaveEnded.class).subList(0, 1));
        assertEquals(new Timed(40, new GameEvent.WaveStarted(2, 2, "Two")),
                timed(GameEvent.WaveStarted.class).get(1));
    }

    @Test
    void waveWaitsForItsLaterEventsEvenWhenNoCampaignIsActive() {
        start(wave("One", alertOnPc1(0), alertOnPc1(20)), wave("Two", alertOnPc1(0)));
        runUntil(29);
        assertTrue(timed(GameEvent.WaveEnded.class).isEmpty());
        runUntil(30);
        assertEquals(1, timed(GameEvent.WaveEnded.class).size());
    }

    @Test
    void activeCampaignKeepsWaveRunning() {
        start(wave("One", new ScriptedEvent.StartCampaign(0, "203.0.113.5", "PC-02", "DB-01",
                new PhishingStrategy(), false)), wave("Two", alertOnPc1(0)));
        runUntil(40);
        assertTrue(timed(GameEvent.WaveEnded.class).isEmpty());
        assertEquals(1, sim.getCurrentWaveNumber());
    }

    @Test
    void playerWinsWhenLastWaveEnds() {
        start(wave("One", alertOnPc1(0)), wave("Two", alertOnPc1(0)));
        runUntil(100);
        // Wave 1: ticks 10-10, wave 2 starts 15 ticks later and ends at once.
        assertEquals(25, sim.getCurrentTick());
        assertTrue(sim.isGameOver());
        assertTrue(sim.isWon());
        assertEquals(new Timed(25, new GameEvent.GameOver(true)), timeline.get(timeline.size() - 1));
        assertEquals(2, sim.getStats().wavesSurvived());
    }

    @Test
    void waveNumberAdvancesAfterEachWave() {
        start(wave("One", alertOnPc1(0)), wave("Two", alertOnPc1(0)), wave("Three", alertOnPc1(0)));
        assertEquals(1, sim.getCurrentWaveNumber());
        assertEquals(3, sim.getTotalWaves());
        runUntil(11);
        assertEquals(2, sim.getCurrentWaveNumber());
    }

    @Test
    void withoutWavesTheGameNeverEndsOnItsOwn() {
        start();
        runUntil(500);
        assertFalse(sim.isGameOver());
    }

    @Test
    void waveDefinitionRejectsEventsOutOfOrder() {
        assertThrows(IllegalArgumentException.class,
                () -> wave("Bad", alertOnPc1(5), alertOnPc1(2)));
    }
}
