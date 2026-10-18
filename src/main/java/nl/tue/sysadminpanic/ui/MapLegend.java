package nl.tue.sysadminpanic.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import nl.tue.sysadminpanic.model.DeviceState;

/**
 * Draws the small key in the top-left corner of the network map.
 * Kept separate so {@link NetworkMapPanel} only deals with the network itself.
 */
class MapLegend {

    private static final int LEFT = 14;
    private static final int TOP = 14;
    private static final int ROW_HEIGHT = 20;
    private static final int WIDTH = 190;
    private static final int PADDING = 10;
    private static final int TEXT_LEFT = LEFT + PADDING + 44;

    /** Draws the legend onto the map's graphics. */
    void draw(Graphics2D g) {
        g.setFont(TerminalTheme.font(12f, false));
        int rows = DeviceState.values().length + 2;
        g.setColor(TerminalTheme.BACKGROUND);
        g.fillRect(LEFT, TOP, WIDTH, rows * ROW_HEIGHT + PADDING);
        g.setColor(TerminalTheme.BORDER);
        g.setStroke(new BasicStroke(1f));
        g.drawRect(LEFT, TOP, WIDTH, rows * ROW_HEIGHT + PADDING);

        int y = TOP + PADDING;
        for (DeviceState state : DeviceState.values()) {
            Color color = TerminalTheme.colorFor(state);
            g.setColor(color);
            g.draw(sampleBox(y));
            label(g, state.name().toLowerCase(), y, color);
            y += ROW_HEIGHT;
        }
        g.setColor(TerminalTheme.TEXT);
        g.setStroke(NetworkMapPanel.DASHED);
        g.draw(sampleBox(y));
        g.setStroke(new BasicStroke(1f));
        label(g, "isolated", y, TerminalTheme.TEXT);
        y += ROW_HEIGHT;
        NetworkMapPanel.drawFirewallMarker(g, LEFT + PADDING + 18, y + 6);
        label(g, "firewall rule", y, TerminalTheme.TEXT);
    }

    /** A small node-like box, the sample in front of each legend row. */
    private static Rectangle2D sampleBox(int rowTop) {
        return new Rectangle2D.Double(LEFT + PADDING + 4, rowTop, 28, 12);
    }

    private static void label(Graphics2D g, String text, int rowTop, Color color) {
        g.setColor(color);
        g.drawString(text, TEXT_LEFT, rowTop + 11);
    }
}
