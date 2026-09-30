# Phase 3 notes: player actions and money

## What was built

- The six player actions as Command objects, plus Reconnect to undo Isolate.
- Money: $1,500 to start, +$50 every 5 ticks, a price per action.
- Validity checks, with human-readable reasons the UI can show as tooltips.
- Restore takes devices away from attackers; Block IP stops attackers who are still outside.
- 51 new tests, 129 in total.

## Classes and their single responsibility

### `nl.tue.sysadminpanic.action` (new package)

| Class | Responsibility |
|-------|----------------|
| `PlayerAction` | The Command interface: `getName()`, `getPrice()`, `checkValid(context)`, `execute(context)`. |
| `ActionResult` | Record `(success, message)` with the factories `success(...)` and `failure(...)`. |
| `ActionContext` | What actions may see and do: campaigns, the IP block list, the current tick, and "a device changed". It **extends `AttackContext`**, so actions can also end campaigns and log. |
| `DeviceAction` | Abstract base for actions aimed at one device. It stores the device and applies the shared rule "INTERNET can't be targeted". |
| `InspectAction` | Free. Sets `knownState = trueState` and reveals the log up to now. |
| `BlockIpAction` | $200. Blocks an IP and stops that IP's campaigns that are still outside. |
| `IsolateAction` | $300. Sets the device's `isolated` flag. |
| `ReconnectAction` | Free. Clears `isolated`. *Not one of the six buttons: the Isolate button becomes "Reconnect" on an isolated device.* |
| `PatchAction` | $800. Sets `patched`. |
| `FirewallRuleAction` | $500. Blocks the link between the selected device and one neighbor. |
| `RestoreAction` | $1,500. Makes the device HEALTHY and takes it away from every campaign. |

### Changes elsewhere

| Class | Change |
|-------|--------|
| `model.Wallet` (new) | Balance, `canAfford`, `spend` (throws if unaffordable), `earn`, total spent (for the end summary), and `format(1500)`, which returns `"$1,500"`. |
| `model.Device` | Remembers the tick of its last inspection. `getRevealedLog()` returns only the lines up to that tick. |
| `attack.AttackCampaign` | Adds `loseControl(device)` (used by Restore) and `isOutside()` (used by Block IP). |
| `sim.Simulation` | Owns the `Wallet` and the blocked-IP set. Pays income, and gains `checkAllowed(action)` and `perform(action)`. Now implements `ActionContext`. |
| `sim.GameEvent` | New events: `MoneyChanged(balance)` and `ActionPerformed(result)`. |
| `GameConstants` | Adds `PRICE_RECONNECT = 0`, so even "free" lives in one place. |

## How the classes interact

The UI (phase 5) will do the equivalent of this:

```java
PlayerAction action = new IsolateAction(selectedDevice);
button.setEnabled(sim.checkAllowed(action).isEmpty());   // tooltip = the reason
...
ActionResult result = sim.perform(action);               // on click
```

`Simulation.perform(action)` runs these steps:

```
perform(action)
├─ checkAllowed(action)
│    ├─ game over?            → "The game is over"
│    ├─ action.checkValid(this) → e.g. "PC-01 is already isolated"
│    └─ wallet.canAfford?     → "Costs $800, you have $700"
├─ if a reason exists: result = failure(reason)        (nothing is charged, nothing changes)
├─ else: wallet.spend(price) → publish MoneyChanged
│        result = action.execute(this)
│            e.g. RestoreAction: device → HEALTHY
│                 for each campaign: campaign.loseControl(device, context)
│                 context.deviceChanged(device) → publish DeviceStateChanged
└─ publish ActionPerformed(result)                     (for the event feed, even on failure)
```

## Design pattern: Command (advanced topic 2)

- **Command:** `PlayerAction`. Each concrete action is an object that holds *everything needed
  to carry out one request*: its target (passed to the constructor), its price, its validity
  rule and its effect.
- **Invoker:** `Simulation.perform()`. It treats every action the same way: check it, charge
  it, run it, publish the result. It contains no `switch` over action types.
- **Receiver:** the model objects the command changes (`Device`, `Link`, `AttackCampaign`),
  reached through `ActionContext`.
