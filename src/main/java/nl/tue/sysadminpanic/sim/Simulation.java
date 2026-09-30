package nl.tue.sysadminpanic.sim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import nl.tue.sysadminpanic.action.ActionContext;
import nl.tue.sysadminpanic.action.ActionResult;
import nl.tue.sysadminpanic.action.PlayerAction;
import nl.tue.sysadminpanic.attack.AttackCampaign;
import nl.tue.sysadminpanic.attack.CampaignStatus;
import nl.tue.sysadminpanic.model.Alert;
import nl.tue.sysadminpanic.model.AlertType;
import nl.tue.sysadminpanic.model.Device;
import nl.tue.sysadminpanic.model.DeviceState;
import nl.tue.sysadminpanic.model.GameConstants;
import nl.tue.sysadminpanic.model.LogEntry;
import nl.tue.sysadminpanic.model.Network;
import nl.tue.sysadminpanic.model.NetworkHealth;
import nl.tue.sysadminpanic.model.Severity;
import nl.tue.sysadminpanic.model.Wallet;
import nl.tue.sysadminpanic.scenario.Scenario;
import nl.tue.sysadminpanic.scenario.WaveDefinition;

/**
 * Runs the game one tick at a time and tells listeners what happened.
 *
 * <p>This is plain model code with no Swing: the UI calls {@link #tick()} from a
 * timer, and tests call it directly in a loop. It is the <em>subject</em> of the
 * Observer pattern, and it implements {@link ActionContext} (and so also
 * {@code AttackContext}) so that every change made by an attack or a player
 * action goes through here and produces an event.
 *
 * <p>Order of work in one tick: advance the clock, pay income, let every active
 * campaign act, then drain health for the resulting situation.
 */
public class Simulation implements ActionContext {

    private final Network network;
    private final NetworkHealth health = new NetworkHealth();
    private final Wallet wallet = new Wallet(GameConstants.START_MONEY);
    private final List<AttackCampaign> campaigns = new ArrayList<>();
    private final List<Alert> alerts = new ArrayList<>();
    private final Set<String> blockedIps = new HashSet<>();
    private final Set<Device> everCompromised = new HashSet<>();
    private final List<GameEventListener> listeners = new ArrayList<>();
    private final WaveRunner waveRunner;

    private int currentTick;
    private int nextAlertId = 1;
    private int exfiltrationsThisTick;
    private boolean gameOver;
    private boolean won;

    /**
     * Creates a simulation at tick 0 with full health and starting money.
     *
     * @param network the network to defend
     * @param waves   the attack script, played in order
     */
    public Simulation(Network network, List<WaveDefinition> waves) {
        this.network = network;
        this.waveRunner = new WaveRunner(this, waves);
    }

    /**
     * Creates a simulation with no wave script, so attacks only happen when
     * started by hand. Used by tests that examine one mechanic at a time.
     *
     * @param network the network to defend
     */
    public Simulation(Network network) {
        this(network, List.of());
    }

    /**
     * Creates a simulation for a fresh game of the given scenario.
     *
     * @param scenario the scenario to play
     * @return a new simulation with a newly built network
     */
    public static Simulation forScenario(Scenario scenario) {
        return new Simulation(scenario.createNetwork(), scenario.getWaves());
    }

    /**
     * Registers a listener that will receive every future event.
     *
     * @param listener the listener to add
     */
    public void addListener(GameEventListener listener) {
        listeners.add(listener);
    }

    // --- Campaigns ---

    /**
     * Adds a campaign that starts with a port scan.
     *
     * @param campaign a campaign that has not begun yet
     */
    public void startCampaignWithRecon(AttackCampaign campaign) {
        if (admit(campaign)) {
            campaign.beginWithRecon(this);
        }
    }

    /**
     * Adds a campaign that starts directly with its break-in attempt.
     *
     * @param campaign a campaign that has not begun yet
     */
    public void startCampaign(AttackCampaign campaign) {
        if (admit(campaign)) {
            campaign.beginWithInitialAccess(this);
        }
    }

