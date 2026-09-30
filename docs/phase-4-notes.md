# Phase 4 notes: scenario and game flow

## What was built

- The Office Network, built with a **Builder**.
- The 5-wave script, written as plain data.
- The wave runner: wave timing, the win condition and the end-of-game statistics.
- A headless test that plays the **whole game** twice (once defended well, once with no
  actions at all) and checks the exact results.

## Classes and their single responsibility

### `nl.tue.sysadminpanic.scenario` (new package)

| Class | Responsibility |
|-------|----------------|
| `Scenario` | Interface for a playable level: a name, `createNetwork()` and `getWaves()`. |
| `NetworkBuilder` | Builder: records `device(...)` and `link(...)` calls, then `build()` creates a fresh `Network`. |
| `OfficeNetworkScenario` | The one level: its network layout and its 5 waves. |
| `WaveDefinition` | Record `(name, events)`. It checks that events are sorted by tick offset. |
| `ScriptedEvent` | A sealed interface with two record types. `StartCampaign` launches an attacker. `ScriptedAlert` raises a hint or false-positive alert and writes its log lines. |

### `nl.tue.sysadminpanic.sim` (additions)

| Class | Responsibility |
|-------|----------------|
| `WaveRunner` | Starts waves, fires their events on time, detects when a wave ends, and declares the win after wave 5. It is package-private: only `Simulation` uses it. |
| `GameStats` | Record holding the numbers for the end summary. |
| `Simulation` | Gains `forScenario(scenario)`, the win and lose handling (`endGame`), `getStats()` and the wave getters. It also tracks which devices were ever compromised. |
| `GameEvent` | New events: `WaveStarted(number, total, name)` and `WaveEnded(number)`. |

### Small additions

- `AttackCampaign.markDetected()` / `isDetected()`, used for the "threats detected" statistic.
- `CampaignStatus.isDefeated()`: true for STOPPED or CONTAINED, used for the
  "threats contained" statistic.
- `InspectAction` marks as detected every campaign that controls the inspected device.

## How the classes interact

```java
Simulation sim = Simulation.forScenario(new OfficeNetworkScenario());
//  -> scenario.createNetwork()  (NetworkBuilder...build(): a brand-new network)
//  -> new WaveRunner(sim, scenario.getWaves())
```

Order of work in `Simulation.tick()`:

```
tick()
├─ currentTick++ ; publish TickAdvanced
├─ payIncome()
├─ advanceCampaigns()          existing attackers act
├─ updateHealth()              may call endGame(false)
└─ if not over: waveRunner.tick(currentTick)
     ├─ wave not running and it's time? → startWave → publish WaveStarted
     ├─ fireDueEvents: every event whose (waveStart + offset) <= now, in list order
     │    switch (event) {
     │      case StartCampaign s -> new AttackCampaign(...) ; sim.startCampaign[WithRecon]
     │      case ScriptedAlert a -> write log lines ; sim.raiseAlert(..., falsePositive)
     │    }
     └─ all events fired and all this wave's campaigns ended?
          → publish WaveEnded ; last wave? endGame(true) : next wave starts in 15 ticks
```

**Why the waves run last in the tick:** a campaign launched at tick T takes its first step
at T+1, exactly like the campaigns the tests start between ticks. As a result, "brute force
succeeds after 15 ticks" means the same thing everywhere. It also means a game lost on
health in tick T cannot be "won" later in that same tick.

## Design patterns (advanced topic 2)

### Builder: `NetworkBuilder`

- **Problem it solves:** building a `Network` by hand needs a variable for every device,
  then `addDevice` and `addLink` calls that refer to those variables (see `OfficeFixture` in
  the tests). That is noisy and easy to get wrong.
- **How:** each `device(...)` and `link(...)` call only *records* a small definition (the
  private `DeviceSpec` and `LinkSpec` records) and returns `this`, so the calls chain.
  `build()` turns the definitions into real objects.
- **Why recording first matters:**
  1. Links can refer to devices by name, in any order.
  2. Every `build()` makes a brand-new network, which is exactly what "Play again" needs:
     the scenario can hand out a fresh network each game.
  3. The complex object is never visible half-built.

### Also visible in this phase

- **Strategy again:** a `ScriptedEvent.StartCampaign` carries the `InitialAccessStrategy`
  to use. The wave script chooses the attack method; the engine never checks which one it is.
- **Sealed interface plus pattern-matching `switch`** in `WaveRunner.fire()`. There is no
  `default` branch, so if someone adds a third kind of `ScriptedEvent`, the code will not
  compile until the runner handles it.
- **Open/Closed principle:** `Simulation` depends only on the `Scenario` interface. A second
  level is a new class; the engine does not change.

