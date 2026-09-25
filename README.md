# HukumAC - Production Minecraft Anti-Cheat Plugin

> **Author:** Ardelys  
> **Target Server:** Paper 1.21.11 / Minecraft 26.3  
> **Server Network:** HükümCraft  
> **Java Version:** Java 21+ (Compatible with Java 25)

---

## Overview

**HukumAC** is a high-performance, modular, server-side anti-cheat plugin specifically engineered for **HükümCraft** on **Paper 1.21.11 / Minecraft 26.3**. It enforces authoritative server-side physics, statistical combat heuristics, raytraced reach calculations, and network balance analysis to detect and mitigate hacked clients without false positives.

---

## Architectural Highlights

- **Modular Check Framework**: Every check is an isolated component extending `Check` with `@CheckInfo` metadata, configurable thresholds, punishments, and decay rates.
- **Authoritative Server Physics**: Simulates vanilla Minecraft ground/air physics, friction, jump curves, and gravity.
- **Latency & Ping Compensation**: Incorporates per-player network latency (`getPing()`) into reach raycasting and tick buffer thresholds.
- **Server Lag Mitigation**: Automatically pauses sensitive checks when server TPS falls below configured thresholds (`min-tps: 18.0`).
- **Dynamic Violation Decay**: Gradual violation level decay over configurable intervals (`decay-interval-seconds: 10`), preventing false bans from accumulated marginal events.
- **Non-blocking Asynchronous Logging**: Queued disk logger (`logs/hukumac.log`) running on a dedicated worker thread to ensure zero main-thread tick stalling.
- **Zero Memory Leaks**: Immediate garbage collection of player ring buffers and history upon disconnection (`PlayerQuitEvent`).

---

## Check Categories & Modules

### Combat Checks
| Check | Sub-Types / Technique | Description |
| :--- | :--- | :--- |
| **KillAura** | `A (GCD/Snap)`, `B (Switch)`, `C (LineOfSight)`, `D (Action)` | Detects aim snapping, impossible rotational GCDs, multi-target switching (<45ms), attacking through walls, and attacking while blocking. |
| **AimAssist** | `A (PitchLock)`, `B (ConstantSmooth)` | Identifies robotic rotational tracking, constant pitch locks, and artificial aim smoothing. |
| **Reach** | `A (3D Raytrace)` | Exact 3D raytrace against player hitboxes with latency compensation (default 3.05 blocks limit). |
| **AutoClicker** | `A (CPS)`, `B (Consistency)` | Flags high CPS (>20 CPS) and click delay variance anomalies / macros. |
| **Criticals** | `A (Packet)`, `B (MicroHop)` | Flags spoofed falling states and micro-hops with impossible vertical delta. |
| **Velocity** | `A (Horizontal)`, `B (Vertical)` | Verifies that player delta vectors respond to server knockback and explosions. |
| **FastBow** | `A (DrawTime)` | Detects firing bows with full force faster than minimum draw ticks. |

### Movement Checks
| Check | Sub-Types / Technique | Description |
| :--- | :--- | :--- |
| **Speed** | `A (Prediction & Buffer)` | Compares horizontal speed (`deltaXZ`) against predicted physics limits across all blocks. |
| **Fly** | `A (Hover)`, `B (Ascent)` | Catches hovering in air for >5 ticks and mid-air ascent without jump or velocity. |
| **Glide** | `A (ConstantDescent)` | Detects slow falling without Elytra or potion effects. |
| **Jesus** | `A (SurfaceMove)`, `B (Speed)` | Flags walking or sprinting across liquid surfaces without sinking. |
| **Step** | `A (Height)` | Detects instant step-ups >0.62 blocks in a single tick. |
| **HighJump** | `A (InitialDeltaY)` | Detects jumping higher than vanilla physics allow. |
| **LongJump** | `A (AirDistance)` | Tracks total horizontal distance covered during jump arcs. |
| **NoFall** | `A (SpoofedGround)`, `B (DistanceReset)` | Detects spoofed `onGround = true` packets while falling in mid-air. |
| **NoSlow** | `A (ItemUsage)` | Enforces speed penalties while eating, drinking potions, or using bows. |
| **InventoryMove** | `A (Container)` | Catches moving or sprinting with open containers (chests, anvils, etc.). |
| **Phase** | `A (SolidBlock)` | Detects movement inside solid occluding blocks or through walls. |
| **Blink** | `A (Burst)` | Flags withholding packets followed by sudden distance leaps. |

### Player & World Checks
| Check | Technique | Description |
| :--- | :--- | :--- |
| **Timer** | `A (ClockDrift)` | Rolling clock balance detecting accelerated client game ticks. |
| **BadPackets** | `A (Pitch)`, `B (NaN_Rot)`, `C (NaN_Pos)` | Enforces pitch bounds (-90° to 90°) and validates against NaN/Infinite numbers. |
| **FastEat** | `A (Duration)` | Enforces minimum consumption time for food and potions. |
| **FastProjectile**| `A (Delay)` | Enforces throw cooldown for snowballs, eggs, and pearls. |
| **ImpossibleActions** | `A (SprintSneak)`, `B (Hunger)`, `C (Sleep)` | Detects impossible player action states. |
| **FastPlace** | `A (Delay)` | Enforces minimum block placement delay. |
| **FastBreak** | `A (Hardness)` | Detects breaking hard blocks faster than tool attributes permit. |
| **Scaffold** | `A (Headless)`, `B (InvalidFace)` | Detects headless bridging and placement on impossible faces. |
| **InvalidBlockInteraction** | `A (Reach)`, `B (Wall)` | Restricts block clicks through walls or beyond interaction reach. |

---

## Commands & Permissions

### Commands
- `/hukumac reload` - Reloads `config.yml` and re-initializes all check parameters.
- `/hukumac alerts` - Toggles in-game cheat alerts for staff.
- `/hukumac debug <player>` - Toggles the real-time debug stream for a player.
- `/hukumac info <player>` - Displays physics and network status (Ping, TPS, Ground state, Total VL).
- `/hukumac violations <player>` - Lists active violations and recent detection history.

### Permissions
- `hukumac.admin` - Access to administrative `/hukumac` commands.
- `hukumac.alerts` - Permission to view detection alert messages.
- `hukumac.debug` - Permission to view check debug messages.
- `hukumac.bypass` - Full bypass of all anti-cheat checks.

---

## Building from Source

Build the plugin using the included Maven Wrapper:

```bash
# Windows
.\mvnw.cmd clean package

# Linux / macOS
./mvnw clean package
```

The resulting production JAR will be generated at:
`target/HukumAC-1.0.1-RELEASE.jar`
