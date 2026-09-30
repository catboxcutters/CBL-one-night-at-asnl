package nl.tue.sysadminpanic.ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import nl.tue.sysadminpanic.action.BlockIpAction;
import nl.tue.sysadminpanic.action.FirewallRuleAction;
import nl.tue.sysadminpanic.action.InspectAction;
import nl.tue.sysadminpanic.action.IsolateAction;
import nl.tue.sysadminpanic.action.PatchAction;
import nl.tue.sysadminpanic.action.PlayerAction;
import nl.tue.sysadminpanic.action.ReconnectAction;
import nl.tue.sysadminpanic.action.RestoreAction;
import nl.tue.sysadminpanic.model.Alert;
import nl.tue.sysadminpanic.model.Device;
import nl.tue.sysadminpanic.model.GameConstants;
import nl.tue.sysadminpanic.model.LogEntry;
import nl.tue.sysadminpanic.model.Wallet;
import nl.tue.sysadminpanic.sim.GameEvent;
import nl.tue.sysadminpanic.sim.GameEventListener;
import nl.tue.sysadminpanic.sim.Simulation;

/**
 * Shows the selected device (name, type, known state, revealed log) and the six
 * action buttons.
 *
 * <p>After every change the panel builds the action each button would perform
 * and asks the simulation whether it is allowed. A button is enabled only if it
 * is, and its tooltip gives the reason when it is not. The panel never decides
 * the rules itself; those live in the actions and the simulation.
 */
public class InspectorPanel extends JPanel implements GameEventListener {

    private static final String NO_DEVICE = "Select a device on the map";
    private static final String NO_ALERT = "Select an alert to block its source IP";
    private static final String NOT_INSPECTED =
            "Not inspected yet.\nUse Inspect (free) to read this device's log.";

    private final Simulation simulation;
    private final JLabel nameLabel = new JLabel(NO_DEVICE);
    private final JLabel detailLabel = new JLabel(" ");
    private final JTextArea logArea = new JTextArea();
    private final JButton inspectButton = button("Inspect", GameConstants.PRICE_INSPECT);
    private final JButton blockIpButton = button("Block IP", GameConstants.PRICE_BLOCK_IP);
    private final JButton isolateButton = button("Isolate", GameConstants.PRICE_ISOLATE);
    private final JButton patchButton = button("Patch", GameConstants.PRICE_PATCH);
    private final JButton firewallButton = button("Firewall rule",
            GameConstants.PRICE_FIREWALL_RULE);
    private final JButton restoreButton = button("Restore", GameConstants.PRICE_RESTORE);
    /** The action each simple button performs when clicked; null means "no target". */
    private final Map<JButton, PlayerAction> currentActions = new HashMap<>();

    private Device device;
    private Alert alert;

