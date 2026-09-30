# Phase 2 notes: attack simulation

## What was built

- Attack campaigns that move through five stages (State pattern). The break-in method is
  pluggable (Strategy pattern).
- Alerts, device logs and the network health drain.
- `Simulation.tick()`, which publishes `GameEvent`s to listeners (Observer pattern).
- 46 new tests, 78 in total.

## Classes and their single responsibility

### `nl.tue.sysadminpanic.model` (additions)

| Class | Responsibility |
|-------|----------------|
| `Severity` | Enum of alert severities: LOW, MEDIUM, HIGH, CRITICAL. |
| `AlertType` | Enum of alert kinds, each with a display label (an enum with a field). *Not named in the spec; added so `Alert.type` is type-safe instead of a free-form string.* |
| `Alert` | Record for one immutable alert. `falsePositive` is never shown in the UI. |
| `NetworkHealth` | Holds the health value, applies drain (clamped at 0), and calculates the drain for one tick. |
| `GameTime` | Turns a tick into `HH:MM:SS` clock time, starting at 09:00:00. *Not named in the spec; the log format and the status bar both need it.* |
| `LogEntry` | Gained `formatted()`, which returns `"09:00:15  Failed login: ..."`. |
| `Network` | Gained `getFirstOfType(type)`, which attacks use to find the INTERNET and the FIREWALL. |

### `nl.tue.sysadminpanic.attack`

| Class | Responsibility |
|-------|----------------|
| `AttackContext` | Interface listing everything an attack may do to the world: log, raise an alert, compromise a device, report exfiltration, report that it ended. *Not named in the spec; see "Why AttackContext" below.* |
| `AttackCampaign` | One attacker. It holds the current stage, the devices the attacker controls, and its status. It hands each tick to its stage. |
| `CampaignStatus` | Enum: ACTIVE, STOPPED, CONTAINED, FINISHED. *Not named in the spec; the spec talks about stopped, contained and finished campaigns, and this enum records which one happened.* |
| `AttackStage` | The State interface: `enter()` (runs once) and `tick()` (runs every tick). |
| `ReconStage` | Port scan. Raises a LOW alert on the FIREWALL, logs 3 probes per tick there, and lasts 5 ticks. |
| `InitialAccessStage` | Counts attempts and delegates everything else to the strategy. |
| `CompromiseStage` | A momentary stage. It compromises the entry device and moves straight on. |
| `LateralMovementStage` | Hops along `shortestPath` toward the target. It is contained when no path exists. |
| `ExfiltrationStage` | Sends data out for 20 ticks. It pauses while there is no route to the INTERNET. |
| `InitialAccessStrategy` | The Strategy interface: how long the break-in takes, what it logs, whether patching helps. |
| `BruteForceStrategy` | Raises a HIGH alert and logs a failed login every tick. It fails against patched devices. |
| `PhishingStrategy` | Raises no alert and succeeds after 3 ticks. It leaves one suspicious process line in the log. |
| `LogTemplates` | All fixed log and alert texts in one place. |

### `nl.tue.sysadminpanic.sim`

| Class | Responsibility |
|-------|----------------|
| `Simulation` | Runs ticks, owns campaigns, alerts and health, and publishes events. It implements `AttackContext`. |
| `GameEvent` | A sealed interface. Its nested records are the event types: `TickAdvanced`, `AlertRaised`, `DeviceStateChanged`, `HealthChanged`, `CampaignEnded`, `GameOver`. The `MoneyChanged`, `ActionPerformed` and wave events arrive in phases 3 and 4. |
| `GameEventListener` | The Observer interface, with one method: `onEvent(GameEvent)`. |

## How the classes interact

One call to `Simulation.tick()` does this:

