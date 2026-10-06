package nl.tue.sysadminpanic.app;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import nl.tue.sysadminpanic.scenario.OfficeNetworkScenario;
import nl.tue.sysadminpanic.ui.MainFrame;

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
            useSystemLookAndFeel();
            new MainFrame(new OfficeNetworkScenario()).setVisible(true);
        });
    }

    /** Makes the game look native on Windows, macOS and Linux. */
    private static void useSystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ReflectiveOperationException | UnsupportedLookAndFeelException e) {
            // Not fatal: Swing falls back to its default look.
        }
    }
}