## Why WaveRunner is a separate class

The spec lists the wave runner as part of `Simulation`. Putting its six fields and eight
methods directly into `Simulation` would push that class toward a god class, and CLAUDE.md
forbids those. So the wave logic lives in `WaveRunner`, which `Simulation` owns and calls once
per tick. It is package-private and talks back through two package-private methods,
`publish()` and `endGame()`, so nothing outside `sim` can see it.

## The wave script (`OfficeNetworkScenario.getWaves()`)

| Wave | Starts at tick* | Events |
|------|-----------------|--------|
| 1 Knock knock | 10 | One campaign from 203.0.113.42: port scan (t+0 to t+5), then brute force on SERVER-01 |
| 2 Special delivery | … | Phishing on PC-02 from 203.0.113.66 (t+0); MEDIUM "unusual outbound" alert on PC-02 (t+15) |
| 3 Crying wolf | … | False-positive LOW scan alert on SERVER-01 (t+0); port scan + brute force on PC-01 from 198.51.100.23 (t+10) |
| 4 Two fronts | … | Brute force on SERVER-01 from 203.0.113.77 and phishing on PC-03 from 198.51.100.140 (both t+0) |
| 5 The big one | … | Phishing on PC-01 from 203.0.113.201 (t+0); brute force on SERVER-01 from 198.51.100.99 (t+5); false-positive "mistyped password" on PC-02 (t+10) |

\* Later waves start 15 ticks after the previous one ends, so their start depends on the player.

Choices made where the spec was silent:

- **Wave 1 is one campaign, not two.** The spec lists "t+0 port scan" and "t+5 brute force
  from the same IP". Recon lasts exactly 5 ticks and is followed by the brute force, so one
  campaign with recon produces exactly that. One Block IP stops it.
- **IPs for the phishing waves** (2, 4 and 5) are not in the spec. I chose addresses from the
  same documentation ranges.
- **False positives carry log lines.** Inspecting the device then shows why the alert is
  harmless: "scheduled vulnerability scan (IT security, change ticket #4471)" or
  "(caps lock was on)". The mistyped-password alert uses the internal address 10.0.0.12,
  because it comes from an employee, not an attacker.
- **INTERNET criticality = 1.** This was the open question from phase 1. It never matters,
  because INTERNET cannot be compromised or isolated.

## End-of-game statistics: definitions

| Statistic | Definition |
|-----------|------------|
| waves survived | waves completed (5 on a win) |
| threats detected | campaigns the SOC had evidence of: a real (non-false-positive) alert with that campaign's IP, *or* the player inspected a device it controlled |
| threats contained | campaigns that ended STOPPED or CONTAINED |
| systems compromised | distinct devices compromised at least once (a re-compromise is not counted twice) |
| money spent | total dollars paid for actions |

## The full-game test (`FullGameTest`)

`ScriptedPlayer` (a test helper) performs a fixed list of actions at fixed ticks, each right
after that tick is processed. Two games are played:

- **The sensible defender** wins at tick 112 with 65.9% health, spending $1,300, with
  7 of 7 threats contained. The Javadoc in the test explains each move.
- **Doing nothing** loses at tick 110, during wave 2's exfiltration.

These exact numbers are possible only because the game has no randomness (CLAUDE.md rule 6).

## Balancing observations for phase 6

The playthroughs show three things worth tuning in phase 6. That phase may only change
`GameConstants` and the wave script.

1. **The wave-2 hint is late.** The alert comes at t+15, but the attacker hops from PC-02 to
   ROUTER at t+13. The lesson "isolate before it spreads" cannot be learned from the alert
   alone. One fix is to move the alert to around t+8.
2. **One firewall rule, ROUTER to SERVER-01 ($500), wins most of the game.** Every phishing
   attack starts on a PC and must pass that link, so after wave 2 the phishing attacks of
   waves 4 and 5 are contained automatically, and the player never needs to notice them.
   Possible fixes: give PC-to-server links an alternative route, or make firewall rules
   more expensive.
3. **Compromised devices left behind drain health forever.** In the sensible run, PC-02 and
   ROUTER drain 0.4 per tick for about 70 ticks. It is not fatal, but Restore at $1,500 is
   almost never affordable. Consider a cheaper Restore or a lower compromised drain.

## Suggested commit message

```
Phase 4: Office Network scenario, 5-wave script, win/lose and stats

- Scenario interface, NetworkBuilder (Builder pattern), OfficeNetworkScenario
- WaveDefinition and sealed ScriptedEvent records (campaigns, alerts, false positives)
- WaveRunner: wave timing, event firing, win after the last wave
- GameStats for the end summary; detection and containment tracking
- Headless full-game tests: a scripted defender wins, doing nothing loses
```
