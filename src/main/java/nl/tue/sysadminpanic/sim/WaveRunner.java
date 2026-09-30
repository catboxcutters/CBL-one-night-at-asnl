package nl.tue.sysadminpanic.sim;

import java.util.ArrayList;
import java.util.List;
import nl.tue.sysadminpanic.attack.AttackCampaign;
import nl.tue.sysadminpanic.model.Device;
import nl.tue.sysadminpanic.model.GameConstants;
import nl.tue.sysadminpanic.model.Network;
import nl.tue.sysadminpanic.scenario.ScriptedEvent;
import nl.tue.sysadminpanic.scenario.WaveDefinition;

/**
 * Plays the wave script: starts waves, fires their events on time, notices when
 * a wave is over, and declares the win after the last one.
 *
 * <p>Rules: the first wave starts {@link GameConstants#FIRST_WAVE_DELAY_TICKS}
 * ticks into the game. A wave is over once all its events have fired and all
 * the campaigns it started have ended. The next wave starts
 * {@link GameConstants#WAVE_GAP_TICKS} ticks later.
 */
class WaveRunner {

    private final Simulation simulation;
    private final List<WaveDefinition> waves;
    private final List<AttackCampaign> waveCampaigns = new ArrayList<>();

    private int waveIndex;
    private int nextWaveStartTick = GameConstants.FIRST_WAVE_DELAY_TICKS;
    private int waveStartTick;
    private int eventsFired;
    private boolean waveActive;

    WaveRunner(Simulation simulation, List<WaveDefinition> waves) {
        this.simulation = simulation;
        this.waves = List.copyOf(waves);
    }

    /**
     * Does this tick's wave work. Called by the simulation after the campaigns
     * have acted, so a campaign started now takes its first step next tick.
     */
    void tick(int tick) {
        if (!waveActive) {
            if (waveIndex >= waves.size() || tick < nextWaveStartTick) {
                return;
            }
            startWave(tick);
        }
        fireDueEvents(tick);
        if (isWaveOver()) {
            endWave(tick);
        }
    }

    private void startWave(int tick) {
        waveActive = true;
        waveStartTick = tick;
        eventsFired = 0;
        waveCampaigns.clear();
        simulation.publish(new GameEvent.WaveStarted(waveIndex + 1, waves.size(),
                currentWave().name()));
    }

    private void fireDueEvents(int tick) {
        List<ScriptedEvent> events = currentWave().events();
        while (eventsFired < events.size()
                && waveStartTick + events.get(eventsFired).tickOffset() <= tick) {
            fire(events.get(eventsFired));
            eventsFired++;
        }
    }

    private void fire(ScriptedEvent event) {
        switch (event) {
            case ScriptedEvent.StartCampaign start -> launch(start);
            case ScriptedEvent.ScriptedAlert alert -> raise(alert);
        }
    }

    private void launch(ScriptedEvent.StartCampaign start) {
        Network network = simulation.getNetwork();
        AttackCampaign campaign = new AttackCampaign(start.sourceIp(),
                network.getDevice(start.entryDeviceId()),
                network.getDevice(start.targetDeviceId()), start.strategy());
        waveCampaigns.add(campaign);
        if (start.withRecon()) {
            simulation.startCampaignWithRecon(campaign);
        } else {
            simulation.startCampaign(campaign);
        }
    }

    private void raise(ScriptedEvent.ScriptedAlert alert) {
        Device device = simulation.getNetwork().getDevice(alert.deviceId());
        for (String line : alert.logLines()) {
            simulation.log(device, line);
        }
        simulation.raiseAlert(alert.type(), alert.sourceIp(), device, alert.severity(),
                alert.summary(), alert.falsePositive());
    }

    private boolean isWaveOver() {
        boolean allFired = eventsFired == currentWave().events().size();
        return allFired && waveCampaigns.stream().noneMatch(AttackCampaign::isActive);
    }

    private void endWave(int tick) {
        waveActive = false;
        simulation.publish(new GameEvent.WaveEnded(waveIndex + 1));
        waveIndex++;
        if (waveIndex == waves.size()) {
            simulation.endGame(true);
        } else {
            nextWaveStartTick = tick + GameConstants.WAVE_GAP_TICKS;
        }
    }

    private WaveDefinition currentWave() {
        return waves.get(waveIndex);
    }

    /** @return how many waves have been completed */
    int getWavesCompleted() {
        return waveIndex;
    }

    /** @return the number of the running or next wave, starting at 1, capped at the total */
    int getCurrentWaveNumber() {
        return Math.min(waveIndex + 1, waves.size());
    }

    /** @return how many waves the script has */
    int getTotalWaves() {
        return waves.size();
    }
}
