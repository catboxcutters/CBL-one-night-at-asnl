# One Night at ASNL: progress report 2

This branch is a snapshot made for the second progress report. It contains the
full, playable game:

- the network model: devices, links and the `Network` graph with BFS
  (reachability, shortest path, isolation and firewall blocking)
- the attack simulation: campaigns whose stages follow the State pattern, with
  brute force and phishing as interchangeable Strategy objects, plus logs, alerts
  and network health
- the six player actions as Command objects (inspect, block IP, isolate, patch,
  firewall rule, restore), with money and income
- the office network scenario built with `NetworkBuilder`, the 5-wave script,
  win/lose rules and end-of-game statistics
- a Swing GUI wired to the model through the Observer pattern: status bar,
  clickable network map, alert list, inspector with action buttons, and a control
  bar with pause/resume

JUnit tests cover the graph algorithms, the attack stages, every action and a
headless playthrough of the whole game.

The visual styling and balancing are not final yet.

## Build and run

Java 21 is required; Maven is not (the Maven wrapper is included).

```
./mvnw package          # Windows: mvnw.cmd package
java -jar target/sysadmin-panic.jar
```

or `./mvnw exec:java`.
