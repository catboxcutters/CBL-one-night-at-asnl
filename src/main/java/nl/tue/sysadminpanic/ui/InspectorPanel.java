package nl.tue.sysadminpanic.ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import nl.tue.sysadminpanic.model.Device;
import nl.tue.sysadminpanic.model.DeviceType;
import nl.tue.sysadminpanic.model.LogEntry;

/**
 * Shows the selected device (name, type, known state, revealed log) and the
 * Inspect button.
 */
public class InspectorPanel extends JPanel {

    private static final String NO_DEVICE = "Select a device on the map";
    private static final String NOT_INSPECTED =
            "Not inspected yet.\nUse Inspect to read this device's log.";

    private final int currentTick;
    private final Runnable onInspected;
    private final JLabel nameLabel = new JLabel(NO_DEVICE);
    private final JLabel detailLabel = new JLabel(" ");
    private final JTextArea logArea = new JTextArea();
    private final JButton inspectButton = new JButton("Inspect");

    private Device device;

    /**
     * @param currentTick the tick that inspecting counts as; logs up to it are revealed
     * @param onInspected called after a device was inspected, so the map can repaint
     */
    public InspectorPanel(int currentTick, Runnable onInspected) {
        super(new BorderLayout(0, 8));
        this.currentTick = currentTick;
        this.onInspected = onInspected;
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Inspector"),
                BorderFactory.createEmptyBorder(4, 6, 6, 6)));
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        // Wrap long lines so the start of every line stays visible.
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        inspectButton.addActionListener(event -> inspect());
        add(buildHeader(), BorderLayout.NORTH);
        add(new JScrollPane(logArea), BorderLayout.CENTER);
        add(inspectButton, BorderLayout.SOUTH);
        refresh();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 18f));
        header.add(nameLabel);
        header.add(detailLabel);
        return header;
    }

    /**
     * Shows a different device.
     *
     * @param newDevice the selected device, or null for none
     */
    public void setDevice(Device newDevice) {
        device = newDevice;
        refresh();
    }

    /** Reveals the true state and the log so far: the security team takes a close look. */
    private void inspect() {
        device.setKnownState(device.getTrueState());
        device.markInspected(currentTick);
        refresh();
        onInspected.run();
    }

    private void refresh() {
        showHeader();
        showLog();
        refreshButton();
    }

    private void showHeader() {
        if (device == null) {
            nameLabel.setText(NO_DEVICE);
            detailLabel.setText(" ");
            return;
        }
        nameLabel.setText(device.getName());
        detailLabel.setText(capitalize(device.getType().name()) + "  ·  Known state: "
                + capitalize(device.getKnownState().name()));
    }

    private void showLog() {
        if (device == null) {
            logArea.setText("");
        } else if (!device.isInspected()) {
            logArea.setText(NOT_INSPECTED);
        } else if (device.getRevealedLog().isEmpty()) {
            logArea.setText("(log is empty)");
        } else {
            logArea.setText(device.getRevealedLog().stream().map(LogEntry::formatted)
                    .collect(Collectors.joining("\n")));
        }
    }

    private void refreshButton() {
        // The INTERNET is outside our network, so there is nothing to inspect there.
        boolean possible = device != null && device.getType() != DeviceType.INTERNET;
        inspectButton.setEnabled(possible);
        if (device == null) {
            inspectButton.setToolTipText(NO_DEVICE);
        } else if (!possible) {
            inspectButton.setToolTipText("The internet is outside our network");
        } else {
            inspectButton.setToolTipText("Reveal the device's true state and its log so far");
        }
    }

    private static String capitalize(String enumName) {
        String lower = enumName.toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