    /**
     * Registers a new campaign. One from a blocked IP is stopped at the edge at once.
     *
     * @return true if the campaign may begin
     */
    private boolean admit(AttackCampaign campaign) {
        campaigns.add(campaign);
        if (blockedIps.contains(campaign.getSourceIp())) {
            campaign.end(CampaignStatus.STOPPED, this);
            return false;
        }
        return true;
    }

    // --- Ticking ---

    /**
     * Advances the game by one tick. Does nothing once the game is over.
     */
    public void tick() {
        if (gameOver) {
            return;
        }
        currentTick++;
        exfiltrationsThisTick = 0;
        publish(new GameEvent.TickAdvanced(currentTick));
        payIncome();
        advanceCampaigns();
        updateHealth();
        // A lost game must not also be won in the same tick, so waves run last.
        if (!gameOver) {
            waveRunner.tick(currentTick);
        }
    }

    private void payIncome() {
        if (currentTick % GameConstants.INCOME_INTERVAL_TICKS == 0) {
            wallet.earn(GameConstants.INCOME_AMOUNT);
            publish(new GameEvent.MoneyChanged(wallet.getBalance()));
        }
    }

    private void advanceCampaigns() {
        // Iterate over a copy: a campaign's actions must not be able to
        // modify the list while we are looping over it.
        for (AttackCampaign campaign : List.copyOf(campaigns)) {
            campaign.tick(this);
        }
    }

    private void updateHealth() {
        double drain = NetworkHealth.drainPerTick(network.getDevices(), exfiltrationsThisTick);
        if (drain <= 0) {
            return;
        }
        health.drain(drain);
        publish(new GameEvent.HealthChanged(health.getValue()));
        if (health.isDepleted()) {
            endGame(false);
        }
    }

    /**
     * Ends the game and announces the result. Called on 0% health, or by the
     * wave runner after the last wave.
     */
    void endGame(boolean playerWon) {
        gameOver = true;
        won = playerWon;
        publish(new GameEvent.GameOver(playerWon));
    }

    // --- Player actions ---

    /**
     * Checks whether the player may do an action right now. The UI uses this to
     * disable a button and show the reason as its tooltip.
     *
     * @param action the action to check
     * @return the reason it is not allowed, or empty if it is
     */
    public Optional<String> checkAllowed(PlayerAction action) {
        if (gameOver) {
            return Optional.of("The game is over");
        }
        Optional<String> invalid = action.checkValid(this);
        if (invalid.isPresent()) {
            return invalid;
        }
        if (!wallet.canAfford(action.getPrice())) {
            return Optional.of("Costs " + Wallet.format(action.getPrice())
                    + ", you have " + Wallet.format(wallet.getBalance()));
        }
        return Optional.empty();
    }

    /**
     * Tries to carry out a player action: checks it, charges its price, runs it,
     * and publishes the result. Works while the game is paused.
     *
     * @param action the action to perform
     * @return what happened, or why nothing happened
     */
    public ActionResult perform(PlayerAction action) {
        Optional<String> problem = checkAllowed(action);
        ActionResult result;
        if (problem.isPresent()) {
            result = ActionResult.failure(problem.get());
        } else {
            chargeFor(action);
            result = action.execute(this);
        }
        publish(new GameEvent.ActionPerformed(result));
        return result;
    }

    private void chargeFor(PlayerAction action) {
        if (action.getPrice() > 0) {
            wallet.spend(action.getPrice());
            publish(new GameEvent.MoneyChanged(wallet.getBalance()));
        }
    }

    // --- Alerts ---

    /**
     * Raises an alert and makes the target look suspicious if it looked healthy.
     * Alerts never mark a device as compromised: only inspecting it reveals that.
     *
     * @param type          what kind of activity was detected
     * @param sourceIp      where the activity came from
     * @param target        the device the alert is about
     * @param severity      how serious the alert is
     * @param summary       one-line description
     * @param falsePositive true if the alert is harmless
     */
    public void raiseAlert(AlertType type, String sourceIp, Device target, Severity severity,
            String summary, boolean falsePositive) {
        Alert alert = new Alert(nextAlertId++, type, sourceIp, target.getId(), severity,
                summary, currentTick, falsePositive);
        alerts.add(alert);
        if (!falsePositive) {
            markCampaignsDetected(sourceIp);
        }
        publish(new GameEvent.AlertRaised(alert));
        if (target.getKnownState() == DeviceState.HEALTHY) {
            target.setKnownState(DeviceState.SUSPICIOUS);
            publish(new GameEvent.DeviceStateChanged(target));
        }
    }

