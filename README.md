# Heartbound

Minecraft 1.21.1 mod about relationships: affinity between players and romantic relationships with mobs.
Multi-loader project: `common` + `fabric` + `neoforge`.

## Structure

- `common/` — all shared logic (affinity, stages, items, data). Vanilla code only.
- `fabric/` — Fabric entry point and platform services.
- `neoforge/` — NeoForge entry point and platform services.

## Build

Requires Java 21.

```
./gradlew build
```

Jars appear in `fabric/build/libs/` and `neoforge/build/libs/`.
On GitHub the same build runs automatically (Actions tab) and uploads both jars as artifacts.

## Before the first commit

Edit `gradle.properties`: set `mod_author` (and `group` / `license` if you want something other than the defaults).
`rootProject.name` in `settings.gradle` is `heartbound`; it should match the repository folder name.

## Roadmap

See GDD.md.

## Config

On first launch the mod writes `config/heartbound.json` (cooldowns, gains, follower limit, morning bonus,
gift overrides). Edit it and restart the game. Gift overrides use mob ids and optionally gender:

```json
"giftGains": { "fox": { "bouquet": 45 }, "piglin/female": { "heart_charm": 70 } }
```
