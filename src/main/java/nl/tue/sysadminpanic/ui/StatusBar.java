package nl.tue.sysadminpanic.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import nl.tue.sysadminpanic.model.GameTime;
import nl.tue.sysadminpanic.model.Wallet;
import nl.tue.sysadminpanic.sim.GameEvent;
import nl.tue.sysadminpanic.sim.GameEventListener;
import nl.tue.sysadminpanic.sim.Simulation;

/**
 * The top bar: scenario name (as a shell path), clock, wave counter, money and
 * the health bar.
 */
public class StatusBar extends JPanel implements GameEventListener {

    private static final int WARNING_BELOW = 60;
    private static final int CRITICAL_BELOW = 30;
    private static final String SEPARATOR = "│";

    private final Simulation simulation;
    private final JLabel clockLabel = infoLabel();
    private final JLabel waveLabel = infoLabel();
    private final JLabel moneyLabel = infoLabel();
    private final JProgressBar healthBar = new JProgressBar(0, 100);

    /**
     * @param scenarioName the name shown on the left
     * @param simulation   the game, read for the initial values
     */
    public StatusBar(String scenarioName, Simulation simulation) {
        super(new BorderLayout(16, 0));
        this.simulation = simulation;
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, TerminalTheme.BORDER),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));

        JPanel info = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        JLabel pathLabel = infoLabel();
        pathLabel.setText("root@soc:~/" + asPath(scenarioName));
        pathLabel.setForeground(TerminalTheme.GREEN);
        info.add(pathLabel);
        for (JLabel label : new JLabel[] {clockLabel, waveLabel, moneyLabel}) {
            info.add(separator());
            info.add(label);
        }
        add(info, BorderLayout.WEST);

        healthBar.setStringPainted(true);
        healthBar.setFont(TerminalTheme.font(14f, true));
        add(healthBar, BorderLayout.CENTER);

        showClock(simulation.getCurrentTick());
        showWave();
        showMoney(simulation.getMoney());
        showHealth(simulation.getHealth());
    }

    @Override
    public void onEvent(GameEvent event) {
        switch (event) {
            case GameEvent.TickAdvanced tick -> showClock(tick.tick());
            case GameEvent.MoneyChanged money -> showMoney(money.balance());
            case GameEvent.HealthChanged health -> showHealth(health.health());
            case GameEvent.WaveStarted started -> showWave();
            case GameEvent.WaveEnded ended -> showWave();
            default -> {
                // Other events do not change the status bar.
            }
        }
    }

    private void showClock(int tick) {
        clockLabel.setText(GameTime.formatHour(tick));
    }

    private void showWave() {
        waveLabel.setText("wave " + simulation.getCurrentWaveNumber() + "/"
                + simulation.getTotalWaves());
    }

    private void showMoney(int balance) {
        moneyLabel.setText(Wallet.format(balance));
    }

    private void showHealth(double health) {
        // Round up so the bar only shows 0% when the game is really lost.
        int percent = (int) Math.ceil(health);
        healthBar.setValue(percent);
        healthBar.setString("network health " + percent + "%");
        if (percent < CRITICAL_BELOW) {
            healthBar.setForeground(TerminalTheme.RED);
        } else if (percent < WARNING_BELOW) {
            healthBar.setForeground(TerminalTheme.YELLOW);
        } else {
            healthBar.setForeground(TerminalTheme.GREEN);
        }
    }

    /** "Office Network" becomes "office-network", like a directory name. */
    private static String asPath(String name) {
        return name.toLowerCase(Locale.ROOT).replace(' ', '-');
    }

    private static JLabel separator() {
        JLabel label = infoLabel();
        label.setText(SEPARATOR);
        label.setForeground(TerminalTheme.DIM);
        return label;
    }

    private static JLabel infoLabel() {
        JLabel label = new JLabel();
        label.setFont(TerminalTheme.font(14f, true));
        return label;
    }
}
