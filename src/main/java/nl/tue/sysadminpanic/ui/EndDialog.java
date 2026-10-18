package nl.tue.sysadminpanic.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import nl.tue.sysadminpanic.model.Wallet;
import nl.tue.sysadminpanic.sim.GameStats;

/**
 * The end-of-game summary: "NETWORK SECURED" or "NETWORK BREACHED", the
 * statistics as a terminal report, and a Play again button.
 */
public class EndDialog extends JDialog {

    // Every report line is padded with dots to this width, so the values line up.
    private static final int LINE_WIDTH = 40;

    /**
     * @param owner       the main window, which the dialog is centered on
     * @param stats       the numbers to show
     * @param onPlayAgain what to run when the player clicks Play again
     */
    public EndDialog(JFrame owner, GameStats stats, Runnable onPlayAgain) {
        super(owner, "Game over", true);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(TerminalTheme.BORDER),
                BorderFactory.createEmptyBorder(20, 28, 20, 28)));
        content.add(buildTitle(stats.won()), BorderLayout.NORTH);
        content.add(buildReport(stats), BorderLayout.CENTER);
        content.add(buildButtons(onPlayAgain), BorderLayout.SOUTH);
        setContentPane(content);
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private JLabel buildTitle(boolean won) {
        JLabel title = new JLabel(won ? "NETWORK SECURED" : "NETWORK BREACHED",
                SwingConstants.CENTER);
        title.setFont(TerminalTheme.font(26f, true));
        title.setForeground(won ? TerminalTheme.GREEN : TerminalTheme.RED);
        return title;
    }

    private JTextArea buildReport(GameStats stats) {
        String report = String.join("\n",
                line("final network health",
                        String.format(Locale.US, "%.1f%%", stats.finalHealth())),
                line("waves survived", stats.wavesSurvived() + " of " + stats.totalWaves()),
                line("threats detected",
                        stats.threatsDetected() + " of " + stats.totalThreats()),
                line("threats contained",
                        stats.threatsContained() + " of " + stats.totalThreats()),
                line("systems compromised", String.valueOf(stats.systemsCompromised())),
                line("money spent", Wallet.format(stats.moneySpent())));
        JTextArea area = new JTextArea(report);
        area.setEditable(false);
        area.setFocusable(false);
        area.setOpaque(false);
        area.setFont(TerminalTheme.font(14f, false));
        return area;
    }

    /** "money spent ......................... $2,300" */
    private static String line(String name, String value) {
        int dots = Math.max(2, LINE_WIDTH - name.length() - value.length() - 2);
        return name + " " + ".".repeat(dots) + " " + value;
    }

    private JPanel buildButtons(Runnable onPlayAgain) {
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton playAgain = TerminalTheme.button("[ play again ]");
        playAgain.setForeground(TerminalTheme.GREEN);
        playAgain.addActionListener(event -> {
            dispose();
            onPlayAgain.run();
        });
        buttons.add(playAgain);
        getRootPane().setDefaultButton(playAgain);
        return buttons;
    }
}