```
Simulation.tick()
├─ currentTick++ ; publish TickAdvanced
├─ for each campaign: campaign.tick(this)       // "this" is the AttackContext
│    └─ stage.tick(campaign, context)
│         e.g. InitialAccessStage: attempts++ ; on the final attempt
│              strategy.canBreakInto(device)?
│              ├─ yes → strategy.logSuccess(...) ; campaign.setStage(new CompromiseStage())
│              │         └─ CompromiseStage.enter → campaign.takeControl(device)
│              │               └─ context.compromise(device) → publish DeviceStateChanged
│              │            → campaign.setStage(new LateralMovementStage())
│              └─ no  → strategy.logFailure(...) ; campaign.end(CONTAINED) → publish CampaignEnded
└─ updateHealth(): drain = NetworkHealth.drainPerTick(devices, exfiltrationsThisTick)
     └─ publish HealthChanged ; if health is 0 → publish GameOver(false)
```

### Timeline of an unattended phishing attack on PC-02, started at tick 0

| Tick | What happens |
|------|--------------|
| 3 | Phishing succeeds. PC-02 is compromised. The only trace is one log line on PC-02. |
| 13 | Hop to ROUTER (10 ticks later). |
| 23 | Hop to SERVER-01. |
| 33 | Hop to DB-01, then switch to exfiltration. |
| 34–53 | 20 chunks of data are sent out, draining 1.3 + 1.0 per tick. |
| 53 | The campaign is FINISHED. Health is 39.7. |
| 84 | Health reaches 0 from the compromised devices alone, and the game is lost. |

These exact numbers are checked in `AttackSimulationTest` and `SimulationTest`.

## Design patterns (advanced topic 2)

### State: `AttackCampaign` + `AttackStage`

- **Context:** `AttackCampaign`. It only stores the current stage and forwards ticks:
  `stage.tick(this, context)`. It contains no `if (stage == RECON)` logic.
- **States:** the five stage classes. Each one knows only its own behaviour and its own
  successor. For example, `ReconStage` ends by calling
  `campaign.setStage(new InitialAccessStage(), ...)`.
- **Why it fits:** an attack really is a sequence of phases with different behaviour. The
  alternative is one big `switch` on a stage enum inside the campaign, which would grow with
  every stage and mix all their fields (scan counter, attempt counter, hop counter, chunk
  counter) in one class. Here each stage owns its own counter.
- **`enter()` vs `tick()`:** `enter` runs once, immediately, when the stage is switched to.
  `tick` runs on every later tick. `setStage` assigns the field *before* calling `enter`, so a
  stage can move straight on from inside its own `enter`. `CompromiseStage` relies on this:
  it is a real state in the model but lasts zero ticks. That is why the brute-force test sees
  the server compromised on *exactly* tick 15, not tick 16.

### Strategy: `InitialAccessStrategy`

- `InitialAccessStage` owns the part the two methods share: counting attempts and deciding
  between success and failure on the final tick.
- The strategy owns the parts that differ: the duration (15 or 3), the alert (HIGH or none),
  the logging, and `canBreakInto(device)` (brute force fails if the device is patched;
  phishing always works).
- **Why it fits:** "how the attacker gets in" is an interchangeable algorithm chosen when the
  campaign is created (`new AttackCampaign(ip, entry, target, new PhishingStrategy())`).
  Adding, say, a USB-stick attack is one new class, with no change to any stage.
- **State vs Strategy, a likely tutor question:** both delegate to an object behind an
  interface. The difference is *who changes it and when*. A campaign's **state** changes
  itself over time (stages switch to their successors). A campaign's **strategy** is chosen
  once from outside and never changes.

### Observer: `Simulation` → `GameEventListener`

- `Simulation` is the subject. It keeps a list of listeners, and `publish(event)` calls
  `onEvent` on each of them, synchronously.
- `GameEvent` is a `sealed` interface with record implementations. A listener (the UI, in
  phase 5) can write `switch (event) { case GameEvent.AlertRaised a -> ...; case ... }`, and
  the compiler checks that every case is handled.
- The model never imports Swing. In the tests, the listener is simply `events::add`, which
  records everything that happened.

