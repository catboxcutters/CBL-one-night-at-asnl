package nl.tue.sysadminpanic.model;

/**
 * The security state of a device.
 *
 * <p>Isolation is deliberately not a state: it is a separate flag on {@link Device},
 * so an isolated device keeps its real state.
 */
public enum DeviceState {
    HEALTHY,
    SUSPICIOUS,
    COMPROMISED
}
