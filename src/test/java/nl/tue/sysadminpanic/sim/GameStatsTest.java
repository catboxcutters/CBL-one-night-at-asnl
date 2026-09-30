package nl.tue.sysadminpanic.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import nl.tue.sysadminpanic.action.BlockIpAction;
import nl.tue.sysadminpanic.action.InspectAction;
import nl.tue.sysadminpanic.action.IsolateAction;
import nl.tue.sysadminpanic.attack.AttackCampaign;
import nl.tue.sysadminpanic.attack.PhishingStrategy;
import nl.tue.sysadminpanic.model.AlertType;
import nl.tue.sysadminpanic.model.Severity;
import org.junit.jupiter.api.Test;

/**
 * Tests for the end-of-game statistics.
 */
class GameStatsTest extends ActionTestBase {

    @Test
    void newGameHasEmptyStats() {
        GameStats stats = sim.getStats();
        assertFalse(stats.won());
        assertEquals(100.0, stats.finalHealth());
        assertEquals(0, stats.totalThreats());
        assertEquals(0, stats.systemsCompromised());
        assertEquals(0, stats.moneySpent());
    }

    @Test
    void campaignWithRealAlertCountsAsDetected() {
        startBruteForce(office.server);
        assertEquals(1, sim.getStats().threatsDetected());
    }

    @Test
    void silentCampaignIsDetectedOnlyByInspectingACompromisedDevice() {
        startPhishing(office.pc2);
        office.runUntil(3);
        assertEquals(0, sim.getStats().threatsDetected());
        sim.perform(new InspectAction(office.pc2));
        assertEquals(1, sim.getStats().threatsDetected());
    }

    @Test
    void falsePositiveDoesNotDetectAnything() {
        startPhishing(office.pc2);
        sim.raiseAlert(AlertType.PORT_SCAN, IP, office.pc1, Severity.LOW, "fp", true);
        assertEquals(0, sim.getStats().threatsDetected());
    }

    @Test
    void stoppedAndContainedCampaignsCountAsContained() {
        startBruteForce(office.server);
        sim.perform(new BlockIpAction(IP));
        startPhishingFrom("198.51.100.7");
        office.runUntil(3);
        sim.perform(new IsolateAction(office.pc2));
        office.runUntil(4);
        GameStats stats = sim.getStats();
        assertEquals(2, stats.threatsContained());
        assertEquals(2, stats.totalThreats());
    }

    @Test
    void systemsCompromisedCountsDistinctDevices() {
        startPhishing(office.pc2);
        office.runUntil(23);
        // PC-02, ROUTER and SERVER-01.
        assertEquals(3, sim.getStats().systemsCompromised());
    }

    @Test
    void moneySpentIsTotalOfPaidActions() {
        sim.perform(new BlockIpAction(IP));
        sim.perform(new IsolateAction(office.pc1));
        sim.perform(new InspectAction(office.pc1));
        assertEquals(500, sim.getStats().moneySpent());
    }

    private void startPhishingFrom(String ip) {
        sim.startCampaign(new AttackCampaign(ip, office.pc2, office.db, new PhishingStrategy()));
    }
}