    /**
     * @param simulation the game, used to check and perform actions
     */
    public InspectorPanel(Simulation simulation) {
        super(new BorderLayout(0, 8));
        this.simulation = simulation;
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Inspector"),
                BorderFactory.createEmptyBorder(4, 6, 6, 6)));
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        // Wrap long lines so the start of every line stays visible.
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        add(buildHeader(), BorderLayout.NORTH);
        add(new JScrollPane(logArea), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);
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

    private JPanel buildButtons() {
        JPanel buttons = new JPanel(new GridLayout(3, 2, 6, 6));
        for (JButton button : List.of(inspectButton, blockIpButton, isolateButton,
                patchButton, restoreButton)) {
            button.addActionListener(event -> performCurrentAction(button));
        }
        firewallButton.addActionListener(event -> chooseLinkAndBlock());
        buttons.add(inspectButton);
        buttons.add(blockIpButton);
        buttons.add(isolateButton);
        buttons.add(patchButton);
        buttons.add(firewallButton);
        buttons.add(restoreButton);
        return buttons;
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

    /**
     * Remembers the selected alert; its source IP is what Block IP blocks.
     *
     * @param newAlert the selected alert, or null for none
     */
    public void setAlert(Alert newAlert) {
        alert = newAlert;
        refresh();
    }

    @Override
    public void onEvent(GameEvent event) {
        // Money, states and logs change constantly, so re-check everything.
        refresh();
    }

    private void refresh() {
        showHeader();
        showLog();
        refreshButtons();
    }

    private void showHeader() {
        if (device == null) {
            nameLabel.setText(NO_DEVICE);
            detailLabel.setText(" ");
            return;
        }
        nameLabel.setText(device.getName());
        String details = capitalize(device.getType().name()) + "  ·  Known state: "
                + capitalize(device.getKnownState().name());
        if (device.isPatched()) {
            details += "  ·  patched";
        }
        if (device.isIsolated()) {
            details += "  ·  isolated";
        }
        detailLabel.setText(details);
    }

    private void showLog() {
        String text;
        if (device == null) {
            text = "";
        } else if (!device.isInspected()) {
            text = NOT_INSPECTED;
        } else {
            text = device.getRevealedLog().stream().map(LogEntry::formatted)
                    .collect(Collectors.joining("\n"));
        }
        // Only replace the text when it changed, so the scroll position is kept.
        if (!text.equals(logArea.getText())) {
            logArea.setText(text);
            logArea.setCaretPosition(text.length());
        }
    }

    private void refreshButtons() {
        boolean hasDevice = device != null;
        offer(inspectButton, hasDevice ? new InspectAction(device) : null, NO_DEVICE,
                "Reveal the device's true state and its log so far");
        String ip = alert != null ? alert.sourceIp() : null;
        offer(blockIpButton, ip != null ? new BlockIpAction(ip) : null, NO_ALERT,
                "Block " + ip + " (the source of the selected alert)");
        refreshIsolateButton();
        offer(patchButton, hasDevice ? new PatchAction(device) : null, NO_DEVICE,
                "Immune to brute force; attackers need twice as long to get in from a neighbor");
        offer(restoreButton, hasDevice ? new RestoreAction(device) : null, NO_DEVICE,
                "Reinstall from backup: healthy again, attackers lose this foothold");
        refreshFirewallButton();
    }

    private void refreshIsolateButton() {
        if (device != null && device.isIsolated()) {
            isolateButton.setText(label("Reconnect", GameConstants.PRICE_RECONNECT));
            offer(isolateButton, new ReconnectAction(device), NO_DEVICE,
                    "Plug the device back into the network");
        } else {
            isolateButton.setText(label("Isolate", GameConstants.PRICE_ISOLATE));
            offer(isolateButton, device != null ? new IsolateAction(device) : null, NO_DEVICE,
                    "Cut all links; staff can't work, so it drains health while isolated");
        }
    }

    /** Enables a button if its action is allowed, and explains why not otherwise. */
    private void offer(JButton button, PlayerAction action, String noTargetReason,
            String description) {
        currentActions.put(button, action);
        Optional<String> problem = action == null
                ? Optional.of(noTargetReason)
                : simulation.checkAllowed(action);
        button.setEnabled(problem.isEmpty());
        button.setToolTipText(problem.orElse(description));
    }

    private void performCurrentAction(JButton button) {
        PlayerAction action = currentActions.get(button);
        if (action != null) {
            simulation.perform(action);
        }
    }

    // --- Firewall rule: needs a second choice, the neighbor ---

    private void refreshFirewallButton() {
        if (device == null) {
            firewallButton.setEnabled(false);
            firewallButton.setToolTipText(NO_DEVICE);
            return;
        }
        boolean possible = !blockableNeighbors(device).isEmpty();
        firewallButton.setEnabled(possible);
        firewallButton.setToolTipText(possible
                ? "Block one link of this device; both ends keep working"
                : firewallProblem(device));
    }

    private List<Device> blockableNeighbors(Device source) {
        List<Device> result = new ArrayList<>();
        for (Device neighbor : simulation.getNetwork().neighbors(source)) {
            if (simulation.checkAllowed(new FirewallRuleAction(source, neighbor)).isEmpty()) {
                result.add(neighbor);
            }
        }
        return result;
    }

    /** Why no link of this device can be blocked, taken from its first neighbor. */
    private String firewallProblem(Device source) {
        Device neighbor = simulation.getNetwork().neighbors(source).get(0);
        return simulation.checkAllowed(new FirewallRuleAction(source, neighbor))
                .orElse("No link can be blocked");
    }

    private void chooseLinkAndBlock() {
        // Remember the device now: the selection may change while the dialog is open.
        Device source = device;
        List<Device> options = source == null ? List.of() : blockableNeighbors(source);
        if (options.isEmpty()) {
            return;
        }
        Object choice = JOptionPane.showInputDialog(this,
                "Block the link from " + source.getName() + " to:", "Firewall rule",
                JOptionPane.QUESTION_MESSAGE, null, options.toArray(), options.get(0));
        if (choice instanceof Device neighbor) {
            simulation.perform(new FirewallRuleAction(source, neighbor));
        }
    }

    // --- Helpers ---

    private static JButton button(String name, int price) {
        return new JButton(label(name, price));
    }

    /** Button text such as "Isolate · $300" or "Inspect · free". */
    private static String label(String name, int price) {
        return name + " · " + (price == 0 ? "free" : Wallet.format(price));
    }

    private static String capitalize(String enumName) {
        String lower = enumName.toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
