package nl.tue.sysadminpanic.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import nl.tue.sysadminpanic.attack.AttackCampaign;
import nl.tue.sysadminpanic.attack.BruteForceStrategy;
import nl.tue.sysadminpanic.attack.CampaignStatus;
import nl.tue.sysadminpanic.attack.PhishingStrategy;
import nl.tue.sysadminpanic.model.AlertType;
import nl.tue.sysadminpanic.model.DeviceState;
import nl.tue.sysadminpanic.model.Severity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for the simulation itself: ticking, hidden information, health, game over and events.
 */
class SimulationTest {

    private static final String IP = "203.0.113.42";

    private OfficeFixture office;
    private final List<GameEvent> events = new ArrayList<>();

    @BeforeEach
    void setUp() {
        office = new OfficeFixture();
        office.sim.addListener(events::add);
    }

    private AttackCampaign phishingOnPc2() {
        return new AttackCampaign(IP, office.pc2, office.db, new PhishingStrategy());
    }

    private <T extends GameEvent> List<T> eventsOfType(Class<T> type) {
        return events.stream().filter(type::isInstance).map(type::cast).toList();
    }

    @Test
    void newSimulationStartsAtTickZeroWithFullHealth() {
        assertEquals(0, office.sim.getCurrentTick());
        assertEquals(100.0, office.sim.getHealth());
        assertFalse(office.sim.isGameOver());
    }

    @Test
    void tickAdvancesClockAndPublishesEvent() {
        office.sim.tick();
        office.sim.tick();
        assertEquals(2, office.sim.getCurrentTick());
        assertEquals(List.of(new GameEvent.TickAdvanced(1), new GameEvent.TickAdvanced(2)), events);
    }

    @Test
    void quietNetworkLosesNoHealth() {
        office.runUntil(100);
        assertEquals(100.0, office.sim.getHealth());
    }

    // --- Hidden information ---

    @Test
    void alertMakesHealthyDeviceLookSuspicious() {
        office.sim.raiseAlert(AlertType.PORT_SCAN, IP, office.pc1, Severity.LOW, "test", true);
        assertEquals(DeviceState.SUSPICIOUS, office.pc1.getKnownState());
        assertEquals(DeviceState.HEALTHY, office.pc1.getTrueState());
    }

    @Test
    void alertNeverMarksDeviceAsCompromised() {
        office.pc1.setKnownState(DeviceState.COMPROMISED);
        office.sim.raiseAlert(AlertType.PORT_SCAN, IP, office.pc1, Severity.LOW, "test", false);
        assertEquals(DeviceState.COMPROMISED, office.pc1.getKnownState());
    }

    @Test
    void silentCompromiseDoesNotChangeKnownState() {
        office.sim.startCampaign(phishingOnPc2());
        office.runUntil(3);
        assertEquals(DeviceState.COMPROMISED, office.pc2.getTrueState());
        assertEquals(DeviceState.HEALTHY, office.pc2.getKnownState());
    }

    @Test
    void bruteForceAlertMakesServerSuspiciousButNotCompromised() {
        office.sim.startCampaign(new AttackCampaign(IP, office.server, office.db,
                new BruteForceStrategy()));
        office.runUntil(15);
        assertEquals(DeviceState.SUSPICIOUS, office.server.getKnownState());
        assertEquals(DeviceState.COMPROMISED, office.server.getTrueState());
    }

    @Test
    void alertsGetIncreasingIdsAndCurrentTick() {
        office.runUntil(7);
        office.sim.raiseAlert(AlertType.PORT_SCAN, IP, office.pc1, Severity.LOW, "a", false);
        office.sim.raiseAlert(AlertType.PORT_SCAN, IP, office.pc3, Severity.LOW, "b", false);
        assertEquals(1, office.sim.getAlerts().get(0).id());
        assertEquals(2, office.sim.getAlerts().get(1).id());
        assertEquals(7, office.sim.getAlerts().get(1).tick());
    }

    // --- Health ---

    @Test
    void compromisedDeviceDrainsHealthEveryTick() {
        office.sim.startCampaign(phishingOnPc2());
        office.runUntil(3);
        assertEquals(99.9, office.sim.getHealth(), 1e-9);
        office.runUntil(4);
        assertEquals(99.8, office.sim.getHealth(), 1e-9);
    }

    @Test
    void isolatedDeviceDrainsHealthEveryTick() {
        office.db.setIsolated(true);
        office.runUntil(10);
        assertEquals(100.0 - 10 * 0.25, office.sim.getHealth(), 1e-9);
    }

    @Test
    void unattendedPhishingCampaignLeavesExpectedHealth() {
        // Ticks 3-12: 0.1, 13-22: 0.4, 23-32: 0.8, 33: 1.3, 34-53: 2.3 => 60.3 total.
        office.sim.startCampaign(phishingOnPc2());
        office.runUntil(53);
        assertEquals(39.7, office.sim.getHealth(), 1e-9);
    }

    @Test
    void gameIsLostWhenHealthReachesZero() {
        office.sim.startCampaign(phishingOnPc2());
        office.runUntil(200);
        assertTrue(office.sim.isGameOver());
        assertEquals(0.0, office.sim.getHealth());
        // After tick 53 the drain is 1.3 per tick: 39.7 / 1.3 runs out on tick 84.
        assertEquals(84, office.sim.getCurrentTick());
        assertEquals(List.of(new GameEvent.GameOver(false)),
                eventsOfType(GameEvent.GameOver.class));
    }

    @Test
    void tickDoesNothingAfterGameOver() {
        office.sim.startCampaign(phishingOnPc2());
        office.runUntil(200);
        int eventCount = events.size();
        office.sim.tick();
        assertEquals(84, office.sim.getCurrentTick());
        assertEquals(eventCount, events.size());
    }

    // --- Events ---

    @Test
    void compromisePublishesDeviceStateChanged() {
        office.sim.startCampaign(phishingOnPc2());
        office.runUntil(3);
        assertEquals(List.of(new GameEvent.DeviceStateChanged(office.pc2)),
                eventsOfType(GameEvent.DeviceStateChanged.class));
    }

    @Test
    void alertPublishesAlertRaisedAndStateChange() {
        office.sim.raiseAlert(AlertType.PORT_SCAN, IP, office.pc1, Severity.LOW, "test", false);
        assertEquals(1, eventsOfType(GameEvent.AlertRaised.class).size());
        assertEquals(1, eventsOfType(GameEvent.DeviceStateChanged.class).size());
    }

    @Test
    void containedCampaignPublishesCampaignEndedOnce() {
        office.sim.startCampaign(phishingOnPc2());
        office.runUntil(3);
        office.pc2.setIsolated(true);
        office.runUntil(20);
        assertEquals(List.of(new GameEvent.CampaignEnded(IP, CampaignStatus.CONTAINED)),
                eventsOfType(GameEvent.CampaignEnded.class));
    }

    @Test
    void healthChangePublishesNewValue() {
        office.sim.startCampaign(phishingOnPc2());
        office.runUntil(3);
        List<GameEvent.HealthChanged> changes = eventsOfType(GameEvent.HealthChanged.class);
        assertEquals(1, changes.size());
        assertEquals(99.9, changes.get(0).health(), 1e-9);
    }
}
