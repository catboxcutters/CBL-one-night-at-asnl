# Phase 5 notes: Swing GUI

## What was built

The game is now playable end to end:

1. A start screen explains the game.
2. The game view has a status bar, the network map, the alert list, the inspector, and a
   control bar with pause and an event feed.
3. When the game ends, a summary dialog appears with "Play again".

Everything reaches the UI through the Observer pattern built in phases 2–4. The model and
simulation packages still import no Swing or AWT.

## Classes and their single responsibility (`nl.tue.sysadminpanic.ui`)

| Class | Responsibility |
|-------|----------------|
| `MainFrame` | The window. It switches between the start screen and the game view, builds the game view, and coordinates **selection** (map ↔ alerts ↔ inspector). |
| `GameClock` | Wraps a `javax.swing.Timer` that calls `simulation.tick()` every second. Has start and pause. |
| `StatusBar` | Scenario name, `HH:MM:SS` clock, "Wave 2/5", money, and the health `JProgressBar`. |
| `NetworkMapPanel` | Custom `paintComponent` drawing of links and nodes, plus click detection. |
| `MapLegend` | Draws the key in the map's corner. *Not in the spec's list; split off so the map panel stays focused.* |
| `AlertPanel` | `JList` of alerts, newest first, with a severity-colored stripe. Selecting an alert selects its device. |
| `InspectorPanel` | The selected device's name, type, known state and revealed log, plus the six action buttons. |
| `ControlBar` | The Pause/Resume button and the one-line event feed. |
| `StartScreen` | Title, how-to-play and the Start button. |
| `EndDialog` | "NETWORK SECURED" or "NETWORK BREACHED", the statistics, and Play again. |

`app.Main` now also switches to the system look-and-feel, so the game looks native on
Windows, macOS and Linux.

## How the classes interact

### Starting a game

```
Start button → MainFrame.startGame()
  simulation = Simulation.forScenario(scenario)     // fresh network every game
  clock      = new GameClock(simulation::tick)
  buildGameView():
    create StatusBar, NetworkMapPanel, AlertPanel, InspectorPanel, ControlBar
    simulation.addListener(each panel)              // Observer: subscribe
    simulation.addListener(this::onGameEvent)       // added last: game over → dialog
  clock.start()
```

### One tick

```
Swing Timer (on the EDT) → simulation.tick()
  → publish(TickAdvanced)  → StatusBar updates the clock; the map repaints; the inspector re-checks its buttons
  → publish(AlertRaised)   → AlertPanel adds a row; ControlBar shows "New HIGH alert: ..."
  → publish(HealthChanged) → StatusBar updates the bar
```

### A player action

```
click "Isolate · $300" → InspectorPanel.performCurrentAction(button)
  → simulation.perform(new IsolateAction(device))   // Command, from phase 3
      → publish(MoneyChanged)        → StatusBar
      → publish(DeviceStateChanged)  → map repaints (dashed border, broken links)
      → publish(ActionPerformed)     → ControlBar feed: "PC-03 isolated from the network"
```

The UI never changes the model directly. It only creates `PlayerAction` objects and hands
them to `simulation.perform()`. All game rules stay out of Swing code.

## Patterns and techniques to be able to explain

### Observer, the UI side

- Each panel `implements GameEventListener` and reacts only to the events it cares about.
  `StatusBar` and `ControlBar` use a pattern-matching `switch` over the sealed `GameEvent`,
  with a `default` branch for the events they ignore.
- `NetworkMapPanel` and `InspectorPanel` simply refresh on *every* event. Their content
  depends on many things at once (known states, isolation, blocked links, money), and a
  refresh is cheap, so this is simpler and cannot miss a case.
- Events are delivered synchronously, and the `GameClock` timer fires on the Event Dispatch
  Thread (EDT). So ticks, UI updates and button clicks all run on **one thread**. There is
  no race condition and no need for `synchronized`.

### Why the end dialog opens via `invokeLater`

