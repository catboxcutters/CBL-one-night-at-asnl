# Phase 1 notes: skeleton and network model

## What was built

- A Maven project (Java 21, JUnit 5 only) with the Maven Wrapper, so nobody needs Maven installed.
- A runnable jar: `./mvnw package`, then `java -jar target/sysadmin-panic.jar`, or `./mvnw exec:java`.
- The network graph with BFS-based reachability and shortest paths (advanced topic 1).
- `GameConstants`, holding every balancing number in the spec.
- 32 JUnit tests.

## Build setup

| File | Purpose |
|------|---------|
| `pom.xml` | Sets Java 21 (`maven.compiler.release`) and JUnit 5 in test scope. `finalName` fixes the jar name to `sysadmin-panic.jar`. `maven-jar-plugin` writes `Main-Class` into the manifest so `java -jar` works. `exec-maven-plugin` makes `./mvnw exec:java` work. |
| `mvnw`, `mvnw.cmd` | The official Maven Wrapper 3.3.2 scripts, for Unix and Windows. The first time they run, they download Maven 3.9.9 into `~/.m2/wrapper`. |
| `.mvn/wrapper/maven-wrapper.properties` | Tells the wrapper which Maven version to download. It uses `only-script` mode, so no binary `.jar` is committed. |

On macOS or Linux, `mvnw` has to be executable. If a fresh checkout says "permission denied",
run `chmod +x mvnw` once. Git keeps that bit after it has been committed.

## Classes and their single responsibility

### `nl.tue.sysadminpanic.model`

| Class | Responsibility |
|-------|----------------|
| `GameConstants` | Holds every tunable number (prices, drain rates, timings, limits) in one place. It has a private constructor and is never instantiated. |
| `DeviceType` | Enum listing the kinds of device: INTERNET, FIREWALL, ROUTER, WORKSTATION, SERVER, DATABASE. |
| `DeviceState` | Enum listing the security states: HEALTHY, SUSPICIOUS, COMPROMISED. |
| `LogEntry` | Record for one immutable log line: tick, device id, text. |
| `Device` | A graph node. It stores identity, criticality, map position, patched and isolated flags, true and known state, and a bounded log. |
| `Link` | A graph edge. It is undirected, connects two devices, and has a `blocked` flag for firewall rules. |
| `Network` | The graph itself. It adds devices and links, looks them up, and answers reachability questions with BFS. |

### `nl.tue.sysadminpanic.app` and `nl.tue.sysadminpanic.ui`

| Class | Responsibility |
|-------|----------------|
| `Main` | The entry point. It creates the window on the Event Dispatch Thread. |
| `MainFrame` | The main window. For now it is an empty, resizable `JFrame` with a `BorderLayout`. The panels are added in phase 5. |

## How the classes interact

```java
Network network = new Network();
Device internet = new Device("INTERNET", "INTERNET", DeviceType.INTERNET, 1, 0.05, 0.5);
Device db = new Device("DB-01", "DB-01", DeviceType.DATABASE, 5, 0.9, 0.8);
network.addDevice(internet);        // creates an empty adjacency list for the device
network.addDevice(db);
network.addLink(internet, db);      // one Link object, added to BOTH adjacency lists

db.setIsolated(true);
network.isReachable(internet, db);  // -> breadthFirstSearch(internet) -> false
network.shortestPath(internet, db); // -> List.of()  (empty: no route)
```

Each of the three public query methods calls one private method, `breadthFirstSearch(start)`:

- `isReachable(from, to)` checks whether `to` is in the result.
- `reachableSet(from)` returns the result's key set.
- `shortestPath(from, to)` follows parent pointers back from `to`.

## Algorithm: breadth-first search (advanced topic 1)

**Representation.** The network uses an adjacency list, `Map<Device, List<Link>>`. We store
`Link` objects rather than neighbor `Device`s because the link carries the `blocked` flag.
Each link is shared by both ends, so blocking it once blocks traffic in both directions.

**BFS.** `breadthFirstSearch(start)` in `Network`:

1. If `start` is isolated, return an empty map. An isolated device takes part in no path at all.
2. Put `start` in the `parents` map with parent `null`, and add it to a FIFO queue.
3. While the queue is not empty, take the front device. For each of its links, step to the
   other end, but only if the link is not blocked, the next device is not isolated, and the
   next device has not been visited yet. Record its parent and enqueue it.
4. Return `parents`. Its keys are exactly the reachable devices.

**Why BFS gives the shortest path.** BFS visits every device at distance 1 before any at
distance 2, and so on. The first time a device is discovered, it is therefore discovered
through a shortest route, and we never overwrite its parent afterwards. (This works because
every hop has the same cost. With weighted edges you would need Dijkstra.)

**Complexity.** The search is O(V + E): each device is enqueued at most once, and each link is
examined at most twice, once from each end.

**One map for two jobs.** The `parents` map serves as the "visited" set (via `containsKey`)
and also stores the path information. Using one structure instead of a separate
`Set<Device> visited` keeps the code short.

**Determinism.** `LinkedHashMap` keeps insertion order, so BFS always explores neighbors in
the order the links were added. When two paths are equally short, the same one is chosen
every time, which keeps tests exact and fits the spec's "no randomness" rule.

## Design decisions worth being able to explain

- **`neighbors()` returns the physical wiring**, including blocked links and isolated devices.
  The firewall-rule dialog (phase 5) needs every neighbor. Questions about where traffic can
  actually go use `isReachable` and `shortestPath` instead.
- **Isolation is a flag on `Device`; links are not deleted.** Reconnecting is then just
  `setIsolated(false)`, and the map can still draw the cut links as broken lines. BFS
  treats an isolated device as impassable.
- **Blocking is a flag on `Link`,** for the same reason: firewall rules can be drawn and
  undone.
- **Map positions are relative, from 0.0 to 1.0.** The map panel multiplies them by its
  current width and height, so node positions scale automatically when the window is
  resized. `Device` rejects positions outside this range.
- **`trueState` and `knownState` are separate fields.** This is the hidden-information
  mechanic. For now both start as HEALTHY. Later phases change them in different ways
  (attacks change the true state; alerts and Inspect change the known state).
- **The device log is bounded** (`MAX_LOG_ENTRIES = 100`). It is an `ArrayDeque`: new lines
  are added at the back, and the oldest line is dropped from the front when the log is full.
  `getLog()` returns a read-only copy, so no other class can modify the log directly.
- **`Device` and `Link` use identity equality** (they do not override `equals`). Every device
  is one unique object, so `==` is the correct notion of sameness, and they can safely be used
  as `HashMap` keys even though their state changes.
- **`GameConstants` already contains all the spec's numbers,** including ones only used in
  later phases, so that all tuning happens in one place from the start.

## Open question for a later phase

The spec gives every device a criticality of 1–5, but it gives no value for INTERNET.
INTERNET can never be compromised or isolated, so its criticality never affects health.
The tests use 1. Decide this for sure in phase 4, when the scenario is built.

## Suggested commit message

```
Phase 1: Maven skeleton and network graph model

- Maven project (Java 21, JUnit 5) with wrapper and runnable jar
- Device, Link, Network with BFS reachability, shortest path, reachable set
- Isolation and firewall blocking respected by BFS
- GameConstants with all balancing numbers
- 32 unit tests for the graph and device model
```
