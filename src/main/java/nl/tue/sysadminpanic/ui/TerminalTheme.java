package nl.tue.sysadminpanic.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;
import nl.tue.sysadminpanic.model.DeviceState;
import nl.tue.sysadminpanic.model.Severity;

/**
 * The look of the game: a dark Linux-terminal skin with ANSI-style colors and
 * a monospace font.
 *
 * <p>Every color and font the UI uses is defined here, so the style can be
 * changed in one place (the UI counterpart of {@code GameConstants}).
 */
public final class TerminalTheme {

    public static final Color BACKGROUND = new Color(0x0C0C0C);
    public static final Color SURFACE = new Color(0x151515);
    public static final Color TEXT = new Color(0xCCCCCC);
    public static final Color DIM = new Color(0x6C6C6C);
    public static final Color BORDER = new Color(0x3A3A3A);
    public static final Color GREEN = new Color(0x4EC94E);
    public static final Color YELLOW = new Color(0xE5C07B);
    public static final Color ORANGE = new Color(0xD19A66);
    public static final Color RED = new Color(0xE06C75);
    public static final Color CYAN = new Color(0x56B6C2);
    public static final Color BLUE = new Color(0x61AFEF);

    /** The shell prompt shown in the status bar and the event feed. */
    public static final String PROMPT = "root@soc:~$ ";

    private static final float BASE_SIZE = 13f;

    /** Common terminal fonts: Windows, macOS, then Linux. */
    private static final List<String> PREFERRED_FONTS = List.of(
            "Cascadia Mono", "Consolas", "Menlo", "DejaVu Sans Mono", "Liberation Mono");
    private static final String FONT_FAMILY = pickFontFamily();

    private TerminalTheme() {
        // Constants and helpers only.
    }

    /**
     * Installs the theme for every Swing component created afterwards.
     * Must be called on the Event Dispatch Thread, before any window is built.
     */
    public static void install() {
        try {
            // Metal is the same on every OS and, unlike the native looks,
            // respects the background colors we set. Its plain "Steel" theme has no
            // gradients, so buttons and scroll bars stay flat like a terminal.
            MetalLookAndFeel.setCurrentTheme(new DefaultMetalTheme());
            UIManager.setLookAndFeel(new MetalLookAndFeel());
        } catch (UnsupportedLookAndFeelException e) {
            // Not fatal: the game still works with the default look.
        }
        installColors();
        setBorder(BorderFactory.createLineBorder(BORDER), "ToolTip.border",
                "ProgressBar.border", "ComboBox.border");
        // Also used by buttons Swing creates itself, such as OK/Cancel in dialogs.
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)), "Button.border");
        setBorder(BorderFactory.createEmptyBorder(), "SplitPane.border", "ScrollPane.border",
                "SplitPaneDivider.border");
        UIManager.put("ScrollBar.width", 10);
        setFonts(font(BASE_SIZE, false), "Label.font", "Button.font", "List.font",
                "TextArea.font", "ToolTip.font", "OptionPane.messageFont",
                "OptionPane.buttonFont", "ComboBox.font", "ProgressBar.font",
                "TitledBorder.font");
    }

    /** Sets the default colors, which also covers dialogs Swing builds for us. */
    private static void installColors() {
        setColors(BACKGROUND, "Panel.background", "OptionPane.background",
                "SplitPane.background", "ScrollPane.background", "Viewport.background",
                "List.background", "ComboBox.background", "Button.background",
                "ProgressBar.background", "ScrollBar.background", "ScrollBar.track",
                "ScrollBar.highlight", "ScrollBar.shadow", "ScrollBar.darkShadow",
                "ScrollBar.trackHighlight", "SplitPane.highlight", "SplitPane.shadow",
                "SplitPane.darkShadow", "List.selectionForeground",
                "ComboBox.selectionForeground",
                // Text drawn over the filled (colored) part of the health bar.
                "ProgressBar.selectionForeground");
        setColors(SURFACE, "TextArea.background", "ToolTip.background", "Button.select");
        setColors(TEXT, "Label.foreground", "OptionPane.messageForeground",
                "List.foreground", "ComboBox.foreground", "Button.foreground",
                "TextArea.foreground", "ToolTip.foreground", "TitledBorder.titleColor",
                "ProgressBar.selectionBackground", "List.selectionBackground",
                "ComboBox.selectionBackground");
        setColors(DIM, "Button.disabledText", "Label.disabledForeground");
        setColors(BACKGROUND, "ComboBox.buttonBackground", "ComboBox.buttonHighlight",
                "ComboBox.buttonShadow", "Button.shadow", "Button.highlight", "Button.light");
        setColors(TEXT, "ComboBox.buttonDarkShadow");
        setColors(BORDER, "Button.darkShadow", "SplitPaneDivider.draggingColor",
                "ScrollBar.thumb", "ScrollBar.thumbHighlight", "ScrollBar.thumbShadow");
    }

    private static void setBorder(Border border, String... keys) {
        for (String key : keys) {
            UIManager.put(key, new BorderUIResource(border));
        }
    }

    private static void setColors(Color color, String... keys) {
        for (String key : keys) {
            UIManager.put(key, new ColorUIResource(color));
        }
    }

    private static void setFonts(Font font, String... keys) {
        for (String key : keys) {
            UIManager.put(key, new FontUIResource(font));
        }
    }

    /**
     * @param size the point size
     * @param bold whether the font is bold
     * @return the monospace font in that size
     */
    public static Font font(float size, boolean bold) {
        return new Font(FONT_FAMILY, bold ? Font.BOLD : Font.PLAIN, Math.round(size));
    }

    /**
     * Picks the first installed terminal font. Java's generic "Monospaced" font
     * is the fallback; on Windows it maps to Courier New, which draws the box and
     * block characters of the title banner at the wrong width.
     */
    private static String pickFontFamily() {
        List<String> installed = Arrays.asList(GraphicsEnvironment
                .getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        for (String family : PREFERRED_FONTS) {
            if (installed.contains(family)) {
                return family;
            }
        }
        return Font.MONOSPACED;
    }

    /**
     * A thin box with a title in its top edge, like {@code ┌─ title ──┐}.
     *
     * @param title the text in the top edge
     * @return the border
     */
    public static Border titledBorder(String title) {
        TitledBorder titled = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER), " " + title + " ");
        titled.setTitleColor(GREEN);
        titled.setTitleFont(font(BASE_SIZE, true));
        return titled;
    }

    /**
     * A flat, terminal-style button. When disabled, its text turns dim.
     *
     * @param text the label
     * @return the button
     */
    public static JButton button(String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        // Colors and border come from the defaults set in install().
        return button;
    }

    /**
     * @param state a device state
     * @return the color the map and inspector use for it
     */
    public static Color colorFor(DeviceState state) {
        return switch (state) {
            case HEALTHY -> GREEN;
            case SUSPICIOUS -> YELLOW;
            case COMPROMISED -> RED;
        };
    }

    /**
     * @param severity an alert severity
     * @return the color of its tag in the alert list
     */
    public static Color colorFor(Severity severity) {
        return switch (severity) {
            case LOW -> BLUE;
            case MEDIUM -> YELLOW;
            case HIGH -> ORANGE;
            case CRITICAL -> RED;
        };
    }
}
