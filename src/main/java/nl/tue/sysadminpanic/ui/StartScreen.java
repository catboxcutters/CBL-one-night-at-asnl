package nl.tue.sysadminpanic.ui;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import nl.tue.sysadminpanic.model.GameConstants;
import nl.tue.sysadminpanic.model.Wallet;

/**
 * The first screen, styled as a terminal login: an ASCII-art title, the
 * how-to-play text as the output of {@code cat}, and the Start button.
 */
public class StartScreen extends JPanel {

    /** "ONE NIGHT AT ASNL" in the ANSI Shadow block-letter style. */
    private static final String BANNER = String.join("\n",
            " ██████╗ ███╗   ██╗███████╗   ███╗   ██╗██╗ ██████╗ ██╗  ██╗████████╗",
            "██╔═══██╗████╗  ██║██╔════╝   ████╗  ██║██║██╔════╝ ██║  ██║╚══██╔══╝",
            "██║   ██║██╔██╗ ██║█████╗     ██╔██╗ ██║██║██║  ███╗███████║   ██║",
            "██║   ██║██║╚██╗██║██╔══╝     ██║╚██╗██║██║██║   ██║██╔══██║   ██║",
            "╚██████╔╝██║ ╚████║███████╗   ██║ ╚████║██║╚██████╔╝██║  ██║   ██║",
            " ╚═════╝ ╚═╝  ╚═══╝╚══════╝   ╚═╝  ╚═══╝╚═╝ ╚═════╝ ╚═╝  ╚═╝   ╚═╝",
            "        █████╗ ████████╗    █████╗ ███████╗███╗   ██╗██╗",
            "       ██╔══██╗╚══██╔══╝   ██╔══██╗██╔════╝████╗  ██║██║",
            "       ███████║   ██║      ███████║███████╗██╔██╗ ██║██║",
            "       ██╔══██║   ██║      ██╔══██║╚════██║██║╚██╗██║██║",
            "       ██║  ██║   ██║      ██║  ██║███████║██║ ╚████║███████╗",
            "       ╚═╝  ╚═╝   ╚═╝      ╚═╝  ╚═╝╚══════╝╚═╝  ╚═══╝╚══════╝");

    private static final String HOW_TO_PLAY = String.join("\n",
            "Last login: 23:59:12 on tty1",
            TerminalTheme.PROMPT + "cat how-to-play.txt",
            "",
            "You run the security team of a small office. Five waves of",
            "attackers will try to reach the database. Survive all five to win.",
            "",
            " * The map shows what you know: green healthy, yellow suspicious,",
            "   red compromised. Some attacks are silent.",
            " * Click an alert to select its device. Not every alert is real:",
            "   inspect is free and shows the true state and the logs.",
            " * Spend money on block ip, isolate, patch, firewall rule and",
            "   restore. You earn " + Wallet.format(GameConstants.INCOME_AMOUNT) + " every "
                    + GameConstants.INCOME_INTERVAL_TICKS + " seconds.",
            " * Compromised and isolated devices drain network health.",
            "   At 0% the network is breached.",
            " * Pause any time: you can still act while paused.",
            "",
            TerminalTheme.PROMPT + "./start");

    /**
     * @param onStart what to run when the player clicks Start
     */
    public StartScreen(Runnable onStart) {
        super(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(6, 8, 6, 8);

        add(textBlock(BANNER, TerminalTheme.GREEN, 13f), c);
        c.insets = new Insets(18, 8, 6, 8);
        add(textBlock(HOW_TO_PLAY, TerminalTheme.TEXT, 14f), c);

        JButton start = TerminalTheme.button("[ start ]");
        start.setFont(TerminalTheme.font(16f, true));
        start.setForeground(TerminalTheme.GREEN);
        start.addActionListener(event -> onStart.run());
        add(start, c);
    }

    /** A read-only block of monospace text, which keeps the ASCII art aligned. */
    private static JTextArea textBlock(String text, Color color, float size) {
        JTextArea area = new JTextArea(text);
        area.setEditable(false);
        area.setFocusable(false);
        area.setOpaque(false);
        area.setForeground(color);
        area.setFont(TerminalTheme.font(size, false));
        return area;
    }
}
