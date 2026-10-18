package nl.tue.sysadminpanic.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.function.Consumer;
import javax.swing.JPanel;
import nl.tue.sysadminpanic.model.Device;
import nl.tue.sysadminpanic.model.DeviceType;
import nl.tue.sysadminpanic.model.Link;
import nl.tue.sysadminpanic.model.Network;
import nl.tue.sysadminpanic.sim.GameEvent;
import nl.tue.sysadminpanic.sim.GameEventListener;

/**
 * The clickable network map, drawn in the terminal style.
 *
 * <p>Devices are drawn at their relative (0..1) positions scaled to the current
 * panel size, so the map follows the window when it is resized. Colors show the
 * <em>known</em> state only: the map never reveals more than the security team knows.
 */
public class NetworkMapPanel extends JPanel implements GameEventListener {

    private static final int NODE_WIDTH = 110;
    private static final int NODE_HEIGHT = 36;
    private static final int NODE_ARC = 4;
    // Keeps nodes at the edges (x or y of 0 or 1) fully inside the panel.
    private static final int MARGIN_X = NODE_WIDTH / 2 + 16;
    private static final int MARGIN_Y = NODE_HEIGHT / 2 + 16;
    private static final String FIREWALL_MARKER = "[fw]";

    static final Stroke DASHED = new BasicStroke(2f, BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_MITER, 10f, new float[] {6f, 4f}, 0f);
    private static final Stroke SOLID = new BasicStroke(2f);
    private static final Stroke THIN = new BasicStroke(1f);

    private final Network network;
    private final MapLegend legend = new MapLegend();
    private Device selected;

    /**
     * @param network          the network to draw
     * @param onDeviceSelected called with the device the player clicks on
     */
    public NetworkMapPanel(Network network, Consumer<Device> onDeviceSelected) {
        this.network = network;
        setBackground(TerminalTheme.BACKGROUND);
        setPreferredSize(new Dimension(760, 560));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                Device device = deviceAt(event.getPoint());
                if (device != null) {
                    onDeviceSelected.accept(device);
                }
            }
        });
    }

    /**
     * Highlights a device as selected.
     *
     * @param device the selected device, or null for none
     */
    public void setSelectedDevice(Device device) {
        selected = device;
        repaint();
    }

    @Override
    public void onEvent(GameEvent event) {
        // Any event may change what the map shows, and a repaint is cheap.
        repaint();
    }

    private Device deviceAt(Point point) {
        for (Device device : network.getDevices()) {
            if (nodeShape(device).contains(point)) {
                return device;
            }
        }
        return null;
    }

    /** Converts a device's relative position to pixel coordinates of its center. */
    private Point2D center(Device device) {
        double x = MARGIN_X + device.getX() * (getWidth() - 2 * MARGIN_X);
        double y = MARGIN_Y + device.getY() * (getHeight() - 2 * MARGIN_Y);
        return new Point2D.Double(x, y);
    }

    private RoundRectangle2D nodeShape(Device device) {
        Point2D c = center(device);
        return new RoundRectangle2D.Double(c.getX() - NODE_WIDTH / 2.0,
                c.getY() - NODE_HEIGHT / 2.0, NODE_WIDTH, NODE_HEIGHT, NODE_ARC, NODE_ARC);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(TerminalTheme.font(13f, true));
        for (Link link : network.getLinks()) {
            drawLink(g, link);
        }
        for (Device device : network.getDevices()) {
            drawNode(g, device);
        }
        legend.draw(g);
        g.dispose();
    }

    private void drawLink(Graphics2D g, Link link) {
        Point2D a = center(link.getFirst());
        Point2D b = center(link.getSecond());
        if (link.getFirst().isIsolated() || link.getSecond().isIsolated()) {
            drawBrokenLine(g, a, b);
        } else {
            g.setColor(TerminalTheme.DIM);
            g.setStroke(SOLID);
            g.draw(new Line2D.Double(a, b));
        }
        if (link.isBlocked()) {
            drawFirewallMarker(g, (a.getX() + b.getX()) / 2, (a.getY() + b.getY()) / 2);
        }
    }

    /** Draws a red line with a gap in the middle: the cable is unplugged. */
    private void drawBrokenLine(Graphics2D g, Point2D a, Point2D b) {
        g.setColor(TerminalTheme.RED);
        g.setStroke(SOLID);
        g.draw(new Line2D.Double(a, pointBetween(a, b, 0.4)));
        g.draw(new Line2D.Double(pointBetween(a, b, 0.6), b));
    }

    private static Point2D pointBetween(Point2D a, Point2D b, double fraction) {
        return new Point2D.Double(a.getX() + (b.getX() - a.getX()) * fraction,
                a.getY() + (b.getY() - a.getY()) * fraction);
    }

    /**
     * Draws the firewall-rule marker, a small cyan "[fw]" box, centered on (x, y).
     * Uses the font currently set on {@code g}.
     */
    static void drawFirewallMarker(Graphics2D g, double x, double y) {
        FontMetrics metrics = g.getFontMetrics();
        int width = metrics.stringWidth(FIREWALL_MARKER) + 6;
        int height = metrics.getHeight();
        Rectangle2D box = new Rectangle2D.Double(x - width / 2.0, y - height / 2.0,
                width, height);
        g.setColor(TerminalTheme.BACKGROUND);
        g.fill(box);
        g.setColor(TerminalTheme.CYAN);
        g.setStroke(THIN);
        g.draw(box);
        drawCentered(g, FIREWALL_MARKER, box.getCenterX(), box.getCenterY());
    }

    private void drawNode(Graphics2D g, Device device) {
        RoundRectangle2D shape = nodeShape(device);
        Color color = colorFor(device);
        // The selected node is drawn in inverse video, like a highlighted terminal line.
        boolean inverse = device == selected;
        g.setColor(inverse ? color : TerminalTheme.BACKGROUND);
        g.fill(shape);
        g.setColor(color);
        g.setStroke(device.isIsolated() ? DASHED : SOLID);
        g.draw(shape);
        g.setColor(inverse ? TerminalTheme.BACKGROUND : color);
        drawCentered(g, device.getName(), shape.getCenterX(), shape.getCenterY());
    }

    private static void drawCentered(Graphics2D g, String text, double centerX, double centerY) {
        FontMetrics metrics = g.getFontMetrics();
        int textX = (int) (centerX - metrics.stringWidth(text) / 2.0);
        int textY = (int) (centerY + (metrics.getAscent() - metrics.getDescent()) / 2.0);
        g.drawString(text, textX, textY);
    }

    private static Color colorFor(Device device) {
        if (device.getType() == DeviceType.INTERNET) {
            return TerminalTheme.DIM;
        }
        return TerminalTheme.colorFor(device.getKnownState());
    }
}
