# FireMaze Plugin

A Minecraft Paper plugin that creates fire-based maze challenges with heat damage, lava traps, fire mob spawns, and flame walls.

## Features

- **Heat Damage System**: Players take damage over time in heat zones
- **Lava Trap Triggers**: Interactive lava traps that damage players
- **Fire Mob Spawn Zones**: Configurable areas that spawn fire-resistant mobs
- **Timed Flame Wall Sections**: Explosive flame wall effects
- **Configurable Settings**: Adjust damage rates, intervals, and mob counts

## Commands

- `/firemaze create heatzone` - Create a heat damage zone
- `/firemaze create lavatrigger` - Create a lava trap trigger
- `/firemaze create firemob` - Create a fire mob spawn zone
- `/firemaze create flamewall` - Create a flame wall section
- `/firemaze start` - Activate heat damage
- `/firemaze stop` - Deactivate heat damage

## Installation

1. Build the plugin using Maven: `mvn clean package`
2. Copy the generated JAR from `target/FireMazePlugin-1.0.jar` to your server's `plugins` folder
3. Restart your Paper server

## Configuration

Edit `plugins/FireMazePlugin/config.yml` to adjust:
- `damage-rate`: Heat damage per tick
- `damage-interval`: Time between damage ticks
- `fire-mob-count`: Number of mobs spawned per trigger
- `fire-damage-multiplier`: Multiplier for fire damage

## Permissions

No special permissions required. All players can use commands and interact with maze elements.