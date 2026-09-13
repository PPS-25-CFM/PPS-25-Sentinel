# Sentinel

[![CI](https://github.com/PPS-25-CFM/PPS-25-Sentinel/actions/workflows/ci.yml/badge.svg)](https://github.com/PPS-25-CFM/PPS-25-Sentinel/actions/workflows/ci.yml) [![codecov](https://codecov.io/gh/PPS-25-CFM/PPS-25-Sentinel/branch/main/graph/badge.svg)](https://codecov.io/gh/PPS-25-CFM/PPS-25-Sentinel)

Sentinel is a simulation system for modeling and analyzing the behavior of warehouses managed by fleets of autonomous robots. For each warehouse the user can configure one or more operating scenarios, specifying the available robots and the set of missions they must complete, along with the management policies that regulate mission assignment, path computation and conflict resolution between robots. Each scenario can be executed through one or more simulations: at the end of each run the system produces a report with performance statistics, allowing alternative configurations of the same warehouse to be compared in order to find the most effective one.

### Tech

Main technologies used are:

- [ScalaFX](http://www.scalafx.org/) - used for the GUI
- [Monix](https://monix.io/) - used for reactive programming (`Task`, `Observable`)
- [uPickle](https://com-lihaoyi.github.io/upickle/) + [os-lib](https://github.com/com-lihaoyi/os-lib) - used for JSON serialization and save/load of warehouses and scenarios

Build and quality tools:

- `sbt` with [ScalaTest](https://www.scalatest.org/), [Scoverage](https://github.com/scoverage/sbt-scoverage), [Scalafmt](https://scalameta.org/scalafmt/), [Wartremover](https://www.wartremover.org/).

## Installation

Requirements: JDK 25 or later.

1. Download the latest release (`Sentinel-<version>.jar`) from the [Releases](../../releases) page.
2. Run it:

```bash
java -jar Sentinel-<version>.jar
```

## Usage

The typical workflow is:
1. **Create a Warehouse**
2. **Generate a Scenario**
3. **Run the Simulation**
4. **Read the Statistics.**

### 1. Create a warehouse

From the main menu open the **Edit Warehouse** page and draw the warehouse as a 2D grid of tiles:

- `Shelf`: non-traversable but interactive cell holding an `Item` (robots stop next to it to pick up / drop goods);
- `Floor`: traversable cell with a crossing cost (time needed to cross it);
- `LoadingBay`: special traversable cell for load / unload operations.

Cells with no tile will be considered as _obstacles_.

Set the grid size, place tiles, then save the warehouse to a JSON file so it can be reused across scenarios and runs.

### 2. Generate a scenario

From the main menu open the **Configure Scenario** page, pick a previously saved warehouse and turn it into an operating scenario:

- add **robots** (`Drone`, `Carrier`, `HeavyCarrier`);
- add **missions** (`relocate`, `pick`, `deliver`);
- choose the **management policies**:
  - assignment: `Nearest`, `Cycle`, `LeastWorkload` or `Random`;
  - routing metric: `Distance`, `Time` or `Obstacles`;
  - collision handling: avoidance `Wait` or `Reroute`, combined with selection `Random`, `Priority` or `Deadline`;
    
Save the scenario to a JSON file. Warehouses and scenarios can be loaded back at any time, so alternative configurations of the same warehouse can be compared.

### 3. Run the simulation

From the main menu choose **Run Simulation**, load a saved scenario file (`*.json`) and watch the run on the warehouse grid: robots are shown with their paths, while the side panels list missions by status (`Pending | Assigned | Running | Completed | Failed`), robots by status (`Idle | Ready | Moving | Arrived`) with their current assignment, and the stream of simulation events.

The run can be controlled with the on-screen commands and the keyboard shortcuts:

- `Pause` (`P`): freeze the simulation;
- `Resume` (`R`): resume the periodic advance (one tick per second);
- `Back` (`A`): step one tick back and pause;
- `Next` (`D`): step one tick forward and pause;
- `Back to menu`: abort the run and return to the main menu.

The simulation ends when there are no more mission left or when the time limit is reached.

### 4. Read the statistics

Once finished, the system shows the final **Report** with performance statistics.
