package nl.tue.sysadminpanic.model;

/**
 * The kind of suspicious activity an alert reports.
 */
public enum AlertType {
    PORT_SCAN("Port scan"),
    BRUTE_FORCE("Brute force"),
    UNUSUAL_OUTBOUND("Unusual outbound connection");

    private final String label;

    AlertType(String label) {
        this.label = label;
    }

    /** @return a short human-readable name for display */
    public String getLabel() {
        return label;
    }
}
