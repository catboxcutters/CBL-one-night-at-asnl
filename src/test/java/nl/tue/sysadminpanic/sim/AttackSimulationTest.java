package nl.tue.sysadminpanic.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import nl.tue.sysadminpanic.attack.AttackCampaign;
import nl.tue.sysadminpanic.attack.BruteForceStrategy;
import nl.tue.sysadminpanic.attack.CampaignStatus;
import nl.tue.sysadminpanic.attack.ExfiltrationStage;
import nl.tue.sysadminpanic.attack.InitialAccessStage;
import nl.tue.sysadminpanic.attack.LateralMovementStage;
import nl.tue.sysadminpanic.attack.PhishingStrategy;
import nl.tue.sysadminpanic.model.Alert;
import nl.tue.sysadminpanic.model.AlertType;
import nl.tue.sysadminpanic.model.Device;
import nl.tue.sysadminpanic.model.DeviceState;
import nl.tue.sysadminpanic.model.LogEntry;
import nl.tue.sysadminpanic.model.Severity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for the attack stages and strategies, driven through the simulation.
 *
 * <p>Timeline of an unattended phishing attack on PC-02 started at tick 0:
 * PC-02 compromised at tick 3, ROUTER at 13, SERVER-01 at 23, DB-01 at 33,
 * data sent on ticks 34 to 53, campaign finished at tick 53.
 */
class AttackSimulationTest {

    private static final String IP = "203.0.113.42";

    private OfficeFixture office;

    @BeforeEach
    void setUp() {
        office = new OfficeFixture();
    }

    private AttackCampaign bruteForce(Device entry) {
        return new AttackCampaign(IP, entry, office.db, new BruteForceStrategy());
    }

    private AttackCampaign phishing(Device entry) {
        return new AttackCampaign(IP, entry, office.db, new PhishingStrategy());
    }

    private long logLinesContaining(Device device, String text) {
        return device.getLog().stream().filter(e -> e.text().contains(text)).count();
    }

    // --- Recon ---

    @Test
    void reconRaisesLowAlertOnFirewallAndLogsScanForFiveTicks() {
        office.sim.startCampaignWithRecon(bruteForce(office.server));
        Alert alert = office.sim.getAlerts().get(0);
        assertEquals(AlertType.PORT_SCAN, alert.type());
        assertEquals(Severity.LOW, alert.severity());
        assertEquals("FIREWALL", alert.targetDeviceId());

        office.runUntil(5);
        assertEquals(15, logLinesContaining(office.firewall, "Dropped connection attempt"));
    }

    @Test
    void reconIsFollowedByBruteForceAtTickFive() {
        AttackCampaign campaign = bruteForce(office.server);
        office.sim.startCampaignWithRecon(campaign);
        office.runUntil(4);
        assertEquals(1, office.sim.getAlerts().size());
        office.runUntil(5);
        assertInstanceOf(InitialAccessStage.class, campaign.getStage());
        assertEquals(Severity.HIGH, office.sim.getAlerts().get(1).severity());
    }

    @Test
    void reconPlusBruteForceCompromisesServerAtTickTwenty() {
        office.sim.startCampaignWithRecon(bruteForce(office.server));
        office.runUntil(19);
        assertEquals(DeviceState.HEALTHY, office.server.getTrueState());
        office.runUntil(20);
        assertEquals(DeviceState.COMPROMISED, office.server.getTrueState());
    }

    // --- Brute force ---

    @Test
    void unattendedBruteForceCompromisesServerAtExactlyTickFifteen() {
        office.sim.startCampaign(bruteForce(office.server));
        office.runUntil(14);
        assertEquals(DeviceState.HEALTHY, office.server.getTrueState());
        office.runUntil(15);
        assertEquals(DeviceState.COMPROMISED, office.server.getTrueState());
    }

    @Test
    void bruteForceRaisesHighAlertAndLogsEveryTick() {
        office.sim.startCampaign(bruteForce(office.server));
        assertEquals(Severity.HIGH, office.sim.getAlerts().get(0).severity());
        office.runUntil(15);
        assertEquals(14, logLinesContaining(office.server, "Failed login: admin from " + IP));
        assertEquals(1, logLinesContaining(office.server, "Successful login"));
    }

    @Test
    void patchedDeviceIsNeverBruteForced() {
        office.server.setPatched(true);
        AttackCampaign campaign = bruteForce(office.server);
        office.sim.startCampaign(campaign);
        office.runUntil(100);
        assertEquals(DeviceState.HEALTHY, office.server.getTrueState());
        assertEquals(0, logLinesContaining(office.server, "Successful login"));
    }

    @Test
    void bruteForceOnPatchedDeviceGivesUpAtTickFifteen() {
        office.server.setPatched(true);
        AttackCampaign campaign = bruteForce(office.server);
        office.sim.startCampaign(campaign);
        office.runUntil(14);
        assertTrue(campaign.isActive());
        office.runUntil(15);
        assertEquals(CampaignStatus.CONTAINED, campaign.getStatus());
        assertEquals(1, logLinesContaining(office.server, "Login attempts from " + IP + " stopped"));
    }

    // --- Phishing ---

    @Test
    void phishingSucceedsSilentlyAtTickThree() {
        office.sim.startCampaign(phishing(office.pc2));
        office.runUntil(2);
        assertEquals(DeviceState.HEALTHY, office.pc2.getTrueState());
        assertTrue(office.pc2.getLog().isEmpty());
        office.runUntil(3);
        assertEquals(DeviceState.COMPROMISED, office.pc2.getTrueState());
        assertTrue(office.sim.getAlerts().isEmpty());
        assertEquals(1, logLinesContaining(office.pc2, "Process started"));
    }

