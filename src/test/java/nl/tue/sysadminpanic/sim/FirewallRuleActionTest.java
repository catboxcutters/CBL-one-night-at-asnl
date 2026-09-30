package nl.tue.sysadminpanic.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import nl.tue.sysadminpanic.action.ActionResult;
import nl.tue.sysadminpanic.action.FirewallRuleAction;
import nl.tue.sysadminpanic.attack.AttackCampaign;
import nl.tue.sysadminpanic.attack.CampaignStatus;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link FirewallRuleAction}.
 */
class FirewallRuleActionTest extends ActionTestBase {

    @Test
    void firewallRuleCostsFiveHundredAndBlocksLink() {
        ActionResult result = sim.perform(new FirewallRuleAction(office.server, office.db));
        assertTrue(result.success());
        assertEquals("Firewall rule added: SERVER-01 - DB-01 blocked", result.message());
        assertTrue(office.network.findLink(office.server, office.db).orElseThrow().isBlocked());
        assertEquals(1000, sim.getMoney());
    }

    @Test
    void firewallRuleContainsAttackerWithNoOtherRoute() {
        AttackCampaign campaign = startPhishing(office.pc2);
        office.runUntil(13);
        sim.perform(new FirewallRuleAction(office.router, office.server));
        office.runUntil(14);
        assertEquals(CampaignStatus.CONTAINED, campaign.getStatus());
    }

    @Test
    void firewallRuleCausesNoHealthDrain() {
        sim.perform(new FirewallRuleAction(office.server, office.db));
        office.runUntil(50);
        assertEquals(100.0, sim.getHealth());
    }

    @Test
    void firewallRuleNeedsDirectlyLinkedDevices() {
        ActionResult result = sim.perform(new FirewallRuleAction(office.pc1, office.db));
        assertFalse(result.success());
        assertEquals("PC-01 is not linked to DB-01", result.message());
        assertEquals(1500, sim.getMoney());
    }

    @Test
    void blockingSameLinkTwiceIsInvalidFromEitherEnd() {
        sim.perform(new FirewallRuleAction(office.server, office.db));
        ActionResult result = sim.perform(new FirewallRuleAction(office.db, office.server));
        assertFalse(result.success());
        assertEquals("That link is already blocked", result.message());
    }
}
