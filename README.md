# One Night at ASNL: progress report 1

This branch is a snapshot made for the first progress report. It shows:

- the network model: devices, links and the `Network` graph with BFS
  (reachability, shortest path, isolation and firewall blocking), with JUnit tests
- the office network scenario, built with `NetworkBuilder`
- a first Swing GUI: a clickable network map and an inspector panel with the
  **Inspect** action

The attack simulation, the player's other actions, money, waves and the full game
flow are not in this snapshot.

The map starts all green, because it shows what the security team *knows*. Click a
device and press **Inspect** to reveal its true state and its log. Some devices have
something to hide.

## Build and run

Java 21 is required; Maven is not (the Maven wrapper is included).

```
./mvnw package          # Windows: mvnw.cmd package
java -jar target/sysadmin-panic.jar
```

or `./mvnw exec:java`.
