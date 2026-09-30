package nl.tue.sysadminpanic.action;

import java.util.List;
import nl.tue.sysadminpanic.attack.AttackCampaign;
import nl.tue.sysadminpanic.attack.AttackContext;
import nl.tue.sysadminpanic.model.Device;

/**
 * Everything a player action is allowed to see and do in the game world.
 *
 * <p>It extends {@link AttackContext} because actions sometimes need the same
 * operations as attacks, such as ending a campaign or writing a log line. The
 * simulation implements it, so every change still produces an event.
 */
public interface ActionContext extends AttackContext {

    /** @return the current simulation tick */
    int getCurrentTick();

    /** @return every campaign started so far, including ended ones */
    List<AttackCampaign> getCampaigns();

    /**
     * @param ip an IP address
     * @return true if the player has blocked this IP
     */
    boolean isIpBlocked(String ip);

    /**
     * Adds an IP to the block list. Campaigns started later from it are stopped at once.
     *
     * @param ip the IP address to block
     */
    void blockIp(String ip);

    /**
     * Tells listeners that an action changed a device, so the map can be redrawn.
     *
     * @param device the device that changed
     */
    void deviceChanged(Device device);
}
