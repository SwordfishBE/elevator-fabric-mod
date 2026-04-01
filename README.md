# Elevator

A simple elevator mod for Fabric.

[![GitHub Release](https://img.shields.io/github/v/release/SwordfishBE/elevator-fabric-mod?display_name=release&logo=github)](https://github.com/SwordfishBE/elevator-fabric-mod/releases)
[![GitHub Downloads](https://img.shields.io/github/downloads/SwordfishBE/elevator-fabric-mod/total?logo=github)](https://github.com/SwordfishBE/elevator-fabric-mod/releases)
[![Modrinth Downloads](https://img.shields.io/modrinth/dt/s5gP8ABG?logo=modrinth&logoColor=white&label=Modrinth%20downloads)](https://modrinth.com/mod/elevator-mod)
[![CurseForge Downloads](https://img.shields.io/curseforge/dt/1487910?logo=curseforge&logoColor=white&label=CurseForge%20downloads)](https://www.curseforge.com/minecraft/mc-mods/elevatorr)

## ✨ Features

| Feature            | Description                                                              |
|--------------------|--------------------------------------------------------------------------|
| **Elevator up**    | Stand on the elevator block and **jump** → teleport to the block above   |
| **Elevator down**  | Stand on the elevator block and **sneak** → teleport to the block below  |
| **Redstone base**  | The elevator block must be placed **on top of a redstone block**         |
| **Carpet hiding**  | Place a carpet on top of the elevator block — it still works!            |
| **Particles**      | Visual particle effect on teleport                                       |
| **Sound**          | Sound effect on teleport                                                 |
| **Cooldown**       | Configurable cooldown between teleports                                  |
| **Safety check**   | Only teleport if the destination has enough headroom                     |

## ❗ How to use

1. Place a **redstone block** on the floor.
2. Place the **elevator block** (default: iron block) on top of the redstone block.
3. Repeat on each floor — all elevator blocks must be the same block type.
4. Leave at least **2 free blocks** of space above each elevator block.
5. Stand on the elevator block and:
   - **Jump** → go up
   - **Sneak** → go down

> Tip: hide the elevator block by placing a carpet on top of it!

## ⚙️ Configuration

The config file is created automatically on first server start:
`config/elevator.json`
```json
{
  "elevatorBlock": "minecraft:iron_block",
  "maxElevatorHeight": 50,
  "particlesEnabled": true,
  "soundEnabled": true,
  "cooldownTicks": 20,
  "safetyEnabled": true
}
```

| Option              | Description                                                        |
|---------------------|--------------------------------------------------------------------|
| `elevatorBlock`     | Block ID used as elevator                                          |
| `maxElevatorHeight` | Maximum distance (in blocks) to search for the next elevator block |
| `particlesEnabled`  | Show particles on teleport                                         |
| `soundEnabled`      | Play sound on teleport                                             |
| `cooldownTicks`     | Ticks between teleports (20 = 1 second)                            |
| `safetyEnabled`     | Skip destination if headroom is blocked                            |

## Commands

| Command            | Description                                                         | Permission |
|--------------------|---------------------------------------------------------------------|------------|
| `/elevator info`   | Shows which configured block must be placed on top of a redstone block | Everyone   |
| `/elevator reload` | Reload the config file                                              | OP         |

## 📦 Installation

### Downloads

| Platform   | Link |
|------------|------|
| GitHub     | [Releases](https://github.com/SwordfishBE/elevator-fabric-mod/releases) |
| Modrinth   | [Elevator Mod](https://modrinth.com/mod/elevator-mod) |
| CurseForge | [Elevatorr](https://www.curseforge.com/minecraft/mc-mods/elevatorr) |

1. Download the latest JAR from your preferred platform above.
2. Place the JAR in your server's `mods/` folder.
3. Make sure [Fabric API](https://modrinth.com/mod/fabric-api) is also installed.
4. Start the server — the config file will be created automatically.

## 🧱 Building from source

Requirements: Java 25+
```bash
git clone https://github.com/SwordfishBE/elevator-fabric-mod.git
cd elevator-fabric-mod

# On Linux/macOS:
./gradlew build

# On Windows:
gradlew.bat build
```

## 📄 License

Released under the [AGPL-3.0 License](LICENSE).
