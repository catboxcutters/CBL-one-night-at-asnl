package nl.tue.sysadminpanic.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link NetworkHealth}, {@link GameTime} and {@link LogEntry} formatting.
 */
class NetworkHealthTest {

    private Device device(int criticality) {
        return new Device("D" + criticality, "D", DeviceType.SERVER, criticality, 0, 0);
    }

    @Test
    void healthyConnectedNetworkHasNoDrain() {
        assertEquals(0.0, NetworkHealth.drainPerTick(List.of(device(3), device(5)), 0));
    }

    @Test
    void compromisedDeviceDrainsPointOneTimesCriticality() {
        Device server = device(4);
        server.setTrueState(DeviceState.COMPROMISED);
        assertEquals(0.4, NetworkHealth.drainPerTick(List.of(server), 0), 1e-9);
    }

    @Test
    void onlyTrueStateCountsNotKnownState() {
        Device server = device(4);
        server.setKnownState(DeviceState.COMPROMISED);
        assertEquals(0.0, NetworkHealth.drainPerTick(List.of(server), 0));
    }

    @Test
    void isolatedDeviceDrainsPointZeroFiveTimesCriticality() {
        Device db = device(5);
        db.setIsolated(true);
        assertEquals(0.25, NetworkHealth.drainPerTick(List.of(db), 0), 1e-9);
    }

    @Test
    void compromisedAndIsolatedDevicePaysBothDrains() {
        Device db = device(5);
        db.setIsolated(true);
        db.setTrueState(DeviceState.COMPROMISED);
        assertEquals(0.75, NetworkHealth.drainPerTick(List.of(db), 0), 1e-9);
    }

    @Test
    void eachActiveExfiltrationDrainsOne() {
        assertEquals(2.0, NetworkHealth.drainPerTick(List.of(), 2), 1e-9);
    }

    @Test
    void healthNeverDropsBelowZero() {
        NetworkHealth health = new NetworkHealth();
        assertFalse(health.isDepleted());
        health.drain(150);
        assertEquals(0.0, health.getValue());
        assertTrue(health.isDepleted());
    }

    @Test
    void gameTimeStartsAtMidnightAndCountsThreeMinutesPerTick() {
        assertEquals("00:00:00", GameTime.format(0));
        assertEquals("00:03:00", GameTime.format(1));
        assertEquals("00:57:00", GameTime.format(19));
        assertEquals("01:00:00", GameTime.format(20));
        assertEquals("06:00:00", GameTime.format(120));
    }

    @Test
    void hourFormatRunsFromTwelveAmToSixAm() {
        assertEquals("12 AM", GameTime.formatHour(0));
        assertEquals("12 AM", GameTime.formatHour(19));
        assertEquals("1 AM", GameTime.formatHour(20));
        assertEquals("5 AM", GameTime.formatHour(119));
        assertEquals("6 AM", GameTime.formatHour(120));
        assertEquals("12 PM", GameTime.formatHour(240));
        assertEquals("1 PM", GameTime.formatHour(260));
    }

    @Test
    void logEntryFormatsWithClockTime() {
        LogEntry entry = new LogEntry(15, "SERVER-01", "Failed login: admin from 203.0.113.42");
        assertEquals("00:45:00  Failed login: admin from 203.0.113.42", entry.formatted());
    }
}
