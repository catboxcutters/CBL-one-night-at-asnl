package nl.tue.sysadminpanic.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
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
import nl.tue.sysadminpanic.sim.GameEvent;
import nl.tue.sysadminpanic.sim.GameEventListener;

/**
 * The alert list, newest first, styled as a log being followed with {@code tail -f}.
 * Selecting an alert tells the main frame, which selects the alert's device.
 */
public class AlertPanel extends JPanel implements GameEventListener {

    private final DefaultListModel<Alert> alerts = new DefaultListModel<>();
    private final JList<Alert> list = new JList<>(alerts) {
        @Override
        public boolean getScrollableTracksViewportWidth() {
            // Always exactly as wide as the visible area, so long rows wrap
            // instead of making the list wider than the panel.
            return true;
        }
    };
    private final Consumer<Alert> onAlertSelected;
    private boolean inserting;

    /**
     * @param onAlertSelected called when the player selects an alert
     */
    public AlertPanel(Consumer<Alert> onAlertSelected) {
        super(new BorderLayout());
        this.onAlertSelected = onAlertSelected;
        setBorder(TerminalTheme.titledBorder("tail -f /var/log/alerts"));
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
        // JList caches row heights. Wrapped rows get taller or shorter when the width
        // changes, so clear that cache on resize (toggling the fixed height does that).
        list.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                list.setFixedCellHeight(1);
                list.setFixedCellHeight(-1);
            }
        });
        // No sideways scrolling: long summaries wrap onto the next line instead.
        add(new JScrollPane(list, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
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
     * Draws one alert row like a log line: {@code 00:04:00 [HIGH]     summary},
     * with the severity tag in its color. The selected row is shown in inverse video.
     *
     * <p>The row is a two-column HTML table as wide as the list: time and tag on the
     * left, the summary on the right. A long summary wraps inside its own column,
     * so continuation lines stay indented under the text instead of under the time.
     */
    private static class AlertRenderer extends DefaultListCellRenderer {

        // "[CRITICAL]" is the longest tag; padding to it keeps the summaries aligned.
        private static final int TAG_WIDTH = 10;
        private static final int PADDING = 6;
        // Before the first layout the list has no width yet; any small value works.
        private static final int MIN_WIDTH = 100;

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index,
                    isSelected, cellHasFocus);
            Alert alert = (Alert) value;
            // Inverse rows use one color; otherwise the tag has its severity color.
            Color tagColor = isSelected
                    ? list.getSelectionForeground()
                    : TerminalTheme.colorFor(alert.severity());
            int width = Math.max(MIN_WIDTH, list.getWidth() - 2 * PADDING);
            String tag = String.format("%-" + TAG_WIDTH + "s", "[" + alert.severity() + "]");
            label.setText("<html><table cellpadding='0' cellspacing='0' width='" + width + "'>"
                    + "<tr><td valign='top' nowrap>" + GameTime.format(alert.tick())
                    + "&nbsp;<font color='" + hex(tagColor) + "'>"
                    + tag.replace(" ", "&nbsp;") + "</font>&nbsp;</td>"
                    + "<td valign='top'>" + escape(alert.summary()) + "</td></tr></table></html>");
            label.setToolTipText(alert.summary());
            label.setBorder(BorderFactory.createEmptyBorder(3, PADDING, 3, PADDING));
            return label;
        }

        private static String hex(Color color) {
            return String.format("#%06X", color.getRGB() & 0xFFFFFF);
        }

        private static String escape(String text) {
            return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        }
    }
}
