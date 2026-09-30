package nl.tue.sysadminpanic.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
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
 * The top bar: scenario name, clock, wave counter, money and the health bar.
 */
public class StatusBar extends JPanel implements GameEventListener {

    private static final Color HEALTH_GOOD = new Color(46, 157, 87);
    private static final Color HEALTH_WARNING = new Color(224, 161, 0);
    private static final Color HEALTH_CRITICAL = new Color(214, 69, 65);
    private static final int WARNING_BELOW = 60;
    private static final int CRITICAL_BELOW = 30;

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
        super(new BorderLayout(12, 0));
        this.simulation = simulation;
        setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        JPanel info = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 0));
        JLabel nameLabel = infoLabel();
        nameLabel.setText(scenarioName);
        info.add(nameLabel);
        info.add(clockLabel);
        info.add(waveLabel);
        info.add(moneyLabel);
        add(info, BorderLayout.WEST);

        healthBar.setStringPainted(true);
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
        clockLabel.setText(GameTime.format(tick));
    }

    private void showWave() {
        waveLabel.setText("Wave " + simulation.getCurrentWaveNumber() + "/"
                + simulation.getTotalWaves());
    }

    private void showMoney(int balance) {
        moneyLabel.setText(Wallet.format(balance));
    }

    private void showHealth(double health) {
        // Round up so the bar only shows 0% when the game is really lost.
        int percent = (int) Math.ceil(health);
        healthBar.setValue(percent);
        healthBar.setString("Network health " + percent + "%");
        if (percent < CRITICAL_BELOW) {
            healthBar.setForeground(HEALTH_CRITICAL);
        } else if (percent < WARNING_BELOW) {
            healthBar.setForeground(HEALTH_WARNING);
        } else {
            healthBar.setForeground(HEALTH_GOOD);
        }
    }

    private static JLabel infoLabel() {
        JLabel label = new JLabel();
        label.setFont(label.getFont().deriveFont(Font.BOLD, 14f));
        return label;
    }
}
