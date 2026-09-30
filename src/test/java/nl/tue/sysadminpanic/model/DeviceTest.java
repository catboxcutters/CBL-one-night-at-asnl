package nl.tue.sysadminpanic.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Device} and {@link Link}.
 */
class DeviceTest {

    private Device pc(String id) {
        return new Device(id, id, DeviceType.WORKSTATION, 1, 0.2, 0.8);
    }

    @Test
    void newDeviceIsHealthyUnpatchedAndConnected() {
        Device device = pc("PC-01");
        assertEquals(DeviceState.HEALTHY, device.getTrueState());
        assertEquals(DeviceState.HEALTHY, device.getKnownState());
        assertFalse(device.isPatched());
        assertFalse(device.isIsolated());
    }

    @Test
    void criticalityOutsideOneToFiveIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Device("X", "X", DeviceType.SERVER, 0, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new Device("X", "X", DeviceType.SERVER, 6, 0, 0));
    }

    @Test
    void positionOutsideUnitSquareIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Device("X", "X", DeviceType.SERVER, 1, 1.5, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new Device("X", "X", DeviceType.SERVER, 1, 0, -0.1));
    }

    @Test
    void logKeepsEntriesOldestFirst() {
        Device device = pc("PC-01");
        device.addLog(new LogEntry(1, "PC-01", "first"));
        device.addLog(new LogEntry(2, "PC-01", "second"));
        assertEquals(List.of("first", "second"),
                device.getLog().stream().map(LogEntry::text).toList());
    }

    @Test
    void logDropsOldestEntryWhenFull() {
        Device device = pc("PC-01");
        int total = GameConstants.MAX_LOG_ENTRIES + 3;
        for (int tick = 0; tick < total; tick++) {
            device.addLog(new LogEntry(tick, "PC-01", "line " + tick));
        }
        List<LogEntry> log = device.getLog();
        assertEquals(GameConstants.MAX_LOG_ENTRIES, log.size());
        assertEquals(3, log.get(0).tick());
        assertEquals(total - 1, log.get(log.size() - 1).tick());
    }

    @Test
    void linkOtherReturnsOppositeEnd() {
        Device a = pc("A");
        Device b = pc("B");
        Link link = new Link(a, b);
        assertEquals(b, link.other(a));
        assertEquals(a, link.other(b));
        assertThrows(IllegalArgumentException.class, () -> link.other(pc("C")));
    }
}
