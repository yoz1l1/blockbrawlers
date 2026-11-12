# Block Brawlers

An original Minecraft 1.20.4 Fabric arena-brawler project. This is an independent game concept and is not affiliated with or endorsed by any other game studio or franchise.

## Current prototype
- Client-side Fabric mod for Minecraft 1.20.4.
- Press **B** to open the Block Brawlers menu (rebindable in Minecraft Controls).
- Four original prototype fighter slots: Ember, Moss, Volt, and Nova.
- Training and online-room UI placeholders; combat and networking are not implemented yet.
- No telemetry, analytics, remote account system, external matchmaking, or gameplay network requests are included.

## Build
Requirements: Java 17 and Gradle.
```bash
gradle build
```
The mod jar is produced in `build/libs/`.

## Privacy and network behavior
The mod code in this repository does not collect, store, or transmit player names, UUIDs, chat, worlds, coordinates, device identifiers, or other personal data. It does not contact a Block Brawlers backend. Minecraft itself, Fabric Loader/API, the launcher, and multiplayer servers may have their own network behavior and privacy policies. Gradle downloads build dependencies when building the project.

Future online features must be opt-in, documented, and designed to transmit only the minimum data required to join a room. Never send Microsoft/Minecraft passwords, access tokens, or session credentials to a custom service. Use Minecraft's authenticated server connection rather than collecting credentials.

## Original-content policy
Use only original names, code, artwork, sounds, models, maps, UI, and other assets, or assets whose licenses explicitly permit the intended use. Do not use third-party game logos, character likenesses, extracted assets, music, sound effects, or confusingly similar branding. Check project name/trademark availability before public commercial release.

## License
Source code is released under the MIT License. See `LICENSE`. This license does not grant rights to third-party trademarks or assets.
