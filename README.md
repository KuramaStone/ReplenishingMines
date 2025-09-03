# Replenishing Mines

**Replenishing Mines** is a 1.21.1 fabric mod that regenerates blocks in defined regions to allow players to have constant access to consumable resources. Brush loot is calculated when a block is placed, remaining consistent each time it regenerates. The regeneration process occurs at a customizable speed at a customizable delay.

## Commands

### Admin Permission
- `ReplenishingMines.admin`

### Core Commands
- `/regenmine reload`: Reload the plugin configuration.
- `/regenmine create <name> <world> <corner1> <corner2>`: Create a new regeneration region.
- `/regenmine delete <name>`: Delete an existing region.
- `/regenmine regen <name>`: Instantly regenerate a region.

### Modify Region Settings
- `/regenmine modify <name> brushLoot <loot>`: Set loot obtained from brushing blocks.
- `/regenmine modify <name> replacements <blockToReplace> <blockTable>`: Set a specific block in the template to be replaced with a block from the table.
- `/regenmine modify <name> regenSpeedInTicks <ticks>`: Define regeneration duration (0 pauses regeneration).
- `/regenmine modify <name> regenInstantly <true/false>`: Toggle instant regeneration after `regenSpeedInTicks`.
- `/regenmine modify <name> save`: Save the current region's block state.

## Configuration (`config.yml`)
```yaml
# When regen is set to instant, this is how fast it will regenerate.
instant regen:
  blocks per tick: 8
```
