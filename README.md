# VanillaFamily

> **Status: Early Development** -- This project is in alpha stage. Features may be incomplete and bugs are expected.

Paper 1.21+ guild/family plugin for Minecraft servers.

## Features

- **Guild Management** -- Create, disband, invite, join, kick, promote, and demote members
- **Dual GUI** -- 27-slot member panel and 9-slot non-member panel, both with click-to-action support
- **Guild Bank** -- Shared item storage with serialization, permission-based deposit and withdraw
- **Guild Chat** -- Toggleable guild-only chat with quick message prefix (`!`)
- **Level & Experience** -- Guild leveling with configurable experience requirements and auto-level-up
- **BUFF System** -- Damage boost, damage reduction, and EXP bonus that scale with guild level
- **Contribution System** -- Earn contribution points through gold donations, daily sign-in, and mob kills
- **Daily Quests** -- Trackable daily quests for killing mobs, breaking blocks, fishing, and more
- **Advancements** -- Custom Minecraft advancements for guild milestones
- **Vault Integration** -- Optional economy support via Vault
- **SQLite Storage** -- Persistent data storage with automatic cleanup of orphaned records

## Commands

| Command | Description |
|---------|-------------|
| `/guild` | Open main GUI panel |
| `/guild help` | Show command list |
| `/guild create <name>` | Create a new guild |
| `/guild info [name]` | View guild information |
| `/guild invite <player>` | Invite a player to your guild |
| `/guild join <name>` | Accept an invitation |
| `/guild leave` | Leave your current guild |
| `/guild kick <player>` | Kick a member |
| `/guild promote <player>` | Promote a member's rank |
| `/guild demote <player>` | Demote a member's rank |
| `/guild chat` | Toggle guild chat mode |
| `/guild upgrade` | Upgrade guild level |
| `/guild bank` | Open guild bank |
| `/guild buff` | View active buffs |
| `/guild contribute <amount>` | Donate gold for contribution |
| `/guild sign` | Daily sign-in |
| `/guild top` | View contribution rankings |

Quick chat: type `!<message>` in public chat to send to your guild.

## Permissions

| Permission | Default |
|------------|---------|
| `vanilla.guild.create` | true |
| `vanilla.guild.disband` | true |
| `vanilla.guild.invite` | true |
| `vanilla.guild.kick` | true |
| `vanilla.guild.promote` | true |
| `vanilla.guild.demote` | true |
| `vanilla.guild.chat` | true |
| `vanilla.guild.upgrade` | true |
| `vanilla.guild.bank` | true |
| `vanilla.guild.contribute` | true |
| `vanilla.admin` | op |

## Dependencies

- **Required:** Paper 1.21+ (or compatible fork)
- **Optional:** [Vault](https://www.spigotmc.org/resources/vault.34315/) for economy support

## Build

```bash
./mvnw clean package
```

Output JAR: `target/VanillaFamily-1.0.0.jar`

## License

This project is provided as-is for personal and server use.
