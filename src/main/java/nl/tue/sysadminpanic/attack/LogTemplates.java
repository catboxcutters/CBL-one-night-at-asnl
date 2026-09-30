package nl.tue.sysadminpanic.attack;

/**
 * Fixed texts for the log lines and alert summaries that attacks produce.
 *
 * <p>Keeping all wording in one place makes the logs consistent and easy to
 * change, and keeps the stage classes focused on timing and behaviour.
 */
public final class LogTemplates {

    /** Ports probed by a port scan, in the order they are logged. */
    private static final int[] SCAN_PORTS = {
        21, 22, 23, 25, 53, 80, 110, 135, 139, 143, 443, 445, 1433, 3306, 3389
    };

    private LogTemplates() {
        // Utility class: never instantiated.
    }

    /**
     * A single dropped connection attempt during a port scan.
     *
     * @param sourceIp the scanning IP
     * @param probe    how many probes came before this one; selects the port
     * @return the log text
     */
    public static String portProbe(String sourceIp, int probe) {
        int port = SCAN_PORTS[probe % SCAN_PORTS.length];
        return "Dropped connection attempt from " + sourceIp + " to port " + port + "/tcp";
    }

    /**
     * @param sourceIp the attacker's IP
     * @return the log text for a failed password guess
     */
    public static String failedLogin(String sourceIp) {
        return "Failed login: admin from " + sourceIp;
    }

    /**
     * @param sourceIp the attacker's IP
     * @return the log text for a successful password guess
     */
    public static String successfulLogin(String sourceIp) {
        return "Successful login: admin from " + sourceIp;
    }

    /**
     * @param sourceIp the attacker's IP
     * @return the log text for when a brute force gives up
     */
    public static String loginAttemptsStopped(String sourceIp) {
        return "Login attempts from " + sourceIp + " stopped";
    }

    /**
     * @return the log text for the malware a phishing attachment starts
     */
    public static String suspiciousProcess() {
        return "Process started: invoice_0923.pdf.exe (unsigned) by user from Downloads";
    }

    /**
     * @param fromDeviceName the device the attacker came from
     * @return the log text for an attacker hopping onto this device
     */
    public static String inboundConnection(String fromDeviceName) {
        return "Inbound admin session from " + fromDeviceName + " (SMB)";
    }

    /**
     * @param destinationIp where the data is sent
     * @return the log text for one tick of data theft
     */
    public static String outboundTransfer(String destinationIp) {
        return "Large outbound transfer: 48 MB to " + destinationIp;
    }

    /**
     * @param sourceIp the scanning IP
     * @return the alert summary for a port scan
     */
    public static String portScanAlert(String sourceIp) {
        return "Port scan detected from " + sourceIp;
    }

    /**
     * @param deviceName the device under attack
     * @param sourceIp   the attacker's IP
     * @return the alert summary for a brute-force attack
     */
    public static String bruteForceAlert(String deviceName, String sourceIp) {
        return "Repeated failed logins on " + deviceName + " from " + sourceIp;
    }
}
