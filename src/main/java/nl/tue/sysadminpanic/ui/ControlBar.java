package nl.tue.sysadminpanic.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import nl.tue.sysadminpanic.action.ActionResult;
import nl.tue.sysadminpanic.model.GameConstants;
import nl.tue.sysadminpanic.sim.GameEvent;
import nl.tue.sysadminpanic.sim.GameEventListener;

/**
 * The bottom bar: the pause/resume button and a one-line feed showing the
 * latest action result or game event, written after a shell prompt with a
 * blinking cursor.
 */
public class ControlBar extends JPanel implements GameEventListener {

    private static final int CURSOR_BLINK_MS = 500;
    private static final String CURSOR = "█";

    private final GameClock clock;
    private final JButton pauseButton = TerminalTheme.button("[ pause ]");
    private final JLabel promptLabel = new JLabel(TerminalTheme.PROMPT);
    private final JLabel feedLabel = new JLabel();
    private final JLabel cursorLabel = new JLabel(CURSOR);
    private final Font normalFont = TerminalTheme.font(13f, false);

    /**
     * @param clock the clock that the pause button stops and starts
     */
    public ControlBar(GameClock clock) {
        super(new BorderLayout(12, 0));
        this.clock = clock;
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, TerminalTheme.BORDER),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)));
        pauseButton.addActionListener(event -> togglePause());
        add(pauseButton, BorderLayout.WEST);
        add(buildFeedLine(), BorderLayout.CENTER);
        startCursorBlink();
        show("Monitoring the network. The first wave arrives in "
                + GameConstants.FIRST_WAVE_DELAY_TICKS + " seconds.", TerminalTheme.TEXT, false);
    }

    private JPanel buildFeedLine() {
        JPanel line = new JPanel(new BorderLayout());
        promptLabel.setFont(TerminalTheme.font(13f, true));
        promptLabel.setForeground(TerminalTheme.GREEN);
        cursorLabel.setFont(normalFont);
        JPanel textAndCursor = new JPanel(new BorderLayout());
        textAndCursor.add(feedLabel, BorderLayout.WEST);
        textAndCursor.add(cursorLabel, BorderLayout.CENTER);
        line.add(promptLabel, BorderLayout.WEST);
        line.add(textAndCursor, BorderLayout.CENTER);
        return line;
    }

    /**
     * Blinks the cursor with its own Swing timer. It is purely cosmetic and
     * separate from the game clock, so it keeps blinking while paused.
     */
    private void startCursorBlink() {
        Timer blink = new Timer(CURSOR_BLINK_MS,
                event -> cursorLabel.setVisible(!cursorLabel.isVisible()));
        blink.start();
        // Stop the timer when "Play again" replaces this bar, so it can be garbage-collected.
        addHierarchyListener(event -> {
            if (!isDisplayable()) {
                blink.stop();
            }
        });
    }

    private void togglePause() {
        if (clock.isRunning()) {
            clock.pause();
            pauseButton.setText("[ resume ]");
            show("Paused. You can still inspect and act.", TerminalTheme.TEXT, false);
        } else {
            clock.start();
            pauseButton.setText("[ pause ]");
            show("Resumed.", TerminalTheme.TEXT, false);
        }
    }

    @Override
    public void onEvent(GameEvent event) {
        switch (event) {
            case GameEvent.ActionPerformed performed -> showResult(performed.result());
            case GameEvent.AlertRaised raised -> show("New " + raised.alert().severity()
                    + " alert: " + raised.alert().summary(), TerminalTheme.TEXT, false);
            case GameEvent.WaveStarted started -> show("WAVE " + started.number() + "/"
                    + started.totalWaves() + ": " + started.name(), TerminalTheme.YELLOW, true);
            case GameEvent.WaveEnded ended -> show("Wave " + ended.number()
                    + " is over. The next wave arrives in " + GameConstants.WAVE_GAP_TICKS
                    + " seconds.", TerminalTheme.TEXT, false);
            case GameEvent.GameOver over -> {
                pauseButton.setEnabled(false);
                show(over.won() ? "All waves survived." : "Network health reached 0%.",
                        over.won() ? TerminalTheme.GREEN : TerminalTheme.RED, true);
            }
            default -> {
                // Ticks, money, health and state changes are shown elsewhere.
            }
        }
    }

    private void showResult(ActionResult result) {
        if (result.success()) {
            show(result.message(), TerminalTheme.TEXT, false);
        } else {
            show("Not done: " + result.message(), TerminalTheme.RED, false);
        }
    }

    private void show(String text, Color color, boolean banner) {
        feedLabel.setText(text + " ");
        feedLabel.setForeground(color);
        feedLabel.setFont(banner ? normalFont.deriveFont(Font.BOLD) : normalFont);
        cursorLabel.setForeground(color);
    }
}
