package nl.tue.sysadminpanic.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import nl.tue.sysadminpanic.action.ActionResult;
import nl.tue.sysadminpanic.action.IsolateAction;
import nl.tue.sysadminpanic.action.ReconnectAction;
import nl.tue.sysadminpanic.attack.AttackCampaign;
import nl.tue.sysadminpanic.attack.CampaignStatus;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link IsolateAction} and {@link ReconnectAction}.
 */
class IsolateActionTest extends ActionTestBase {

    @Test
    void isolateCostsThreeHundredAndIsolatesDevice() {
        ActionResult result = sim.perform(new IsolateAction(office.pc1));
        assertTrue(result.success());
        assertTrue(office.pc1.isIsolated());
        assertEquals(1200, sim.getMoney());
    }

    @Test
    void isolatingCompromisedPcContainsCampaignOnNextTick() {
        AttackCampaign campaign = startPhishing(office.pc2);
        office.runUntil(3);
        sim.perform(new IsolateAction(office.pc2));
        office.runUntil(4);
        assertEquals(CampaignStatus.CONTAINED, campaign.getStatus());
    }

    @Test
    void isolatedDeviceDrainsHealth() {
        sim.perform(new IsolateAction(office.server));
        office.runUntil(10);
        assertEquals(100.0 - 10 * 0.2, sim.getHealth(), 1e-9);
    }

    @Test
    void isolatingTwiceIsInvalid() {
        sim.perform(new IsolateAction(office.pc1));
        ActionResult result = sim.perform(new IsolateAction(office.pc1));
        assertFalse(result.success());
        assertEquals("PC-01 is already isolated", result.message());
    }

    @Test
    void reconnectIsFreeAndUndoesIsolation() {
        sim.perform(new IsolateAction(office.pc1));
        ActionResult result = sim.perform(new ReconnectAction(office.pc1));
        assertTrue(result.success());
        assertFalse(office.pc1.isIsolated());
        assertEquals(1200, sim.getMoney());
    }

    @Test
    void reconnectingConnectedDeviceIsInvalid() {
        ActionResult result = sim.perform(new ReconnectAction(office.pc1));
        assertFalse(result.success());
        assertEquals("PC-01 is not isolated", result.message());
    }
}
