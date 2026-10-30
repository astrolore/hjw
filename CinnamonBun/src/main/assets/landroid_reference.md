# Landroid: Android 14 to 17 Space Game Reference (mod v2)

Source: AndroidEasterEggs (UpsideDownCake = Android 14, VanillaIceCream = Android 15, Baklava = Android 16, CinnamonBun = Android 17 and 17.1).
Updated against the Android 17 (Cinnamon Bun) source. Word pools were identical in 14, 15 and 16; Android 17 grew most of them (see section 4). Android 15 and later have the "activities" system.

---

## 1. How a system is generated

- **Daily seed:** by default the seed is today's date, so everyone gets the same system on the same day. The seed is `year * 10000 + month * 100 + day` with the month counted from 0 (January = 0), so 28 Sep 2026 is 20260828.
- **System code:** version prefix + last 5 digits of the seed, e.g. `XXX-60828` for 28 Sep 2026. The prefix comes from the host app at runtime (`DessertUtils.getDessertCode()`), it is not hard-coded in this source. Earlier versions used `UDC-` (14), `VIC-` (15), `BKL-` (16).
- **Deterministic:** the same seed always builds the same star, planets, names and starting position. (Landing activities are the exception, see section 5.)
- **Star:** name, class, radius (1,000 to 8,000), mass = 4/3 * pi * r^3 * 0.5.
  - Class picked evenly from O, B, A, F, G, K, M (not realistic weighting).
  - Colors: O blue, B pale blue, A blue-white, F white, G yellow, K orange-yellow, M orange.
- **Planets:** 1 to 10 per system. Radius 50 to 2,000. Orbit distance 16,000 to 150,000. Mass = 4/3 * pi * r^3 * 2.5. Orbit speed follows Kepler's third law. Planet color is a hue taken from its radius.
- **Planet names:** system name + number (1, 2, 3...) sorted by distance from the star. Example: "Cygnus Prime 3".
- **Ship start:** random angle, random distance from the star within the planet orbit range, random heading.

## 2. Every field a planet has

| Field | What it is | Source |
|---|---|---|
| Name | System name + orbit number | see above |
| TYPE | descriptor + body type, e.g. "Frozen moon" | 75% planet descriptors / 25% "any" descriptors, then a body type |
| ATMO | one word | 75% atmosphere pool / 25% "any" |
| FLORA | one word | 75% life pool / 25% "any" |
| FAUNA | one word | 75% life pool / 25% "any" (rolled separately from flora) |
| Numbers | radius, mass, orbit, speed | random ranges |
| Explored | true once you land | text fields and planet art only show when explored |

Picks come from a shuffled bag: nothing repeats until the whole pool has been used once. The "any" pool is a single shared bag used by planet, life and atmosphere rolls.

## 3. System name recipe

1. Base: a constellation name (5% chance from the rare list).
2. Suffix: 75% chance. Of those, 5% from the rare suffix list; 5% chance a second rare suffix is added after a space.
3. Letter: 30% chance of A to Z. 5% chance of an extra delimiter after it.
4. Number: 30% chance of a number from 2 to 5038.

Delimiters between parts (weights): space 15, hyphen 3, `_` 1, `/` 1, `.` 1, `*` 1, `^` 1, `#` 1, `(^*!%@##!!` 0.1.

## 4. Word pools

Pool sizes are the Android 17 counts.

### Constellations (50)
Aries, Taurus, Gemini, Cancer, Leo, Virgo, Libra, Scorpio, Sagittarius, Capricorn, Aquarius, Pisces, Andromeda, Cygnus, Draco, Alcor, Calamari, Neko, Monoceros, Norma, Abnorma, Morel, Redlands, Cupcake, Donut, Eclair, Froyo, Gingerbread, Honeycomb, Icecreamsandwich, Jellybean, Kitkat, Lollipop, Marshmallow, Nougat, Oreo, Pie, Quincetart, Redvelvetcake, Snowcone, Tiramisu, Upsidedowncake, Vanillaicecream, Android, Binder, Campanile, Dread, Eridani, Baklava, Cinnamonbun

