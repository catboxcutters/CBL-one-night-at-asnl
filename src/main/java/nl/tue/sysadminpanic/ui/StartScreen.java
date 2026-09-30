package nl.tue.sysadminpanic.ui;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import nl.tue.sysadminpanic.model.GameConstants;
import nl.tue.sysadminpanic.model.Wallet;

/**
 * The first screen: title, short how-to-play and the Start button.
 */
public class StartScreen extends JPanel {

    private static final String HOW_TO_PLAY = "<html><div style='width:520px'>"
            + "<p>You run the security team of a small office. Five waves of attackers"
            + " will try to reach the database. Survive all five to win.</p><br>"
            + "<ul>"
            + "<li>The map shows what you know: green healthy, amber suspicious,"
            + " red compromised. Some attacks are silent.</li>"
            + "<li>Click an alert to select its device. Not every alert is real:"
            + " Inspect is free and shows the true state and the logs.</li>"
            + "<li>Spend money on Block IP, Isolate, Patch, Firewall rule and Restore."
            + " You earn " + Wallet.format(GameConstants.INCOME_AMOUNT) + " every "
            + GameConstants.INCOME_INTERVAL_TICKS + " seconds.</li>"
            + "<li>Compromised and isolated devices drain network health."
            + " At 0% the network is breached.</li>"
            + "<li>Pause any time: you can still act while paused.</li>"
            + "</ul></div></html>";

    /**
     * @param onStart what to run when the player clicks Start
     */
    public StartScreen(Runnable onStart) {
        super(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.insets = new Insets(8, 8, 8, 8);

        JLabel title = new JLabel("Sysadmin Panic", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 40f));
        add(title, c);

        add(new JLabel(HOW_TO_PLAY), c);

        JButton start = new JButton("Start");
        start.setFont(start.getFont().deriveFont(Font.BOLD, 18f));
        start.addActionListener(event -> onStart.run());
        c.insets = new Insets(20, 8, 8, 8);
        add(start, c);
    }
}