`GameOver` is published *from inside* `simulation.tick()`. A modal dialog blocks the code
that opens it until it closes. Opening it straight from the listener would therefore freeze
halfway through `tick()`, with the rest of the tick unfinished. `SwingUtilities.invokeLater`
queues the dialog to open *after* the current tick has finished.

### The map scales with the window

Device positions are relative (0..1, since phase 1). `center(device)` computes
`MARGIN + x * (width - 2 * MARGIN)` on every paint, so resizing the window just repaints at
the new size. The margins are half a node plus padding, so nodes at the edges stay fully
visible. Clicks use the same shapes (`nodeShape(device).contains(point)`), so the clickable
area always matches the drawing.

### Buttons explain themselves

After every event, `InspectorPanel.refreshButtons()` builds the action each button *would*
perform for the current selection and calls `simulation.checkAllowed(action)` (from phase 3):

- allowed → the button is enabled, and its tooltip describes the action;
- not allowed → the button is disabled, and its tooltip is the reason, for example
  "Costs $1,500, you have $900", "PC-01 is already isolated" or "Select an alert to block
  its source IP".

The panel contains no game rules of its own. It just asks.

### Paint code details

- `paintComponent` works on a copy, `graphics.create()`, and disposes it afterwards. This
  keeps rendering settings such as antialiasing and strokes from leaking into other Swing
  components that share the same `Graphics`.
- Links are drawn before nodes, so lines never cross over the node boxes.
- Colors show **known** state only. The map never leaks hidden information.

## Behaviour details and decisions

1. **Block IP uses the source IP of the selected alert** (agreed). The button is disabled with
   "Select an alert to block its source IP" until an alert is selected. The alert stays
   selected when you click around the map, so you can still block its IP afterwards.
2. **Isolate turns into "Reconnect · free"** when the selected device is isolated.
3. **Firewall rule** opens a small dialog listing only the neighbors whose link *can* be
   blocked right now. The device is remembered before the dialog opens, because the timer
   keeps running while the dialog is open and the selection could change.
4. **The alert list ignores the selection changes Swing reports when a new alert is inserted
   at the top**, via the `inserting` flag; otherwise every new alert would steal the
   selection. Clicking an alert that is already selected re-selects its device.
5. **The inspector log only replaces its text when the text changed**, so the scroll position
   is not reset every second. Long lines wrap.
6. **Small addition beyond the spec's list:** the inspector header also shows "patched" and
   "isolated". These are existing device states, and without them the player cannot tell
   whether Patch or Isolate is still worth buying.
7. **The event feed does not announce "campaign contained".** Doing so would reveal silent
   attacks the player never detected, which breaks the hidden-information idea.
8. **INTERNET can be selected** (to look at it), but every device action is disabled with
   "The internet is outside our network".

## How this was checked

- All 164 existing tests still pass; the GUI code changed none of the model.
- `java -jar target/sysadmin-panic.jar` opens and runs.
- A throwaway screenshot driver (kept outside the project) opened the real window, clicked
  Start, played into wave 2 and saved images of the start screen, the game view and the end
  dialog. This caught two real problems, both now fixed:
  - the inspector log scrolled sideways and hid the start of lines;
  - bold words in the start-screen text were oddly spaced.

There are no automated GUI tests. Swing tests need a display and are fragile, and the
behaviour behind every button is already covered by the action tests from phase 3.

## Terminal theme (added after phase 5)

The GUI was later restyled to look like a Linux terminal: dark background, ANSI-style
colors, monospace text and shell-flavored labels (`root@soc:~$`, `tail -f /var/log/alerts`,
`isolate · $300`). **Only the look changed.** The layout, the Observer wiring, the button
logic and every model class are the same as before, and all tests pass unchanged.

### `TerminalTheme`: one palette, one place

| Class | Responsibility |
|---|---|
| `TerminalTheme` | Holds every UI color and the font, installs the look and feel, and offers small helpers: `font`, `titledBorder`, `button`, `colorFor(DeviceState)`, `colorFor(Severity)`. |

