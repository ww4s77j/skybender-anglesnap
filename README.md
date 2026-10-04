# skybender-anglesnap
Fire control mod for the Orbital Skybender platform in Minecraft

Requirements:
 - Minecraft Java Edition 1.21.11, 26.1, 26.1.1, 26.1.2, 26.2, or 26.3
 - Fabric Loader 0.19.5 or newer
 - Fabric API for the same Minecraft version
 - Java 21 for 1.21.11, or Java 25 for 26.x (the official Minecraft Launcher includes the right Java automatically)

The mod only needs to be installed on your own game. It works in singleplayer and on multiplayer servers without being installed on the server.

Each Minecraft release has its own matching mod JAR. This is required because Fabric uses different runtime mappings for 1.21.11 and the 26.x releases.

Installation (official Minecraft Launcher):
1. Install Fabric Loader:
   - Download the installer from [fabricmc.net/use/installer](https://fabricmc.net/use/installer/) and run it.
   - On the Client tab, choose your Minecraft version and click Install.
   - This adds a `fabric-loader-...` profile to the Minecraft Launcher.
2. Start Minecraft once with the new Fabric profile, then quit. This creates the `mods` folder.
3. Download Fabric API from [Modrinth](https://modrinth.com/mod/fabric-api). Pick the file listed for your Minecraft version.
4. Download this mod from the [Releases page](https://github.com/cubicmetre/skybender-anglesnap/releases):
   - Download the JAR whose `+<version>` suffix exactly matches your Minecraft version: `+1.21.11`, `+26.1`, `+26.1.1`, `+26.1.2`, `+26.2`, or `+26.3`.
   - Don't download the "Source code" zip files; those are for developers.
5. Open your `mods` folder:
   - Windows: press Win+R, paste `%APPDATA%\.minecraft\mods` and press Enter.
   - macOS: in Finder, choose Go > Go to Folder and paste `~/Library/Application Support/minecraft/mods`.
   - Linux: `~/.minecraft/mods`.
6. Copy both jar files (Fabric API and this mod) into the `mods` folder.
7. Start Minecraft with the Fabric profile.
8. To check it worked, open chat and type `/skybend` followed by a space. The suggestions should list `set`, `time` and `fire`.

Installation (Prism Launcher or MultiMC):
1. Create a new instance for a supported Minecraft version and choose Fabric as the mod loader.
2. Open the instance's Mods page and add Fabric API. Use "Download mods" to search for it, or "Add file" if you downloaded it already.
3. Download the matching jar from the [Releases page](https://github.com/cubicmetre/skybender-anglesnap/releases) and add it with "Add file".
4. Make sure the instance uses Java 21 for 1.21.11 or Java 25 for 26.x (Settings > Java). Prism can download it for you.
5. Launch the instance and check for `/skybend` in chat as above.

Troubleshooting:
 - If Minecraft says the mod is incompatible or won't start, check that the jar's `+<version>` suffix exactly matches your Minecraft version and that Fabric API is in the `mods` folder.
 - If `/skybend` doesn't appear, make sure you launched the Fabric profile, not the plain Minecraft one.

Building:
 - Run `./gradlew build` (or `./gradlew buildAll`, which does the same thing) to build all supported targets. Each release JAR is written to its target project's `build/libs` directory, and the unit tests run as part of the build.
 - Run `./gradlew buildandplay` to build every target and then launch a development client to test by hand; it blocks until you close Minecraft. Pick a version with `-Pmc=mc12111` (default is `mc263`), and use `./gradlew :mc263:runClient` to launch one without rebuilding first.
 - A development run uses its own directory at `targets/<target>/run`, so your normal `.minecraft` installation is untouched and each version can keep its own test world.

Verifying a build:
 - `./gradlew build` already proves that every supported version compiles and that its tests pass, which is what catches Minecraft and Fabric API changes between versions.
 - The six versions share only two code paths: `1.21.11` uses one set of client classes and `26.1` through `26.3` share another, so launching `-Pmc=mc263` and `-Pmc=mc12111` covers the runtime behaviour of all six.
 - In the launched game, join a world and check three things: `/skybend` offers `set`, `tps`, `overlay`, `time` and `fire`; `/skybend tps` reports a measured rate rather than the fallback; and the dial is visible and sweeps as you turn.
 - The log files at `targets/<target>/run/logs/latest.log` should contain `Server time hook active` after joining a world. If that line is missing, the packet hook did not load on that version and the tick rate will stay at its 20 TPS default.

In Game:
 - Run command `/skybend set <n> <ox> <oz>` where `<n>` is the size of the tnt warhead (1-15), `<ox>` is the X coordinate of the cannon origin and `<oz>` is the z coordinate of the cannon origin.
 - Run command `/skybend time <tx> <tz>` where `<tx>` is the target x coordinate, and `<tz>` is the target z coordinate. Running this command without a defined target, i.e. `tx` and `tz` intentionally left blank, will instead use the location that the player is looking at in the world. The command will then return a time estimate for delivery of a tnt warhead to the target location.
 - Run command `/skybend fire <tx> <tz>` where `<tx>` is the target x coordinate, and `<tz>` is the target z coordinate. Running this command without a defined target, i.e. `tx` and `tz` intentionally left blank, will instead use the location that the player is looking at in the world. The command will then compell the client to look at a specific sequence of view directions to transfer target information to the cannon remotely using the cannons wireless interface.
 - Run command `/skybend tps` to show the server tick rate the mod is pacing against, `/skybend tps auto` to go back to measuring it, or `/skybend tps <rate>` to pin it manually (1-100).
 - The in-world input dial is drawn by default: all sixteen encoder digits placed around you at the angles the cannon reads, the smaller master/fire/up switches, and a gauge around the crosshair that runs red to green to show when the next input may be given. `/skybend overlay off` hides it and `/skybend overlay on` brings it back, `radius <blocks>` and `scale <factor>` resize it, and `/skybend overlay` alone reports the current setting.



When Firing:
- Once the time estimate reaches zero, the cannon will wait for the player to look straight down before dispatching the tnt warhead. Additional firing instructions sent to the cannon in between the time a sequence is input and the time the warhead is ready will be ignored.

Server timing:
 - The mod measures the server's real tick rate from the game time a vanilla server broadcasts once per second, and paces both the input sequence and the time estimate against it. On a server running below 20 TPS each angle is held for the same number of server ticks the cannon expects, just spread over more real time; if the server stalls, the countdown freezes rather than running past it.

Dial overlay:
 - The dial renders entirely on your client and is shown by default (use `/skybend overlay off` to hide it). It mirrors what the mod is transmitting: the digit panel you are looking at is framed in white, which is also the panel a hand input would send. Nothing about it is visible to other players and it never changes what is transmitted.

 - `/skybend tps` shows the rate currently in use, `/skybend tps auto` goes back to measuring it, and `/skybend tps <rate>` pins a fixed rate (1-100) if you would rather not trust the measurement. The rate only affects pacing, never the encoded payload.