### Rare constellations (22, 5% chance)
Jandycane, Zombiegingerbread, Astro, Bender, Flan, Untitled-1, Expedit, Petitfour, Worcester, Xylophone, Yellowpeep, Zebraball, Hutton, Klang, Frogblast, Exo, Keylimepie, Nov, Nrp, Waffle, Helloworld, Cuckoo

### Suffixes (36)
Alpha, Beta, Gamma, Delta, Epsilon, Zeta, Eta, Theta, Iota, Kappa, Lambda, Mu, Nu, Xi, Omicron, Pi, Rho, Sigma, Tau, Upsilon, Phi, Chi, Psi, Omega, Prime, Secundo, Major, Minor, Diminished, Augmented, Ultima, Penultima, Mid, Proxima, Novis, Plus

### Rare suffixes (12)
Serif, Sans, Oblique, Grotesque, Handtooled, III "Trey", Alfredo, 2.0, (Final), (Final (Final)), (Draft), Con Carne

### Body types (20)
planet, planetoid, moon, moonlet, centaur, asteroid, space garbage, detritus, satellite, core, giant, body, slab, rock, husk, planemo, object, planetesimal, exoplanet, ploonet

### Planet descriptors (32)
earthy, swamp, frozen, grassy, arid, crowded, ancient, lively, homey, modern, boring, compact, expensive, polluted, rusty, sandy, undulating, verdant, tessellated, hollow, scalding, hemispherical, oblong, oblate, vacuum, high-pressure, low-pressure, plastic, metallic, burned-out, bucolic, polyhedral

### Atmospheres (27)
toxic, breathable, radioactive, clear, calm, peaceful, vacuum, stormy, freezing, burning, humid, tropical, cloudy, obscured, damp, dank, clammy, frozen, contaminated, temperate, moist, minty, relaxed, skunky, breezy, soup, glitter

### Life descriptors: flora and fauna (50)
aggressive, passive-aggressive, shy, timid, nasty, brutish, short, absent, teen-aged, confused, transparent, cubic, quadratic, higher-order, huge, tall, wary, loud, yodeling, purring, slender, cats, adorable, eclectic, electric, microscopic, trunkless, myriad, cantankerous, gargantuan, contagious, fungal, cattywampus, spatchcocked, rotisserie, farm-to-table, organic, synthetic, unfocused, focused, capitalist, communal, bossy, malicious, compliant, psychic, oblivious, passive, bonsai, enormous

### "Any" descriptors (86 entries, shared)
silly, dangerous, vast, invisible, superfluous, superconducting, superior, alien, phantom, friendly, peaceful, lonely, uncomfortable, charming, fractal, imaginary, forgotten, tardy, gassy, fungible, bespoke, artisanal, exceptional, puffy, rusty, fresh, crusty, glossy, lovely, processed, macabre, reticulated, shocking, void, undefined, gothic, beige, mid, milquetoast, melancholy, unnerving, cheery, vibrant, heliotrope, psychedelic, nondescript, indescribable, tubular, toroidal, voxellated, low-poly, low-carb, 100% cotton, synthetic, boot-cut, bell-bottom, bumpy, fluffy, sous-vide, tepid, upcycled, sous-vide (listed twice), bedazzled, ancient, inexplicable, sparkling, still, lemon-scented, eccentric, tilted, pungent, pine-scented, corduroy, overengineered, bioengineered, impossible, commercial, cooked, pumpkin-spiced, salted, minty, textured, shaded, dithered, unsequenced, super-effective

