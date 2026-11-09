DONOR FX - client-side Fabric mod for Minecraft Java 1.21.11
=============================================================
Only YOU need it installed. It does nothing to the server.

WHAT IT DOES
- Watches chat for "<Name> paid you $<amount>" (e.g. "Steve paid you $10M").
- Plays an animation. Bigger payment = crazier animation:
    under 100K   small toast + level-up sound
    100K+        gold banner + coin rain
    1M+          white flash, screen shake, firework sound
    10M+         rainbow screen, giant "BIG PAYMENT", fireworks
    100M+        cinema bars, scrolling meme captions, dragon roar
    1B+          strobe, "THE ECONOMY IS BROKEN", everything at once
- Top-5 donor leaderboard on the right side, saved between sessions
  (config/donorfx-leaderboard.json).
- Press J to turn Cena mode on/off (everyone looks like the skin in
  src/main/resources/assets/donorfx/textures/entity/cena.png).

KEYS (change in Options > Controls > Donor FX)
  J  Cena mode      K  replay last animation      L  show/hide leaderboard

COMMANDS
  /donorfx test 10000000    try an animation without anyone paying you
  /donorfx reset            clear the leaderboard

SETUP
1. Install JDK 21 + IntelliJ IDEA Community.
2. This zip has no gradle wrapper jar. Go to https://fabricmc.net/develop, generate
   any 1.21.11 template, and copy its gradlew, gradlew.bat and gradle/ folder here.
3. Open the folder in IntelliJ, let Gradle sync, run "Minecraft Client" to test.
   If a version is "not found", copy the current 1.21.11 versions from
   fabricmc.net/develop into gradle.properties.
4. Build the jar: gradlew build  -> build/libs/donorfx-1.0.0.jar
   Put it plus Fabric API in your mods folder.

CHANGING THE COSTUME
Replace cena.png with any 64x64 Minecraft skin PNG (same file name).

NO-INSTALL JAR BUILD (GitHub)
1. Create a free GitHub account and a new empty repository.
2. Upload everything in this folder (including the hidden .github folder).
3. Open the Actions tab, wait for "Build jar" to finish (a few minutes).
4. Open the run, download the "donorfx-jar" artifact, unzip it, and use
   donorfx-1.0.0.jar (not the -sources jar).