## Why AttackContext exists

The stages need to change the world: write logs, raise alerts, compromise devices. They
could have received the `Simulation` directly, but then:

- the `attack` package would depend on `sim`, and `sim` already depends on `attack`
  (a circular dependency);
- a stage could call anything on `Simulation`, even `tick()`.

The `AttackContext` interface lists only the six things an attack may do, and `Simulation`
implements it. Every change an attack makes therefore passes through the simulation, which
is exactly where the matching event gets published. This is the Dependency Inversion
Principle: the `attack` package defines the interface it needs, and the higher-level `sim`
package provides it.

## Things to study before explaining this to a tutor

- **Hidden information:** `compromise()` changes only `trueState`. `raiseAlert()` changes only
  `knownState`, and only from HEALTHY to SUSPICIOUS. Phishing raises no alert, so a phished PC
  still *looks* healthy (see the test `silentCompromiseDoesNotChangeKnownState`). Only
  Inspect (phase 3) reveals the truth.
- **The path is recomputed every tick** in `LateralMovementStage`. That single design choice
  handles "if the path is broken, recompute" and "if no path exists, contained", with no
  extra bookkeeping. The cost is one BFS per campaign per tick, which is trivial on 8 devices.
- **The hop counter is not reset when the path changes.** If the route switches to a detour,
  the attacker keeps the progress it has made toward its next hop.
- **Exfiltration is reported, not stored.** `ExfiltrationStage` calls
  `context.reportExfiltration()`. The simulation counts these calls during the tick and uses
  the count for the drain. This avoids an off-by-one: the 20th chunk still drains health on
  the tick it is sent, even though the campaign ends in that same tick.
- **The list of campaigns is copied before iterating** (`List.copyOf(campaigns)`), so a
  campaign that causes a change during its turn cannot trigger a
  `ConcurrentModificationException`.
- **Floating point:** the drain values (0.1 × criticality) are not exact in binary, so the
  tests compare health with a tolerance (`assertEquals(39.7, health, 1e-9)`).
- **Ended campaigns stay in the list.** Phase 4's statistics need them, and `tick()` on an
  ended campaign does nothing.

## Decisions made on ambiguous points

1. **A brute force against a patched device gives up at tick 15** and the campaign ends as
   CONTAINED. *(You chose this.)*
2. **Recon alert target = FIREWALL**, because that is where the scan is logged. Selecting that
   alert in the UI will select the firewall.
3. **Exfiltration drain is 1.0 per exfiltrating campaign.** If both wave-5 campaigns reach
   DB-01, that is 2.0 per tick.
4. **Alerts are raised once, when a stage or strategy starts**, not every tick. Otherwise a
   15-tick brute force would fill the alert list with 15 identical HIGH alerts.
5. **A paused exfiltration waits indefinitely**, as the spec says. The campaign stays active,
   so the wave won't end, until the route comes back or the player Restores the attacker's
   devices (phase 3). **Revisit this in the phase 6 balancing pass** if playtesting shows it
   feels bad.
6. **Contained campaigns leave their compromised devices compromised**, and those devices keep
   draining health until they are Restored (phase 3).

## Prepared for phase 3

- `AttackCampaign.getControlledDevices()` supports "Restore every device it controls".
- `getCurrentDevice() == null` means the attacker is still outside, which is exactly when
  Block IP works.
- `CampaignStatus.STOPPED` is ready for Block IP.

## Suggested commit message

```
Phase 2: attack simulation with State, Strategy and Observer

- AttackCampaign with Recon, InitialAccess, Compromise, LateralMovement
  and Exfiltration stages (State pattern)
- BruteForce and Phishing initial-access strategies (Strategy pattern)
- Alerts, log templates, hidden true/known device state
- NetworkHealth drain from compromised devices, isolation and exfiltration
- Simulation.tick() publishing sealed GameEvent records (Observer pattern)
- 46 new tests for exact attack timings, containment and health
```