- **Client:** the UI (phase 5), which creates action objects from what the player selected.
- **Why it fits:** all six actions share the same life cycle (validate, pay, execute, give
  feedback). Command lets that life cycle be written once in `perform()`. The same objects
  also answer the UI's question "may I enable this button, and if not, why?" via
  `checkAllowed()`. Adding a seventh action means adding one class; neither `perform()` nor
  the button logic changes.
- **What a tutor may ask: is this "real" Command without undo?** Yes. The core of the pattern
  is turning a request into an object, so it can be validated, passed around, and executed by
  code that doesn't know its concrete type. Undo is an optional extension. Here, Reconnect is
  a separate command rather than an `undo()` method, because the spec prices them differently
  (Isolate costs money, Reconnect is free).

## Other OOP points worth explaining

- **`DeviceAction` is an abstract base class** with a `final` `checkValid()` that first applies
  the shared INTERNET rule and then calls the overridable hook `checkDevice()`. This is the
  Template Method idea on a small scale: subclasses can add checks but cannot skip the shared
  one. `BlockIpAction` targets an IP, not a device, so it implements `PlayerAction` directly.
- **`ActionContext extends AttackContext`:** the same reasoning as `AttackContext` in phase 2.
  The `action` package defines what it needs, and `Simulation` provides it. This avoids a
  circular dependency: `sim` depends on `action`, but `action` never depends on `sim`.
  Package dependencies run one way:
  `ui → sim → action → attack → model`.
- **Money is checked by the invoker, not the commands.** Affordability is the same rule for
  every action, so it lives in one place (`Simulation.checkAllowed`). Each action only checks
  what is specific to it.

## Behaviour details and decisions

1. **Order of checks:** game over, then the action's own validity, then money. "PC-01 is
   already isolated" is more useful than "you can't afford it" when both are true.
2. **Inspect shows a snapshot.** The known state and the revealed log both reflect the moment
   of the last inspection. New attacker activity stays hidden until you inspect again.
   Inspect is free, so this costs only a click, and it keeps the "hidden information" idea
   consistent.
3. **Restore on the attacker's current device** pushes the attacker back to the device it
   took most recently. The attacker must redo a full hop to come forward again. If it was
   exfiltrating, it drops back to lateral movement, and exfiltration restarts from 0 when it
   returns. Restoring its **last** device stops the campaign (status STOPPED).
4. **Restore is always allowed** on any non-INTERNET device, even one that is really healthy.
   Disabling it for healthy devices would leak hidden information for free.
5. **Block IP only affects attackers still outside** (`isOutside()`). An attacker inside no
   longer uses its original connection. A blocked IP also stops any *later* campaign from that
   IP before it begins. The current wave script never reuses an IP, but that is what
   "blocking an IP" means.
6. **Block IP messages reveal only what the SOC can observe:** "incoming attack cut off" or
   "no active connections from it". Wave 3's false positive teaches that paying $200 to block
   a harmless scanner is a waste.
7. **Patching a compromised device does not remove the attacker.** Only Restore does.
8. **A firewall rule may cut FIREWALL ↔ INTERNET**, because the selected device (FIREWALL) is
   allowed and INTERNET is its neighbor. That stops all exfiltration for $500 with no health
   drain, which may be too strong. **Check this in the phase 6 balancing pass.**

## Open question for phase 5

**Where does Block IP get its IP from?** The inspector's buttons act on the selected device,
but Block IP needs an IP address. The natural choice is the source IP of the currently selected
alert (the button would be disabled when no alert is selected). This will be settled when the
UI is built.

## Suggested commit message

```
Phase 3: player actions (Command pattern) and money

- PlayerAction commands: Inspect, Block IP, Isolate/Reconnect, Patch,
  Firewall rule, Restore, with validity checks and ActionResult feedback
- Simulation.perform/checkAllowed: validate, charge, execute, publish
- Wallet with income every 5 ticks and "$1,500" formatting
- Restore removes attacker footholds; Block IP stops attackers still outside
- Inspect reveals a snapshot of true state and logs
- 51 new tests, one test class per action
```
