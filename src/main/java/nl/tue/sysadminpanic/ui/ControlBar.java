package nl.tue.sysadminpanic.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import nl.tue.sysadminpanic.action.ActionResult;
import nl.tue.sysadminpanic.model.GameConstants;
import nl.tue.sysadminpanic.sim.GameEvent;
import nl.tue.sysadminpanic.sim.GameEventListener;

/**
 * The bottom bar: the pause/resume button and a one-line feed showing the
 * latest action result or game event.
 */
public class ControlBar extends JPanel implements GameEventListener {

    private static final Color FAILURE = new Color(200, 50, 50);

    private final GameClock clock;
    private final JButton pauseButton = new JButton("Pause");
    private final JLabel feedLabel = new JLabel();
    private final Color normalColor = feedLabel.getForeground();
    private final Font normalFont = feedLabel.getFont();

    /**
     * @param clock the clock that the pause button stops and starts
     */
    public ControlBar(GameClock clock) {
        super(new BorderLayout(12, 0));
        this.clock = clock;
        setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        pauseButton.addActionListener(event -> togglePause());
        add(pauseButton, BorderLayout.WEST);
        add(feedLabel, BorderLayout.CENTER);
        show("Monitoring the network. The first wave arrives in "
                + GameConstants.FIRST_WAVE_DELAY_TICKS + " seconds.", false, false);
    }

    private void togglePause() {
        if (clock.isRunning()) {
            clock.pause();
            pauseButton.setText("Resume");
            show("Paused. You can still inspect and act.", false, false);
        } else {
            clock.start();
            pauseButton.setText("Pause");
            show("Resumed.", false, false);
        }
    }

    @Override
    public void onEvent(GameEvent event) {
        switch (event) {
            case GameEvent.ActionPerformed performed -> showResult(performed.result());
            case GameEvent.AlertRaised raised -> show("New " + raised.alert().severity()
                    + " alert: " + raised.alert().summary(), false, false);
            case GameEvent.WaveStarted started -> show("WAVE " + started.number() + "/"
                    + started.totalWaves() + ": " + started.name(), false, true);
            case GameEvent.WaveEnded ended -> show("Wave " + ended.number()
                    + " is over. The next wave arrives in " + GameConstants.WAVE_GAP_TICKS
                    + " seconds.", false, false);
            case GameEvent.GameOver over -> {
                pauseButton.setEnabled(false);
                show(over.won() ? "All waves survived." : "Network health reached 0%.",
                        !over.won(), true);
            }
            default -> {
                // Ticks, money, health and state changes are shown elsewhere.
            }
        }
    }

    private void showResult(ActionResult result) {
        show(result.success() ? result.message() : "Not done: " + result.message(),
                !result.success(), false);
    }

    private void show(String text, boolean failure, boolean banner) {
        feedLabel.setText(text);
        feedLabel.setForeground(failure ? FAILURE : normalColor);
        feedLabel.setFont(banner ? normalFont.deriveFont(Font.BOLD, 14f) : normalFont);
    }
}
