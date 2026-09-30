package nl.tue.sysadminpanic.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import nl.tue.sysadminpanic.action.ActionResult;
import nl.tue.sysadminpanic.action.InspectAction;
import nl.tue.sysadminpanic.model.AlertType;
import nl.tue.sysadminpanic.model.DeviceState;
import nl.tue.sysadminpanic.model.Severity;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link InspectAction}.
 */
class InspectActionTest extends ActionTestBase {

    @Test
    void inspectIsFree() {
        sim.perform(new InspectAction(office.pc1));
        assertEquals(1500, sim.getMoney());
    }

    @Test
    void inspectRevealsSilentCompromise() {
        startPhishing(office.pc2);
        office.runUntil(3);
        assertEquals(DeviceState.HEALTHY, office.pc2.getKnownState());
        ActionResult result = sim.perform(new InspectAction(office.pc2));
        assertEquals(DeviceState.COMPROMISED, office.pc2.getKnownState());
        assertEquals("PC-02 inspected: compromised", result.message());
    }

    @Test
    void inspectClearsFalsePositiveSuspicion() {
        sim.raiseAlert(AlertType.PORT_SCAN, IP, office.pc1, Severity.LOW, "scanner", true);
        assertEquals(DeviceState.SUSPICIOUS, office.pc1.getKnownState());
        sim.perform(new InspectAction(office.pc1));
        assertEquals(DeviceState.HEALTHY, office.pc1.getKnownState());
    }

    @Test
    void logIsHiddenUntilInspected() {
        startPhishing(office.pc2);
        office.runUntil(3);
        assertFalse(office.pc2.isInspected());
        assertTrue(office.pc2.getRevealedLog().isEmpty());
        sim.perform(new InspectAction(office.pc2));
        assertTrue(office.pc2.isInspected());
        assertEquals(1, office.pc2.getRevealedLog().size());
    }

    @Test
    void revealedLogIsSnapshotUntilNextInspection() {
        startBruteForce(office.server);
        office.runUntil(5);
        sim.perform(new InspectAction(office.server));
        office.runUntil(10);
        assertEquals(5, office.server.getRevealedLog().size());
        assertEquals(10, office.server.getLog().size());
        sim.perform(new InspectAction(office.server));
        assertEquals(10, office.server.getRevealedLog().size());
    }

    @Test
    void inspectPublishesDeviceStateChanged() {
        sim.perform(new InspectAction(office.pc1));
        assertEquals(List.of(new GameEvent.DeviceStateChanged(office.pc1)),
                eventsOfType(GameEvent.DeviceStateChanged.class));
    }
}
