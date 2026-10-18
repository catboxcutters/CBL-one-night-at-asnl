package nl.tue.sysadminpanic.app;

import javax.swing.SwingUtilities;
import nl.tue.sysadminpanic.scenario.OfficeNetworkScenario;
import nl.tue.sysadminpanic.ui.MainFrame;
import nl.tue.sysadminpanic.ui.TerminalTheme;

/**
 * Entry point of One Night at ASNL.
 */
public final class Main {

    private Main() {
        // Entry point only: never instantiated.
    }

    /**
     * Starts the game.
     *
     * @param args command-line arguments (ignored)
     */
    public static void main(String[] args) {
        // Swing components may only be created and touched on the Event Dispatch Thread.
        SwingUtilities.invokeLater(() -> {
            TerminalTheme.install();
            new MainFrame(new OfficeNetworkScenario()).setVisible(true);
        });
    }
}
