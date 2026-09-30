package nl.tue.sysadminpanic.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import nl.tue.sysadminpanic.action.ActionResult;
import nl.tue.sysadminpanic.action.InspectAction;
import nl.tue.sysadminpanic.action.IsolateAction;
import nl.tue.sysadminpanic.action.PatchAction;
import org.junit.jupiter.api.Test;

/**
 * Tests for income, prices and the checks every action goes through.
 */
class MoneyAndActionFlowTest extends ActionTestBase {

    @Test
    void playerStartsWithFifteenHundredDollars() {
        assertEquals(1500, sim.getMoney());
    }

    @Test
    void incomeIsFiftyDollarsEveryFiveTicks() {
        office.runUntil(4);
        assertEquals(1500, sim.getMoney());
        office.runUntil(5);
        assertEquals(1550, sim.getMoney());
        office.runUntil(10);
        assertEquals(1600, sim.getMoney());
        assertEquals(List.of(new GameEvent.MoneyChanged(1550), new GameEvent.MoneyChanged(1600)),
                eventsOfType(GameEvent.MoneyChanged.class));
    }

    @Test
    void actionChargesItsPriceAndPublishesNewBalance() {
        sim.perform(new IsolateAction(office.pc1));
        assertEquals(1200, sim.getMoney());
        assertEquals(300, sim.getMoneySpent());
        assertEquals(List.of(new GameEvent.MoneyChanged(1200)),
                eventsOfType(GameEvent.MoneyChanged.class));
    }

    @Test
    void unaffordableActionFailsWithReasonAndHasNoEffect() {
        sim.perform(new PatchAction(office.server));
        ActionResult result = sim.perform(new PatchAction(office.db));
        assertFalse(result.success());
        assertEquals("Costs $800, you have $700", result.message());
        assertFalse(office.db.isPatched());
        assertEquals(700, sim.getMoney());
    }

    @Test
    void checkAllowedGivesTooltipReasonWithoutDoingAnything() {
        sim.perform(new PatchAction(office.server));
        Optional<String> reason = sim.checkAllowed(new PatchAction(office.db));
        assertEquals(Optional.of("Costs $800, you have $700"), reason);
        assertEquals(Optional.empty(), sim.checkAllowed(new IsolateAction(office.db)));
        assertEquals(700, sim.getMoney());
    }

    @Test
    void invalidityIsReportedBeforeMoney() {
        sim.perform(new PatchAction(office.server));
        ActionResult result = sim.perform(new PatchAction(office.server));
        assertEquals("SERVER-01 is already patched", result.message());
    }

    @Test
    void everyAttemptPublishesActionPerformed() {
        ActionResult ok = sim.perform(new IsolateAction(office.pc1));
        ActionResult failed = sim.perform(new IsolateAction(office.pc1));
        assertTrue(ok.success());
        assertFalse(failed.success());
        assertEquals(List.of(new GameEvent.ActionPerformed(ok), new GameEvent.ActionPerformed(failed)),
                eventsOfType(GameEvent.ActionPerformed.class));
    }

    @Test
    void actionsWorkBetweenTicksWithoutAdvancingTime() {
        office.runUntil(7);
        sim.perform(new IsolateAction(office.pc1));
        assertEquals(7, sim.getCurrentTick());
        assertTrue(office.pc1.isIsolated());
    }

    @Test
    void internetCannotBeTargeted() {
        ActionResult result = sim.perform(new InspectAction(office.internet));
        assertFalse(result.success());
        assertEquals("The internet is outside our network", result.message());
    }

    @Test
    void actionsAreRefusedAfterGameOver() {
        startPhishing(office.pc2);
        office.runUntil(200);
        assertTrue(sim.isGameOver());
        ActionResult result = sim.perform(new InspectAction(office.pc1));
        assertEquals(ActionResult.failure("The game is over"), result);
    }
}
