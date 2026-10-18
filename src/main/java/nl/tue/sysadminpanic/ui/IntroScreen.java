package nl.tue.sysadminpanic.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import javax.swing.JPanel;

/**
 * Shows the "Help Wanted" newspaper ad over the whole window until the player
 * clicks anywhere.
 */
public class IntroScreen extends JPanel {

    private static final String IMAGE_RESOURCE = "/images/help_wanted.png";

    /**
     * true: the image covers the whole window and the edges may be cropped when
     * the window's aspect ratio differs from the image's.
     * false: the whole image is always visible, with black bars if needed.
     */
    private static final boolean COVER_WINDOW = true;

    /** The ad, or null if it could not be loaded (the screen is then plain black). */
    private final BufferedImage image;

    /** Guards against a double click starting the game twice. */
    private boolean finished;

    /**
     * @param onContinue what to run when the player clicks
     */
    public IntroScreen(Runnable onContinue) {
        setBackground(Color.BLACK);
        image = loadImage();
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                if (!finished) {
                    finished = true;
                    onContinue.run();
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // fills with the black background
        if (image == null) {
            return;
        }
        double scaleX = (double) getWidth() / image.getWidth();
        double scaleY = (double) getHeight() / image.getHeight();
        double scale = COVER_WINDOW ? Math.max(scaleX, scaleY) : Math.min(scaleX, scaleY);
        int width = (int) Math.round(image.getWidth() * scale);
        int height = (int) Math.round(image.getHeight() * scale);

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.drawImage(image, (getWidth() - width) / 2, (getHeight() - height) / 2,
                    width, height, null);
        } finally {
            g2.dispose();
        }
    }

    private static BufferedImage loadImage() {
        try (InputStream in = IntroScreen.class.getResourceAsStream(IMAGE_RESOURCE)) {
            if (in == null) {
                System.err.println("Image not found: " + IMAGE_RESOURCE);
                return null;
            }
            return ImageIO.read(in);
        } catch (IOException e) {
            System.err.println("Could not load " + IMAGE_RESOURCE + ": " + e);
            return null;
        }
    }
}