### What changed in Android 17
- Constellations: added Eridani, Baklava, Cinnamonbun. Cuckoo moved to the rare list.
- Rare constellations: added Nov, Waffle, Helloworld, Cuckoo. Nat is gone. "Petit Four" is now spelled Petitfour.
- Planet descriptors: added polyhedral. Atmospheres: added glitter. Life: added enormous.
- "Any" descriptors: added commercial, cooked, pumpkin-spiced, salted, minty, textured, shaded, dithered, unsequenced, super-effective.
- Fauna nouns: added agents. Flora nouns: added spores, fungi, nuts, algae.
- Activities: added dithering, petting {fauna}, petting {flora}, grafting {flora}, healing, rehearsing, vibing, [REDACTED].
- Suffixes, rare suffixes, body types and the generator logic are unchanged.

## 5. Landing activities (Android 15 and later)

When you land, a random activity is rolled for that planet (shown as `JOB:` on the HUD). Output is uppercased. The roll uses the same random stream as sparks and exhaust, so the exact activity depends on how you fly and is not fixed by the seed.

### Plain activities
refueling, sightseeing, vacationing, luncheoning, recharging, taking up space, reticulating space splines, using facilities, spelunking, repairing, dithering, healing, rehearsing, vibing, [REDACTED]

### Templated activities
- herding {fauna}
- taming {fauna}
- breeding {fauna}
- singing lullabies to {fauna}
- singing lullabies to {flora}
- singing lullabies to the {planet}
- gardening {flora}
- collecting {flora}
- surveying the {planet}
- mapping the {planet}
- breathing {atmo}
- reprocessing {atmo}
- bottling {atmo}
- petting {fauna}
- petting {flora}
- grafting {flora}

Tags are filled with the planet's own word plus a generic noun:
- **fauna nouns:** fauna, animals, locals, creatures, critters, wildlife, specimens, life, cells, agents
- **flora nouns:** flora, plants, flowers, trees, mosses, specimens, life, cells, spores, fungi, nuts, algae
- **atmo nouns:** air, atmosphere, clouds, atmo, gases
- **{planet}:** the planet's TYPE text, e.g. "frozen moon"

Example result: "HERDING CATS CRITTERS".

## 6. Flying and physics

- **Controls:** touch and drag anywhere. The drag direction is your heading. Inside a 50 dp dead zone you only turn; past it thrust ramps up, reaching full power at 100 dp. Lift your finger to cut thrust.
- **Engine:** acceleration 1,000 at full thrust. Speed limit 5,000.
- **Gravity:** every planet and the star pull on the ship (G = 0.01, force scales with mass over distance squared). For 2 seconds after launching, gravity is switched off.
- **Landing:** touch a planet while your nose points within 45 degrees of straight away from it (engine toward the ground) and you land (speed does not matter). Any other contact bounces you off in a spray of sparks. Landing marks the planet explored.
- **Launching:** thrust while landed. The ship lifts off after 1 second.
- **Edge of space:** the universe is a circle of radius 200,000 around the star; you cannot leave it.
- **Transits:** flying in front of the star is detected as a transit.
- **Landing marker:** a landing is cleaned up after 15 minutes of simulation time.

## 7. The HUD

- **Top left:** STAR (name and system code), CLASS, RADIUS, MASS, BODIES (explored / total), then one block (BODY, TYPE, ATMO, FAUNA, FLORA) per explored planet.
- **Bottom left:** ALT (distance to the nearest surface, shown under 10,000), or LND and JOB while landed. Then THR (thrust %), POS, VEL.
- **Bottom right:** the AUTO button, which toggles the autopilot. With the mod menu on you can swap it for REVEAL (see section 9).

## 8. Small extras

- **Autopilot (screensaver):** the Daydream screensaver flies itself with the autopilot, with auto zoom, from a truly random start position. Statuses: NONE, LANDED, LAUNCHING, LANDING, CHASING, APPROACHING, plus a target name. It picks the nearest unexplored planet, or a random one once all are explored. Its banner reads "---- AUTOPILOT ENGAGED ----". Source comments list four internal titles: "No Android's Sky", "Char Star Field", "V-leet: Harmless", "Contemplating My Orbital Mechanics".
- **Progress notification (Android 17):** while playing, an ongoing notification shows the ship's state. Landed: "landed: [planet]" with "currently: [activity]". Autopilot en route: "headed to: [planet]" with strategy, distance and ETA, plus a progress bar with a ship icon. Otherwise "in space" and "selecting new target..." or "under manual control". Short status chips: landed, en route, launched, adrift. The progress bar needs Android 16 or newer; older versions get plain text. You can turn the notification off in Settings.
- **Unlock:** opening the egg records the unlock and enables the screensaver component.
- **Versions:** Android 17 (Cinnamon Bun) is dated May 2026, Android 17.1 August 2026.