    @Test
    void patchingDoesNotStopPhishing() {
        office.pc2.setPatched(true);
        office.sim.startCampaign(phishing(office.pc2));
        office.runUntil(3);
        assertEquals(DeviceState.COMPROMISED, office.pc2.getTrueState());
    }

    // --- Lateral movement ---

    @Test
    void attackerHopsAlongShortestPathEveryTenTicks() {
        AttackCampaign campaign = phishing(office.pc2);
        office.sim.startCampaign(campaign);
        office.runUntil(12);
        assertEquals(office.pc2, campaign.getCurrentDevice());
        office.runUntil(13);
        assertEquals(office.router, campaign.getCurrentDevice());
        office.runUntil(23);
        assertEquals(office.server, campaign.getCurrentDevice());
        office.runUntil(33);
        assertEquals(office.db, campaign.getCurrentDevice());
        assertEquals(List.of(office.pc2, office.router, office.server, office.db),
                List.copyOf(campaign.getControlledDevices()));
    }

    @Test
    void hopLogsInboundConnectionOnNewDevice() {
        office.sim.startCampaign(phishing(office.pc2));
        office.runUntil(13);
        LogEntry entry = office.router.getLog().get(0);
        assertEquals(13, entry.tick());
        assertEquals("Inbound admin session from PC-02 (SMB)", entry.text());
    }

    @Test
    void hopOntoPatchedDeviceTakesTwentyTicks() {
        office.router.setPatched(true);
        AttackCampaign campaign = phishing(office.pc2);
        office.sim.startCampaign(campaign);
        office.runUntil(22);
        assertEquals(DeviceState.HEALTHY, office.router.getTrueState());
        office.runUntil(23);
        assertEquals(office.router, campaign.getCurrentDevice());
    }

    @Test
    void isolatingTheCompromisedPcContainsTheCampaign() {
        AttackCampaign campaign = phishing(office.pc2);
        office.sim.startCampaign(campaign);
        office.runUntil(3);
        office.pc2.setIsolated(true);
        office.runUntil(4);
        assertEquals(CampaignStatus.CONTAINED, campaign.getStatus());
        office.runUntil(50);
        assertEquals(DeviceState.HEALTHY, office.router.getTrueState());
    }

    @Test
    void firewallRuleInFrontOfTargetContainsTheCampaign() {
        AttackCampaign campaign = phishing(office.pc2);
        office.sim.startCampaign(campaign);
        office.runUntil(23);
        office.network.findLink(office.server, office.db).orElseThrow().setBlocked(true);
        office.runUntil(24);
        assertEquals(CampaignStatus.CONTAINED, campaign.getStatus());
        assertEquals(DeviceState.HEALTHY, office.db.getTrueState());
    }

    @Test
    void attackerTakesDetourWhenPathIsBlocked() {
        office.network.addLink(office.pc3, office.server);
        AttackCampaign campaign = phishing(office.pc2);
        office.sim.startCampaign(campaign);
        office.runUntil(13);
        office.network.findLink(office.router, office.server).orElseThrow().setBlocked(true);
        office.runUntil(23);
        assertEquals(office.pc3, campaign.getCurrentDevice());
        assertTrue(campaign.isActive());
    }

    // --- Exfiltration ---

    @Test
    void exfiltrationLastsTwentyTicksThenCampaignFinishes() {
        AttackCampaign campaign = phishing(office.pc2);
        office.sim.startCampaign(campaign);
        office.runUntil(33);
        assertInstanceOf(ExfiltrationStage.class, campaign.getStage());
        office.runUntil(52);
        assertTrue(campaign.isActive());
        office.runUntil(53);
        assertEquals(CampaignStatus.FINISHED, campaign.getStatus());
        assertEquals(20, logLinesContaining(office.db, "Large outbound transfer"));
    }

    @Test
    void exfiltrationDrainsOneExtraHealthPerTick() {
        office.sim.startCampaign(phishing(office.pc2));
        office.runUntil(33);
        double before = office.sim.getHealth();
        office.runUntil(34);
        // Compromised: PC-02 (1) + ROUTER (3) + SERVER-01 (4) + DB-01 (5) = 13 x 0.1,
        // plus 1.0 for the exfiltration.
        assertEquals(2.3, before - office.sim.getHealth(), 1e-9);
    }

    @Test
    void exfiltrationPausesWhileRouteToInternetIsBrokenAndThenResumes() {
        AttackCampaign campaign = phishing(office.pc2);
        office.sim.startCampaign(campaign);
        office.runUntil(33);
        var link = office.network.findLink(office.firewall, office.router).orElseThrow();
        link.setBlocked(true);
        office.runUntil(40);
        assertTrue(campaign.isActive());
        assertEquals(0, logLinesContaining(office.db, "Large outbound transfer"));
        link.setBlocked(false);
        // Paused on ticks 34-40, so the 20 chunks are sent on ticks 41-60.
        office.runUntil(59);
        assertTrue(campaign.isActive());
        office.runUntil(60);
        assertEquals(CampaignStatus.FINISHED, campaign.getStatus());
    }

    // --- Campaign bookkeeping ---

    @Test
    void campaignIsOutsideTheNetworkUntilCompromise() {
        AttackCampaign campaign = phishing(office.pc2);
        office.sim.startCampaign(campaign);
        office.runUntil(2);
        assertNull(campaign.getCurrentDevice());
        assertTrue(campaign.getControlledDevices().isEmpty());
        office.runUntil(3);
        assertInstanceOf(LateralMovementStage.class, campaign.getStage());
    }
}
