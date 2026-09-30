package nl.tue.sysadminpanic.sim;

import java.util.ArrayList;
import java.util.List;
import nl.tue.sysadminpanic.attack.AttackCampaign;
import nl.tue.sysadminpanic.attack.BruteForceStrategy;
import nl.tue.sysadminpanic.attack.PhishingStrategy;
import nl.tue.sysadminpanic.model.Device;
import org.junit.jupiter.api.BeforeEach;

/**
 * Shared setup for the player-action tests: a fresh office network, a recorded
 * event list, and helpers to start the two kinds of attack.
 */
abstract class ActionTestBase {

    static final String IP = "203.0.113.42";

    OfficeFixture office;
    Simulation sim;
    final List<GameEvent> events = new ArrayList<>();

    @BeforeEach
    void setUpOffice() {
        office = new OfficeFixture();
        sim = office.sim;
        sim.addListener(events::add);
    }

    AttackCampaign startPhishing(Device entry) {
        AttackCampaign campaign = new AttackCampaign(IP, entry, office.db, new PhishingStrategy());
        sim.startCampaign(campaign);
        return campaign;
    }

    AttackCampaign startBruteForce(Device entry) {
        AttackCampaign campaign = new AttackCampaign(IP, entry, office.db, new BruteForceStrategy());
        sim.startCampaign(campaign);
        return campaign;
    }

    <T extends GameEvent> List<T> eventsOfType(Class<T> type) {
        return events.stream().filter(type::isInstance).map(type::cast).toList();
    }
}
