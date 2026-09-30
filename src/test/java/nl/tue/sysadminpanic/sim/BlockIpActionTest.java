package nl.tue.sysadminpanic.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import nl.tue.sysadminpanic.action.ActionResult;
import nl.tue.sysadminpanic.action.BlockIpAction;
import nl.tue.sysadminpanic.attack.AttackCampaign;
import nl.tue.sysadminpanic.attack.BruteForceStrategy;
import nl.tue.sysadminpanic.attack.CampaignStatus;
import nl.tue.sysadminpanic.model.DeviceState;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link BlockIpAction}.
 */
class BlockIpActionTest extends ActionTestBase {

    @Test
    void blockIpCostsTwoHundred() {
        sim.perform(new BlockIpAction(IP));
        assertEquals(1300, sim.getMoney());
        assertTrue(sim.isIpBlocked(IP));
    }

    @Test
    void blockIpDuringReconStopsCampaign() {
        AttackCampaign campaign = new AttackCampaign(IP, office.server, office.db,
                new BruteForceStrategy());
        sim.startCampaignWithRecon(campaign);
        office.runUntil(3);
        ActionResult result = sim.perform(new BlockIpAction(IP));
        assertEquals("Blocked " + IP + ": incoming attack cut off", result.message());
        assertEquals(CampaignStatus.STOPPED, campaign.getStatus());
        office.runUntil(30);
        assertEquals(9, office.firewall.getLog().size());
        assertEquals(DeviceState.HEALTHY, office.server.getTrueState());
    }

    @Test
    void blockIpDuringBruteForceStopsCampaign() {
        AttackCampaign campaign = startBruteForce(office.server);
        office.runUntil(10);
        sim.perform(new BlockIpAction(IP));
        office.runUntil(30);
        assertEquals(CampaignStatus.STOPPED, campaign.getStatus());
        assertEquals(DeviceState.HEALTHY, office.server.getTrueState());
    }

    @Test
    void blockIpAfterAttackerGotInHasNoEffectOnIt() {
        AttackCampaign campaign = startPhishing(office.pc2);
        office.runUntil(5);
        ActionResult result = sim.perform(new BlockIpAction(IP));
        assertTrue(result.success());
        assertEquals("Blocked " + IP + ": no active connections from it", result.message());
        assertTrue(campaign.isActive());
    }

    @Test
    void blockIpOnlyStopsCampaignsFromThatIp() {
        AttackCampaign campaign = startBruteForce(office.server);
        sim.perform(new BlockIpAction("198.51.100.23"));
        assertTrue(campaign.isActive());
    }

    @Test
    void blockingSameIpTwiceIsInvalid() {
        sim.perform(new BlockIpAction(IP));
        ActionResult result = sim.perform(new BlockIpAction(IP));
        assertFalse(result.success());
        assertEquals(IP + " is already blocked", result.message());
        assertEquals(1300, sim.getMoney());
    }

    @Test
    void laterCampaignFromBlockedIpIsStoppedBeforeItStarts() {
        sim.perform(new BlockIpAction(IP));
        AttackCampaign campaign = new AttackCampaign(IP, office.server, office.db,
                new BruteForceStrategy());
        sim.startCampaignWithRecon(campaign);
        assertEquals(CampaignStatus.STOPPED, campaign.getStatus());
        assertTrue(sim.getAlerts().isEmpty());
    }
}
