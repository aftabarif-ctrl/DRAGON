# Dragon Lords (NeoForge 1.21.1)

Five tameable, rideable dragons: Fire, Ice, Forest (poison), Ocean (water), Storm (lightning).

## Build
Requires JDK 21 and Gradle 8.8+.
    gradle wrapper --gradle-version 8.10.2
    ./gradlew runClient      # test in dev
    ./gradlew build          # jar in build/libs

## Play
- Spawn eggs are in the Spawn Eggs creative tab; wild dragons spawn in their habitat biomes.
- Tame: feed the dragon's favourite food (Fire: cooked porkchop, Ice: salmon, Forest: cooked chicken, Ocean: cod, Storm: cooked mutton), 1-in-3 chance per feed.
- Saddle: right-click the tamed dragon with a normal saddle. Right-click again to mount. Shift+right-click = sit/stand.
- Controls: W = forward, mouse = steer. On the ground, hold W and look up to take off. Look down to dive (speed boost).
  Release W to glide/descend; glide into the ground to land.
  R (hold) = breath, V = charge. Rebind in Controls > Dragon Lords.
- Breath and charge destroy blocks up to hardness 30 (obsidian is safe) and respect the mobGriefing gamerule.

## Replacing the textures
`tools/generate_assets.py` creates placeholder skins. Paint over
`assets/dragonlords/textures/entity/<type>_dragon.png` (256x256). UV islands are the `texOffs(...)` values in DragonModel.java:
body (0,0) · neck (0,44)/(40,44) · head (80,44) · snout (120,44) · jaw (150,44) · horn (190,44)
tails (0,64)/(44,64)/(84,64) · spike (120,64) · wing arm/membrane (0,88)/(0,96) · wing tip (80,88)/(80,96)
front legs (0,120) · back legs (0,140) · foot (36,120) · saddle (100,120)

## Tuning
All per-dragon numbers (health, damage, size, food, particles, sounds) are in DragonType.java.