## 9. Mod features

This build adds the following on top of the stock game.

### Menu
The MENU button (top right) drops down CHOOSE WORLD, HISTORY, SETTINGS and REFERENCE. At the top of the dropdown you see the world's name, its date or random mode, its seed, and whether achievements are ENABLED or DISABLED for it. If you hide the HUD, a SHOW HUD entry appears at the top of the dropdown.

### Choose World
- **Calendar:** years 1000 to 9999. Every date is its own world, and today's date is the same world as the stock daily one.
- **Fast seek:** tap `<` or `>` to move one month, `<<` or `>>` to move one year. Hold any of them to keep going, faster and faster.
- **Seed box:** type any whole number from 0 to 9,223,372,036,854,775,807 (leading zeros are fine, 007 is 7). The line under the box tells you whether it is a date world or a random-world seed. Months count from 0 inside seeds, so seed 20261027 is 27 November 2026 and 27 October 2026 is 20260927.
- **Buttons:** LAUNCH plays the selected date, TODAY jumps back, RANDOM WORLD rolls a random seed (below 10,000,000, so it never collides with a date), LAUNCH SEED plays the number you typed.

### History
Every world you start is logged by seed: system name, planet count, star class, best explored count, date or random mode, last played time and visit count. Tap one to replay it. Replaying rebuilds the world fresh (planets start unexplored). A world where the mod menu was used is marked ACHIEVEMENTS DISABLED FOR WORLD. CLEAR needs a second tap and wipes the log and those marks.

### Settings: gameplay
- **Dynamic camera (on):** the zoom follows your speed, eases in near a surface, and kicks on landings. The camera shakes while you thrust and thuds when you land or bounce.
- **Camera shake (70%):** how strong that shake is.
- **Haptics (on):** vibration on landing, bounces and thruster start.
- **World grid (on) / Orbit lines (on).**
- **Show HUD (on):** hides all telemetry. Bring it back from the top of the menu.
- **Keep screen awake (on).**
- **Ship notification (on).**
The ship's trail is always on, and it now lasts long enough to trace a lap around the whole universe.

### Settings: mod menu
- **MOD MENU (off):** the master switch. Turning it on shows a warning: using the mod menu in a world permanently disables achievements for that world. Loading any world while it is on shows a second warning with CONTINUE or TURN OFF MODS. While it is off, everything below is greyed out and inactive.
- **Replace AUTO with REVEAL (off):** off shows AUTO, on shows REVEAL (tap again to HIDE). REVEAL shows every planet's details and art at once, like a fully explored system. It does not count as exploring: landing is what raises your explored count.
- **FOV:** how much of the universe you see. A notch marks the default and the thumb snaps to it. While you drag, the settings page turns see-through so you can watch the game behind it. At the far right it reads GOD'S EYE and the whole universe fits on screen: the edge of the universe is a ring, small bodies get dots, and when your ship is too small to see it becomes a pulsing red dot with a ring.
- **Engine power (100%)** and **Speed limit (5,000):** change how the ship flies. The defaults are the stock game.

### Reference
This page. Press EDIT to edit it as raw markdown: EDIT turns into RESET (back to the built-in text) and CLOSE turns into SAVE. Your edited copy is kept on your phone.

### Seeds
A world is fully decided by its seed, so the same seed always gives the same world. The system code you see in the HUD is the last 5 digits of the seed.

### Other changes
- **No intro:** opening the egg goes straight into the game. The constellation puzzle and hold-the-logo warp are gone.
