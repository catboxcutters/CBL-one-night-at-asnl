package nl.tue.sysadminpanic.model;

/**
 * One immutable alert shown to the security team.
 *
 * @param id             unique, increasing alert number
 * @param type           what kind of activity was detected
 * @param sourceIp       the IP address the activity came from
 * @param targetDeviceId the id of the device the alert is about
 * @param severity       how serious the alert is
 * @param summary        one-line description for the alert list
 * @param tick           the simulation tick at which the alert was raised
 * @param falsePositive  true if the alert is harmless; never shown in the UI, so the
 *                       player has to find this out by inspecting the device
 */
public record Alert(
        int id,
        AlertType type,
        String sourceIp,
        String targetDeviceId,
        Severity severity,
        String summary,
        int tick,
        boolean falsePositive) {
}