Before this change, colors were spread over six classes, and the map and the legend each
had their own copy of "healthy green". Now there is a single palette, the UI counterpart of
`GameConstants`: change `TerminalTheme.GREEN` and the map, legend, inspector, status bar and
end dialog all follow. `Main` calls `TerminalTheme.install()` once, on the EDT, before any
window exists.

### Things to be able to explain

- **Why Metal instead of the system look and feel?** The native Windows and macOS looks
  ignore many background colors (buttons especially), and they look different on each OS.
  Metal is Java's own cross-platform look, so the game looks identical everywhere. We use
  Metal's plain *Steel* theme (`DefaultMetalTheme`) because the default *Ocean* theme paints
  a light gradient on every button and scroll bar.
- **How can dialogs be dark when we never style them?** `install()` writes defaults into
  `UIManager` (keys such as `Panel.background`, `Button.border`, `OptionPane.background`).
  Every Swing component reads these defaults when it is created. So the
  `JOptionPane` of the firewall rule, which Swing builds itself, is themed without extra code.
  This is also why `install()` must run *before* the first window is built.
- **The font.** Java's generic `Monospaced` font maps to Courier New on Windows, which draws
  the block characters of the ASCII-art title at the wrong width, so the banner falls apart.
  `pickFontFamily()` therefore takes the first installed font from a short list of common
  terminal fonts (Cascadia Mono / Consolas on Windows, Menlo on macOS, DejaVu Sans Mono /
  Liberation Mono on Linux), with `Monospaced` as the last fallback. No font files are bundled.
- **Inverse video.** A terminal highlights by swapping foreground and background. The
  selected map node is filled with its state color and drawn with black text; the selected
  alert row is light with dark text. This replaced the blue selection halo.
- **Colored alert tags use HTML.** A `JLabel` has one foreground color, but the severity tag
  should have its own. Swing labels render simple HTML, so the renderer wraps the tag in
  `<font color=...>`. Spaces in the tag become `&nbsp;` (HTML would collapse them and the
  columns would no longer line up), and the summary is escaped. Selected rows skip the HTML
  and use a single inverse color.
- **The blinking cursor** in the event feed has its own 500 ms `javax.swing.Timer` in
  `ControlBar`. It is purely cosmetic and separate from `GameClock`, so it keeps blinking
  while the game is paused. A `HierarchyListener` stops it when "Play again" replaces the
  bar, so old timers don't keep running.
- **The firewall-rule marker** is now a small cyan `[fw]` box instead of a shield. It is
  shared by the map and the legend (`NetworkMapPanel.drawFirewallMarker`).
- **The split-pane divider** uses `BasicSplitPaneUI`, because Metal paints a light, dotted
  grip on it.
- **Alert rows wrap.** Each row is a two-column HTML table (time and tag | summary) as
  wide as the list, so a long summary wraps inside its own column and stays readable.
  Two details make this work:
  - The list overrides `getScrollableTracksViewportWidth()` to return `true`. Otherwise
    `JList` grows as wide as its longest row and nothing ever wraps.
  - `JList` caches row heights. When the panel is resized, rows wrap differently and change
    height, so a resize listener clears that cache by toggling `setFixedCellHeight`.

The theme was checked by rendering the start screen, game view, an isolated device, the
firewall dialog and the end dialog to images off-screen, then looking at them.

## Suggested commit message

```
Phase 5: Swing GUI, playable end to end

- MainFrame with start screen, game view and end-of-game dialog
- StatusBar, NetworkMapPanel (+ MapLegend), AlertPanel, InspectorPanel, ControlBar
- GameClock: Swing Timer driving Simulation.tick() on the EDT, with pause
- Panels observe the simulation; buttons use checkAllowed() for enabling and tooltips
- Block IP acts on the selected alert's source IP; firewall rule picks a neighbor
```
