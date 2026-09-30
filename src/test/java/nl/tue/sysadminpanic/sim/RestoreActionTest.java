package nl.tue.sysadminpanic.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import nl.tue.sysadminpanic.action.ActionResult;
import nl.tue.sysadminpanic.action.RestoreAction;
import nl.tue.sysadminpanic.attack.AttackCampaign;
import nl.tue.sysadminpanic.attack.CampaignStatus;
import nl.tue.sysadminpanic.attack.LateralMovementStage;
import nl.tue.sysadminpanic.model.DeviceState;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link RestoreAction}.
 *
 * <p>Money: $1,500 at the start plus $50 every 5 ticks, so exactly one restore is
 * affordable at tick 0 and a second one only after 150 ticks.
 */
class RestoreActionTest extends ActionTestBase {

    @Test
    void restoreCostsFifteenHundredAndMakesDeviceHealthy() {
        startPhishing(office.pc2);
        office.runUntil(3);
        ActionResult result = sim.perform(new RestoreAction(office.pc2));
        assertTrue(result.success());
        assertEquals(DeviceState.HEALTHY, office.pc2.getTrueState());
        assertEquals(DeviceState.HEALTHY, office.pc2.getKnownState());
        assertEquals(0, sim.getMoney());
    }

    @Test
    void restoringAttackersOnlyDeviceStopsCampaign() {
        AttackCampaign campaign = startPhishing(office.pc2);
        office.runUntil(5);
        sim.perform(new RestoreAction(office.pc2));
        assertEquals(CampaignStatus.STOPPED, campaign.getStatus());
        assertTrue(campaign.getControlledDevices().isEmpty());
    }

    @Test
    void restoreStopsTheHealthDrain() {
        startPhishing(office.pc2);
        office.runUntil(5);
        sim.perform(new RestoreAction(office.pc2));
        double health = sim.getHealth();
        office.runUntil(50);
        assertEquals(health, sim.getHealth());
    }

    @Test
    void restoringAttackersCurrentDevicePushesItBackOneStep() {
        AttackCampaign campaign = startPhishing(office.pc2);
        office.runUntil(13);
        assertEquals(office.router, campaign.getCurrentDevice());
        sim.perform(new RestoreAction(office.router));
        assertTrue(campaign.isActive());
        assertEquals(office.pc2, campaign.getCurrentDevice());
        // The attacker needs a full hop time again to retake the router.
        office.runUntil(22);
        assertEquals(DeviceState.HEALTHY, office.router.getTrueState());
        office.runUntil(23);
        assertEquals(office.router, campaign.getCurrentDevice());
    }

    @Test
    void restoringEarlierDeviceLeavesAttackerWhereItIs() {
        AttackCampaign campaign = startPhishing(office.pc2);
        office.runUntil(13);
        sim.perform(new RestoreAction(office.pc2));
        assertEquals(office.router, campaign.getCurrentDevice());
        assertEquals(List.of(office.router), List.copyOf(campaign.getControlledDevices()));
        office.runUntil(23);
        assertEquals(office.server, campaign.getCurrentDevice());
    }

    @Test
    void restoringTargetDuringExfiltrationSendsAttackerBackToLateralMovement() {
        AttackCampaign campaign = startPhishing(office.pc2);
        office.runUntil(40);
        sim.perform(new RestoreAction(office.db));
        assertEquals(office.server, campaign.getCurrentDevice());
        assertInstanceOf(LateralMovementStage.class, campaign.getStage());
        office.runUntil(49);
        assertEquals(DeviceState.HEALTHY, office.db.getTrueState());
        office.runUntil(50);
        assertEquals(DeviceState.COMPROMISED, office.db.getTrueState());
    }

    @Test
    void secondRestoreIsUnaffordableAtFirst() {
        sim.perform(new RestoreAction(office.pc1));
        ActionResult result = sim.perform(new RestoreAction(office.pc2));
        assertEquals("Costs $1,500, you have $0", result.message());
    }
}
