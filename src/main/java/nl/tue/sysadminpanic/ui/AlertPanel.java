package nl.tue.sysadminpanic.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import nl.tue.sysadminpanic.model.Alert;
import nl.tue.sysadminpanic.model.GameTime;
import nl.tue.sysadminpanic.model.Severity;
import nl.tue.sysadminpanic.sim.GameEvent;
import nl.tue.sysadminpanic.sim.GameEventListener;

/**
 * The alert list, newest first, with a severity-colored stripe on each row.
 * Selecting an alert tells the main frame, which selects the alert's device.
 */
public class AlertPanel extends JPanel implements GameEventListener {

    private final DefaultListModel<Alert> alerts = new DefaultListModel<>();
    private final JList<Alert> list = new JList<>(alerts);
    private final Consumer<Alert> onAlertSelected;
    private boolean inserting;

    /**
     * @param onAlertSelected called when the player selects an alert
     */
    public AlertPanel(Consumer<Alert> onAlertSelected) {
        super(new BorderLayout());
        this.onAlertSelected = onAlertSelected;
        setBorder(BorderFactory.createTitledBorder("Alerts"));
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new AlertRenderer());
        list.addListSelectionListener(event -> {
            // Adding a new alert at the top shifts the selected row, which Swing
            // reports as a selection change; that is not the player choosing.
            if (!inserting && !event.getValueIsAdjusting()) {
                reportSelection();
            }
        });
        // Clicking the already-selected alert fires no selection change, but the
        // player still expects its device to be selected again.
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                reportSelection();
            }
        });
        add(new JScrollPane(list), BorderLayout.CENTER);
    }

    private void reportSelection() {
        Alert alert = list.getSelectedValue();
        if (alert != null) {
            onAlertSelected.accept(alert);
        }
    }

    @Override
    public void onEvent(GameEvent event) {
        if (event instanceof GameEvent.AlertRaised raised) {
            inserting = true;
            alerts.add(0, raised.alert());
            inserting = false;
        }
    }

    /**
     * @param severity an alert severity
     * @return the color used to mark it
     */
    static Color colorFor(Severity severity) {
        return switch (severity) {
            case LOW -> new Color(100, 140, 190);
            case MEDIUM -> new Color(230, 168, 23);
            case HIGH -> new Color(235, 110, 40);
            case CRITICAL -> new Color(214, 45, 45);
        };
    }

    /** Draws one alert row: time, severity and summary, with a colored left stripe. */
    private static class AlertRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index,
                    isSelected, cellHasFocus);
            Alert alert = (Alert) value;
            label.setText(GameTime.format(alert.tick()) + "  " + alert.severity() + "  "
                    + alert.summary());
            label.setToolTipText(alert.summary());
            label.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 6, 0, 0, colorFor(alert.severity())),
                    BorderFactory.createEmptyBorder(4, 8, 4, 4)));
            return label;
        }
    }
}
