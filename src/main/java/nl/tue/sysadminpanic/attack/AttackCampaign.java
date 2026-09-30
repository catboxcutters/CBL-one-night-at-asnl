package nl.tue.sysadminpanic.attack;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import nl.tue.sysadminpanic.model.Device;

/**
 * One attacker working through the stages of an attack.
 *
 * <p>This is the <em>context</em> of the State pattern: it holds the current
 * {@link AttackStage} and hands every tick to it. The stage decides what happens
 * and when to switch to the next stage, so this class contains no
 * stage-specific logic at all.
 */
public class AttackCampaign {

    private final String sourceIp;
    private final Device entryDevice;
    private final Device target;
    private final InitialAccessStrategy accessStrategy;
    private final Set<Device> controlledDevices = new LinkedHashSet<>();

    private AttackStage stage;
    private Device currentDevice;
    private CampaignStatus status = CampaignStatus.ACTIVE;
    private boolean detected;

    /**
     * Creates a campaign. It does nothing until one of the {@code begin} methods is called.
     *
     * @param sourceIp       the attacker's IP address
     * @param entryDevice    the device the attacker tries to break into first
     * @param target         the device the attacker wants to steal data from
     * @param accessStrategy how the attacker breaks into the entry device
     */
    public AttackCampaign(String sourceIp, Device entryDevice, Device target,
            InitialAccessStrategy accessStrategy) {
        this.sourceIp = sourceIp;
        this.entryDevice = entryDevice;
        this.target = target;
        this.accessStrategy = accessStrategy;
    }

    /**
     * Starts the attack with a port scan, followed by the break-in.
     *
     * @param context the game world
     */
    public void beginWithRecon(AttackContext context) {
        setStage(new ReconStage(), context);
    }

    /**
     * Starts the attack directly with the break-in, skipping the port scan.
     *
     * @param context the game world
     */
    public void beginWithInitialAccess(AttackContext context) {
        setStage(new InitialAccessStage(), context);
    }

    /**
     * Advances the attack by one tick. Does nothing once the campaign has ended.
     *
     * @param context the game world
     */
    public void tick(AttackContext context) {
        if (isActive()) {
            stage.tick(this, context);
        }
    }

    /**
     * Switches to a new stage and lets it run its entry logic immediately.
     * The field is assigned before {@code enter} runs, so a stage may switch
     * straight on to another stage from inside its own {@code enter}.
     */
    void setStage(AttackStage newStage, AttackContext context) {
        stage = newStage;
        stage.enter(this, context);
    }

    /**
     * Ends the campaign. Only the first call has an effect.
     *
     * @param endStatus how the campaign ended; must not be ACTIVE
     * @param context   the game world, informed that the campaign ended
     */
    public void end(CampaignStatus endStatus, AttackContext context) {
        if (!isActive()) {
            return;
        }
        status = endStatus;
        context.campaignEnded(this);
    }

    /**
     * Compromises a device and makes it the attacker's current position.
     */
    void takeControl(Device device, AttackContext context) {
        context.compromise(device);
        controlledDevices.add(device);
        currentDevice = device;
    }

    /**
     * Takes a device away from the attacker, because the player restored it.
     *
     * <p>With no devices left, the attacker is out of the network and the
     * campaign is stopped. If the attacker was standing on the restored device,
     * it falls back to the device it took most recently and has to fight its way
     * forward again, so any progress towards the next hop or on stealing data is lost.
     *
     * @param device  the restored device
     * @param context the game world
     */
    public void loseControl(Device device, AttackContext context) {
        if (!controlledDevices.remove(device) || !isActive()) {
            return;
        }
        if (controlledDevices.isEmpty()) {
            end(CampaignStatus.STOPPED, context);
        } else if (device == currentDevice) {
            currentDevice = lastControlledDevice();
            setStage(new LateralMovementStage(), context);
        }
    }

    private Device lastControlledDevice() {
        Device last = null;
        for (Device device : controlledDevices) {
            last = device;
        }
        return last;
    }

    /**
     * Records that the security team has evidence of this campaign: a real alert
     * about it was raised, or the player inspected a device it had compromised.
     * Used only for the end-of-game statistics.
     */
    public void markDetected() {
        detected = true;
    }

    /** @return true if the security team ever had evidence of this campaign */
    public boolean isDetected() {
        return detected;
    }

    /** @return true while the attacker has not yet gained access to any device */
    public boolean isOutside() {
        return currentDevice == null;
    }

    /** @return the attacker's IP address */
    public String getSourceIp() {
        return sourceIp;
    }

    /** @return the device the attacker breaks into first */
    public Device getEntryDevice() {
        return entryDevice;
    }

    /** @return the device the attacker wants to steal data from */
    public Device getTarget() {
        return target;
    }

    /** @return how the attacker breaks into the entry device */
    public InitialAccessStrategy getAccessStrategy() {
        return accessStrategy;
    }

    /** @return the current stage, or null before the campaign has begun */
    public AttackStage getStage() {
        return stage;
    }

    /** @return the device the attacker is on, or null while still outside the network */
    public Device getCurrentDevice() {
        return currentDevice;
    }

    /** @return every device this attacker has compromised, in order (read-only) */
    public Set<Device> getControlledDevices() {
        return Collections.unmodifiableSet(controlledDevices);
    }

    /** @return whether the campaign is running or how it ended */
    public CampaignStatus getStatus() {
        return status;
    }

    /** @return true while the campaign is still running */
    public boolean isActive() {
        return status == CampaignStatus.ACTIVE;
    }
}