    private void markCampaignsDetected(String sourceIp) {
        for (AttackCampaign campaign : campaigns) {
            if (campaign.getSourceIp().equals(sourceIp)) {
                campaign.markDetected();
            }
        }
    }

    // --- AttackContext and ActionContext ---

    @Override
    public void raiseAlert(AlertType type, String sourceIp, Device target, Severity severity,
            String summary) {
        raiseAlert(type, sourceIp, target, severity, summary, false);
    }

    @Override
    public void log(Device device, String text) {
        device.addLog(new LogEntry(currentTick, device.getId(), text));
    }

    @Override
    public void compromise(Device device) {
        if (device.getTrueState() != DeviceState.COMPROMISED) {
            device.setTrueState(DeviceState.COMPROMISED);
            everCompromised.add(device);
            publish(new GameEvent.DeviceStateChanged(device));
        }
    }

    @Override
    public void reportExfiltration(Device source) {
        exfiltrationsThisTick++;
    }

    @Override
    public void campaignEnded(AttackCampaign campaign) {
        publish(new GameEvent.CampaignEnded(campaign.getSourceIp(), campaign.getStatus()));
    }

    @Override
    public boolean isIpBlocked(String ip) {
        return blockedIps.contains(ip);
    }

    @Override
    public void blockIp(String ip) {
        blockedIps.add(ip);
    }

    @Override
    public void deviceChanged(Device device) {
        publish(new GameEvent.DeviceStateChanged(device));
    }

    /** Sends an event to every listener, in the order they were added. */
    void publish(GameEvent event) {
        for (GameEventListener listener : listeners) {
            listener.onEvent(event);
        }
    }

    // --- Getters ---

    @Override
    public Network getNetwork() {
        return network;
    }

    @Override
    public int getCurrentTick() {
        return currentTick;
    }

    /** @return the current network health, from 0 to 100 */
    public double getHealth() {
        return health.getValue();
    }

    /** @return the money currently available */
    public int getMoney() {
        return wallet.getBalance();
    }

    /** @return the total money spent so far */
    public int getMoneySpent() {
        return wallet.getTotalSpent();
    }

    /** @return true once the game has ended */
    public boolean isGameOver() {
        return gameOver;
    }

    /** @return true if the game ended with the player surviving every wave */
    public boolean isWon() {
        return won;
    }

    /** @return the number of the running or next wave, starting at 1 */
    public int getCurrentWaveNumber() {
        return waveRunner.getCurrentWaveNumber();
    }

    /** @return how many waves the script has */
    public int getTotalWaves() {
        return waveRunner.getTotalWaves();
    }

    /**
     * Collects the numbers for the end-of-game summary. Can be called at any
     * time, but is meant for when the game is over.
     *
     * @return the statistics so far
     */
    public GameStats getStats() {
        int detected = 0;
        int contained = 0;
        for (AttackCampaign campaign : campaigns) {
            if (campaign.isDetected()) {
                detected++;
            }
            if (campaign.getStatus().isDefeated()) {
                contained++;
            }
        }
        return new GameStats(won, health.getValue(), waveRunner.getWavesCompleted(),
                waveRunner.getTotalWaves(), detected, contained, campaigns.size(),
                everCompromised.size(), wallet.getTotalSpent());
    }

    /** @return every alert raised so far, oldest first (read-only) */
    public List<Alert> getAlerts() {
        return Collections.unmodifiableList(alerts);
    }

    /** @return every campaign started so far, including ended ones (read-only) */
    @Override
    public List<AttackCampaign> getCampaigns() {
        return Collections.unmodifiableList(campaigns);
    }
}
