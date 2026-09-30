package nl.tue.sysadminpanic.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Draws the small key in the top-left corner of the network map.
 * Kept separate so {@link NetworkMapPanel} only deals with the network itself.
 */
class MapLegend {

    private static final int LEFT = 14;
    private static final int TOP = 14;
    private static final int ROW_HEIGHT = 20;
    private static final int WIDTH = 150;
    private static final int PADDING = 10;
    private static final Color PANEL = new Color(20, 26, 34, 220);

    private static final String[] STATE_LABELS = {"Healthy", "Suspicious", "Compromised"};
    private static final Color[] STATE_COLORS = {
        NetworkMapPanel.HEALTHY, NetworkMapPanel.SUSPICIOUS, NetworkMapPanel.COMPROMISED
    };

    /** Draws the legend onto the map's graphics. */
    void draw(Graphics2D g) {
        int rows = STATE_LABELS.length + 2;
        g.setColor(PANEL);
        g.fillRoundRect(LEFT, TOP, WIDTH, rows * ROW_HEIGHT + PADDING, 10, 10);
        g.setFont(g.getFont().deriveFont(Font.PLAIN, 12f));
        int y = TOP + PADDING;
        for (int i = 0; i < STATE_LABELS.length; i++) {
            g.setColor(STATE_COLORS[i]);
            g.fillRoundRect(LEFT + PADDING, y, 14, 12, 4, 4);
            label(g, STATE_LABELS[i], y);
            y += ROW_HEIGHT;
        }
        drawIsolatedSample(g, y);
        label(g, "Isolated", y);
        y += ROW_HEIGHT;
        NetworkMapPanel.drawShield(g, LEFT + PADDING + 7, y + 5);
        label(g, "Firewall rule", y);
    }

    private void drawIsolatedSample(Graphics2D g, int y) {
        g.setColor(NetworkMapPanel.LIGHT_TEXT);
        g.setStroke(NetworkMapPanel.DASHED);
        g.draw(new RoundRectangle2D.Double(LEFT + PADDING, y, 14, 12, 4, 4));
        g.setStroke(new BasicStroke(1f));
    }

    private void label(Graphics2D g, String text, int rowTop) {
        g.setColor(NetworkMapPanel.LIGHT_TEXT);
        g.drawString(text, LEFT + PADDING + 24, rowTop + 11);
    }
}
