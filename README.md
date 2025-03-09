# 🏹 Towerbow
Towerbow is a bow-focused pvp gamemode inspired by SkyHigh using Minestom and MinestomPvP, featuring 2v2, 3v3, 4v4, 5v5, and 6v6 team battles.

## Game Overview

The game takes place on a 100x100 map where players must eliminate their opponents using a bow while airborne, as the "terrestrial" zone (below a certain Y-level) inflicts damage after a short invincibility period at the start of the game.

- Each players has 3 lives. (configurable)
- The last team standing wins.
- Saturation is always full, and intra-team damage is disabled.

## Game Flow

## Pre-Game
Before the game starts, players are in a waiting lobby where each player can choose to join one of the two teams or become a spectator.
The game can start with a minimum of 4 players.

### Team Selection

- When a game is created, the server randomly selects the colors for the two teams (e.g., Blue vs. Red, Orange vs. Purple).
- Players choose their team from the available options: Blue vs. Red, Cyan vs. Yellow, Orange vs. Purple.

### Game Start

- Once all players have joined a team, the game begins.
- Players are teleported to their respective 7x7 team zones.
- The starting island clears, and players receive a blindness effect and are immobilized for 5 seconds.

### Invincibility Period

- A 20-second invincibility period follows, accompanied by a Jump Boost effect.
- PVP is disabled during this period.

### Player Equipment

- Players spawn with:
  - 1 Golden Apple (+1 per kill)
  - Golden Pickaxe with Efficiency II & Sharpness
  - Iron Armor with Protection II & Feather Falling V
    - Helmet: Stained Glass Block in team color
    - Chestplate: Leather Armor in team color
  - Infinity Bow with Punch & Power II
  - Infinite Cobblestone blocks that turn into Mossy Cobblestone and disappear after 3 minutes (faster as the game progresses).

### Game Progression

- After 8 minutes, the border shrinks to 50x50 over 1 minute.
- Longshots (hits > 50 blocks, scales with the map size) heal 2 hearts for the player and display a message to all players.

## Bonus Blocks

- Starting 60 seconds into the game, bonus blocks appear every 60 seconds (faster as the game progresses).
- Bonus blocks spawn between two random players from different teams if they are more than 20 blocks apart.
- If teams are too close, the block spawns 10 blocks below a random player.

### Bonus Effects

- **Explosive Arrow**: Creates a small explosion 2 seconds after impact, breaking blocks.
- **Smoke Arrow**: Releases smoke particles and applies darkness to blind opponents.
- **Wall Arrow**: Explodes in the air after 0.4 seconds, creating a 5x5 cube.
- **Full Heal**: Fully heals the team and provides 4 absorption hearts.
- **Strike**: Strikes the opposing team with lightning, dealing 4 hearts of damage.

## Map Scaling

- The map size adjusts based on the number of players:
  - 1v1 (2-3 players): 40x40
  - 2v2 (4-5 players): 60x60
  - 3v3 (6-7 players): 70x70
  - 4v4 (8-9 players): 80x80
  - 5v5 (10-11 players): 90x90
  - 6v6 (12 players): 100x100
