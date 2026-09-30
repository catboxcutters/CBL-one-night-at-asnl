package nl.tue.sysadminpanic.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import nl.tue.sysadminpanic.model.Wallet;
import nl.tue.sysadminpanic.sim.GameStats;

/**
 * The end-of-game summary: "NETWORK SECURED" or "NETWORK BREACHED", the
 * statistics, and a Play again button.
 */
public class EndDialog extends JDialog {

    private static final Color SECURED = new Color(46, 157, 87);
    private static final Color BREACHED = new Color(214, 69, 65);

    /**
     * @param owner       the main window, which the dialog is centered on
     * @param stats       the numbers to show
     * @param onPlayAgain what to run when the player clicks Play again
     */
    public EndDialog(JFrame owner, GameStats stats, Runnable onPlayAgain) {
        super(owner, "Game over", true);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setBorder(BorderFactory.createEmptyBorder(20, 28, 20, 28));
        content.add(buildTitle(stats.won()), BorderLayout.NORTH);
        content.add(buildStats(stats), BorderLayout.CENTER);
        content.add(buildButtons(onPlayAgain), BorderLayout.SOUTH);
        setContentPane(content);
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private JLabel buildTitle(boolean won) {
        JLabel title = new JLabel(won ? "NETWORK SECURED" : "NETWORK BREACHED",
                SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 26f));
        title.setForeground(won ? SECURED : BREACHED);
        return title;
    }

    private JPanel buildStats(GameStats stats) {
        JPanel grid = new JPanel(new GridLayout(0, 2, 24, 6));
        addRow(grid, "Final network health",
                String.format(Locale.US, "%.1f%%", stats.finalHealth()));
        addRow(grid, "Waves survived", stats.wavesSurvived() + " of " + stats.totalWaves());
        addRow(grid, "Threats detected", stats.threatsDetected() + " of " + stats.totalThreats());
        addRow(grid, "Threats contained",
                stats.threatsContained() + " of " + stats.totalThreats());
        addRow(grid, "Systems compromised", String.valueOf(stats.systemsCompromised()));
        addRow(grid, "Money spent", Wallet.format(stats.moneySpent()));
        return grid;
    }

    private static void addRow(JPanel grid, String name, String value) {
        grid.add(new JLabel(name));
        JLabel valueLabel = new JLabel(value, SwingConstants.RIGHT);
        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD));
        grid.add(valueLabel);
    }

    private JPanel buildButtons(Runnable onPlayAgain) {
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton playAgain = new JButton("Play again");
        playAgain.addActionListener(event -> {
            dispose();
            onPlayAgain.run();
        });
        buttons.add(playAgain);
        getRootPane().setDefaultButton(playAgain);
        return buttons;
    }
}
