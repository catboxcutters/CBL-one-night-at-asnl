package nl.tue.sysadminpanic.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.util.function.Consumer;
import javax.swing.JPanel;
import nl.tue.sysadminpanic.model.Device;
import nl.tue.sysadminpanic.model.DeviceType;
import nl.tue.sysadminpanic.model.Link;
import nl.tue.sysadminpanic.model.Network;

/**
 * The clickable network map.
 *
 * <p>Devices are drawn at their relative (0..1) positions scaled to the current
 * panel size, so the map follows the window when it is resized. Colors show the
 * <em>known</em> state only: the map never reveals more than the security team knows.
 */
public class NetworkMapPanel extends JPanel {

    private static final int NODE_WIDTH = 104;
    private static final int NODE_HEIGHT = 40;
    private static final int NODE_ARC = 14;
    // Keeps nodes at the edges (x or y of 0 or 1) fully inside the panel.
    private static final int MARGIN_X = NODE_WIDTH / 2 + 16;
    private static final int MARGIN_Y = NODE_HEIGHT / 2 + 16;

    static final Color BACKGROUND = new Color(30, 37, 48);
    static final Color HEALTHY = new Color(46, 157, 87);
    static final Color SUSPICIOUS = new Color(230, 168, 23);
    static final Color COMPROMISED = new Color(214, 69, 65);
    static final Color INTERNET = new Color(128, 136, 148);
    static final Color LINK = new Color(107, 118, 134);
    static final Color BROKEN_LINK = new Color(214, 69, 65);
    static final Color SHIELD = new Color(66, 133, 244);
    static final Color SELECTION = new Color(120, 200, 255);
    static final Color LIGHT_TEXT = new Color(222, 228, 236);
    private static final Color DARK_TEXT = new Color(30, 30, 30);

    static final Stroke DASHED = new BasicStroke(2f, BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_MITER, 10f, new float[] {6f, 4f}, 0f);
    private static final Stroke SOLID = new BasicStroke(2f);
    private static final Stroke SELECTION_STROKE = new BasicStroke(3f);

    private final Network network;
    private final MapLegend legend = new MapLegend();
    private Device selected;

    /**
     * @param network          the network to draw
     * @param onDeviceSelected called with the device the player clicks on
     */
    public NetworkMapPanel(Network network, Consumer<Device> onDeviceSelected) {
        this.network = network;
        setBackground(BACKGROUND);
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

    /** Redraws the map, for example after a device's known state changed. */
    public void refresh() {
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
            g.setColor(LINK);
            g.setStroke(SOLID);
            g.draw(new Line2D.Double(a, b));
        }
        if (link.isBlocked()) {
            drawShield(g, (a.getX() + b.getX()) / 2, (a.getY() + b.getY()) / 2);
        }
    }

    /** Draws a red line with a gap in the middle: the cable is unplugged. */
    private void drawBrokenLine(Graphics2D g, Point2D a, Point2D b) {
        g.setColor(BROKEN_LINK);
        g.setStroke(SOLID);
        g.draw(new Line2D.Double(a, pointBetween(a, b, 0.4)));
        g.draw(new Line2D.Double(pointBetween(a, b, 0.6), b));
    }

    private static Point2D pointBetween(Point2D a, Point2D b, double fraction) {
        return new Point2D.Double(a.getX() + (b.getX() - a.getX()) * fraction,
                a.getY() + (b.getY() - a.getY()) * fraction);
    }

    /** Draws a small shield centered on (x, y): the marker for a firewall rule. */
    static void drawShield(Graphics2D g, double x, double y) {
        Path2D shield = new Path2D.Double();
        shield.moveTo(x - 8, y - 9);
        shield.lineTo(x + 8, y - 9);
        shield.lineTo(x + 8, y);
        shield.quadTo(x + 8, y + 7, x, y + 11);
        shield.quadTo(x - 8, y + 7, x - 8, y);
        shield.closePath();
        g.setColor(SHIELD);
        g.fill(shield);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(1.5f));
        g.draw(shield);
    }

    private void drawNode(Graphics2D g, Device device) {
        RoundRectangle2D shape = nodeShape(device);
        Color fill = fillFor(device);
        g.setColor(fill);
        g.fill(shape);
        // Isolated devices get a light dashed border: "unplugged".
        g.setColor(device.isIsolated() ? LIGHT_TEXT : fill.darker());
        g.setStroke(device.isIsolated() ? DASHED : SOLID);
        g.draw(shape);
        if (device == selected) {
            drawSelection(g, shape);
        }
        drawName(g, device, shape, fill == SUSPICIOUS ? DARK_TEXT : Color.WHITE);
    }

    private void drawSelection(Graphics2D g, RoundRectangle2D shape) {
        g.setColor(SELECTION);
        g.setStroke(SELECTION_STROKE);
        g.draw(new RoundRectangle2D.Double(shape.getX() - 5, shape.getY() - 5,
                shape.getWidth() + 10, shape.getHeight() + 10, NODE_ARC + 6, NODE_ARC + 6));
    }

    private void drawName(Graphics2D g, Device device, RoundRectangle2D shape, Color color) {
        g.setFont(getFont().deriveFont(Font.BOLD, 13f));
        FontMetrics metrics = g.getFontMetrics();
        int textX = (int) (shape.getCenterX() - metrics.stringWidth(device.getName()) / 2.0);
        int textY = (int) (shape.getCenterY() + (metrics.getAscent() - metrics.getDescent()) / 2.0);
        g.setColor(color);
        g.drawString(device.getName(), textX, textY);
    }

    private static Color fillFor(Device device) {
        if (device.getType() == DeviceType.INTERNET) {
            return INTERNET;
        }
        return switch (device.getKnownState()) {
            case HEALTHY -> HEALTHY;
            case SUSPICIOUS -> SUSPICIOUS;
            case COMPROMISED -> COMPROMISED;
        };
    }
}
