package nl.tue.sysadminpanic.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import nl.tue.sysadminpanic.action.ActionResult;
import nl.tue.sysadminpanic.action.PatchAction;
import nl.tue.sysadminpanic.attack.AttackCampaign;
import nl.tue.sysadminpanic.attack.CampaignStatus;
import nl.tue.sysadminpanic.model.DeviceState;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PatchAction}.
 */
class PatchActionTest extends ActionTestBase {

    @Test
    void patchCostsEightHundredAndPatchesDevice() {
        ActionResult result = sim.perform(new PatchAction(office.server));
        assertTrue(result.success());
        assertTrue(office.server.isPatched());
        assertEquals(700, sim.getMoney());
    }

    @Test
    void patchingDuringBruteForceMakesItFail() {
        AttackCampaign campaign = startBruteForce(office.server);
        office.runUntil(10);
        sim.perform(new PatchAction(office.server));
        office.runUntil(15);
        assertEquals(CampaignStatus.CONTAINED, campaign.getStatus());
        assertEquals(DeviceState.HEALTHY, office.server.getTrueState());
    }

    @Test
    void patchingNextHopSlowsLateralMovement() {
        AttackCampaign campaign = startPhishing(office.pc2);
        office.runUntil(3);
        sim.perform(new PatchAction(office.router));
        office.runUntil(13);
        assertEquals(office.pc2, campaign.getCurrentDevice());
        office.runUntil(23);
        assertEquals(office.router, campaign.getCurrentDevice());
    }

    @Test
    void patchingDoesNotRemoveAnAttacker() {
        startPhishing(office.pc2);
        office.runUntil(3);
        sim.perform(new PatchAction(office.pc2));
        assertEquals(DeviceState.COMPROMISED, office.pc2.getTrueState());
    }

    @Test
    void patchingTwiceIsInvalid() {
        sim.perform(new PatchAction(office.server));
        ActionResult result = sim.perform(new PatchAction(office.server));
        assertFalse(result.success());
        assertEquals(700, sim.getMoney());
    }
}
