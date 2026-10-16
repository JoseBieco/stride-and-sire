# Fase C — Genética Transparente Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give every `AbstractHorse` a visible, breedable genetics layer: expanded
attribute ceilings with a letter-grade readout, diet-driven breeding that can push
a foal above the vanilla ceiling, three heritable lineage traits with a
latent/manifest state, and a Breeder's Spyglass to inspect all of it.

**Architecture:** The three numeric attributes (Speed, Jump, Health) stay stored
only in their native Forge `Attribute` instances — their ceiling is raised via an
`AttributeModificationEvent` handler (same mechanism Fase A used for the Gold
Horseshoe's speed modifier), and a pure `AttributeGrading` utility derives the
letter grade on demand, so there is never a second source of truth for a number
that already exists on the entity. The three lineage traits have no vanilla
equivalent, so they live in a new `HorseGeneticsAttachment` (same `AttachmentType`
pattern as `HorseshoeAttachment`/`SaddlebagAttachment`), each holding one of three
states: ABSENT, LATENT, MANIFEST. A breeding-event hook applies the inheritance
math from the spec to both attributes and traits in one pass, checking which diet
item (if any) started the breeding. The Breeder's Spyglass is a read-only screen
reading both the attribute grades and the attachment.

**Tech Stack:** Minecraft Forge, Minecraft 26.3, Java 25, Forge GameTest
framework, JUnit 5 for pure-logic unit tests (grading math, inheritance math).

**Spec:** `docs/superpowers/specs/2026-10-16-fase-c-genetica-transparente-design.md`

## Global Constraints

- Scope: Horse, Donkey, Mule, SkeletonHorse, ZombieHorse (unchanged from Fases A/B).
- Attribute ceiling: +20% above vanilla max for Speed, Jump, and Health.
- Grade bands (exact, from spec section 3), as a percentage of the *original*
  vanilla max: E 0–25%, D 25–50%, C 50–75%, B 75–100%, A 100–110%, **A/S are only
  reachable via diet-guided breeding — never from a wild spawn or unguided
  breeding**, S 110–120%.
- Diet items and the attribute each favors: Golden Carrot → Speed, Golden Apple →
  Health, Glistering Melon → Jump. Glistering Melon is not a vanilla horse
  breeding item today and must be newly enabled as one for `AbstractHorse`.
- Guided-breeding math (per attribute, every time a diet item is used): roll
  average-of-parents + random variance, clamped to 0–120% of vanilla max: for
  the favored attribute only, there's an additional 20% chance to instead anchor
  on `max(parent1, parent2)` plus a random upward bonus, still clamped to 120%.
- Unguided breeding (no diet item): same averaging formula, clamped to 0–100% —
  never produces A or S.
- Trait inheritance chances (exact, from spec section 5): with **no** parent
  carrying the trait, 2% LATENT / 1% MANIFEST per breeding; with **at least one**
  parent carrying it (latent or manifest), 35% LATENT / 15% MANIFEST.
- A trait in the LATENT state never activates its behavior on the carrying horse
  — only MANIFEST does.
- The Enriched Golden Apple from the original pitch is explicitly dropped; do not
  implement it.

## Review Focus

- Unguided breeding's random-variance roll must never land above 100% even at
  its extreme — an off-by-one in the clamp is the most likely way a wild/unguided
  foal accidentally gets an A/S grade.
- Using a diet item must only grant the 20%-best-parent mechanic to its *own*
  favored attribute — the other two attributes still only get the plain
  expanded-range average roll, never the best-parent anchor.
- Breeding two parents that **both** already carry a trait must still use the
  same "at least one parent" table (35%/15%) — nothing in the spec defines a
  higher tier for two carrying parents, so the implementation must not invent one.
- A MANIFEST trait's behavior must not suppress the underlying damage itself —
  e.g. Fearless suppresses the rearing/dismount reaction, not the zombie/creeper
  damage the horse actually takes.
- The Draft-trait saddlebag capacity unlock (Task 10) must be computed live from
  the mount's *current* trait state on every menu open, not cached at the time
  the saddlebag was equipped — otherwise a horse that loses/gains the trait (e.g.
  across a save reload) shows a stale slot count.

---

### Task 1: Attribute grade utility (pure logic)

**Files:**
- Create: `src/main/java/com/josebieco/strideandsire/genetics/Grade.java`
- Create: `src/main/java/com/josebieco/strideandsire/genetics/AttributeGrading.java`
- Test: `src/test/java/com/josebieco/strideandsire/genetics/AttributeGradingTest.java`

**Interfaces:**
- Produces: `enum Grade { E, D, C, B, A, S }`; static
  `AttributeGrading.gradeOf(double value, double vanillaMin, double vanillaMax) -> Grade`
  where the percentage is computed against `vanillaMax` (not the expanded max) per
  the Global Constraints band table.
- Consumed by: Task 2 (verifying the expanded ceiling lands in A/S), Task 7
  (Spyglass screen).

- [ ] **Step 1: Write failing tests for the six band boundaries**

```java
@Test void bandsMatchSpecTable() {
    double min = 0, max = 100; // normalized vanilla range for the test
    assertEquals(Grade.E, AttributeGrading.gradeOf(10, min, max));
    assertEquals(Grade.D, AttributeGrading.gradeOf(30, min, max));
    assertEquals(Grade.C, AttributeGrading.gradeOf(60, min, max));
    assertEquals(Grade.B, AttributeGrading.gradeOf(90, min, max));
    assertEquals(Grade.A, AttributeGrading.gradeOf(105, min, max));
    assertEquals(Grade.S, AttributeGrading.gradeOf(115, min, max));
}

@Test void bandBoundariesAreExact() {
    double min = 0, max = 100;
    assertEquals(Grade.B, AttributeGrading.gradeOf(100, min, max)); // exactly vanilla max
    assertEquals(Grade.A, AttributeGrading.gradeOf(100.0001, min, max));
    assertEquals(Grade.S, AttributeGrading.gradeOf(110.0001, min, max));
}
```

- [ ] **Step 2: Run tests, verify they fail**

Run: `./gradlew test --tests AttributeGradingTest`
Expected: FAIL — class not found.

- [ ] **Step 3: Implement `Grade` and `AttributeGrading.gradeOf`**

- [ ] **Step 4: Run tests, verify they pass**

Run: `./gradlew test --tests AttributeGradingTest`
Expected: PASS, 2/2.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/josebieco/strideandsire/genetics/Grade.java \
        src/main/java/com/josebieco/strideandsire/genetics/AttributeGrading.java \
        src/test/java/com/josebieco/strideandsire/genetics/AttributeGradingTest.java
git commit -m "feat: add attribute grade bands and grading utility"
```

---

### Task 2: Attribute ceiling expansion (+20%)

**Files:**
- Create: `src/main/java/com/josebieco/strideandsire/event/AttributeRangeExpansionHandler.java`
- Test (GameTest): `src/main/java/com/josebieco/strideandsire/gametest/AttributeRangeExpansionGameTest.java`

**Interfaces:**
- Consumes: nothing new.
- Produces: a `@Mod.EventBusSubscriber` reacting to the attribute-registration
  modification event, raising the registered max of `Attributes.MOVEMENT_SPEED`,
  `Attributes.JUMP_STRENGTH`, and `Attributes.MAX_HEALTH` **as applied to
  `AbstractHorse` entities** by 20% over their current vanilla-registered max.
- Consumed by: Task 4 (breeding sets values up to this new ceiling),
  `AttributeGrading` callers (Task 1) which take the *original* vanilla max as a
  parameter, not this new one — this task only changes what value a horse is
  *allowed* to hold, not how it's graded.

- [ ] **Step 1: Write a GameTest asserting a horse's `MOVEMENT_SPEED` attribute
  instance accepts a base value 15% above the pre-mod vanilla max without being
  clamped back down**

- [ ] **Step 2: Run the GameTest server, verify it fails**

Run: `./gradlew runGameTestServer`
Expected: FAIL — value gets clamped to the old vanilla max.

- [ ] **Step 3: Implement `AttributeRangeExpansionHandler`**

- [ ] **Step 4: Run the GameTest server, verify it passes**

Run: `./gradlew runGameTestServer`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: raise horse attribute ceilings 20% above vanilla max"
```

---

### Task 3: HorseGenetics trait attachment

**Files:**
- Create: `src/main/java/com/josebieco/strideandsire/genetics/TraitState.java`
- Create: `src/main/java/com/josebieco/strideandsire/genetics/LineageTrait.java`
- Create: `src/main/java/com/josebieco/strideandsire/attachment/HorseGeneticsAttachment.java`
- Create: `src/main/java/com/josebieco/strideandsire/attachment/HorseGeneticsAccess.java`
- Modify: `src/main/java/com/josebieco/strideandsire/registry/ModAttachments.java`
  (add `GENETICS`, keyed `strideandsire:genetics`)
- Test (GameTest): `src/main/java/com/josebieco/strideandsire/gametest/HorseGeneticsAttachmentGameTest.java`

**Interfaces:**
- Produces: `enum TraitState { ABSENT, LATENT, MANIFEST }`;
  `enum LineageTrait { DRAFT, SURE_FOOTED, FEARLESS }`;
  `record HorseGeneticsAttachment(Map<LineageTrait, TraitState> traits)` with an
  `EMPTY` constant (all ABSENT); static
  `HorseGeneticsAccess.get(AbstractHorse) -> HorseGeneticsAttachment`,
  `HorseGeneticsAccess.set(AbstractHorse, LineageTrait, TraitState)`.
- Consumed by: Task 5 (trait inheritance), Task 7 (Spyglass), Tasks 8–10 (trait
  behaviors).

- [ ] **Step 1: Write a GameTest that sets `LineageTrait.FEARLESS` to
  `TraitState.MANIFEST` on a horse via `HorseGeneticsAccess.set`, and asserts
  `HorseGeneticsAccess.get(horse).traits().get(LineageTrait.FEARLESS) == TraitState.MANIFEST`
  while the other two traits remain `ABSENT`**

- [ ] **Step 2: Run the GameTest server, verify it fails**

Run: `./gradlew runGameTestServer`
Expected: FAIL — classes not found.

- [ ] **Step 3: Implement `TraitState`, `LineageTrait`, `HorseGeneticsAttachment`,
  `HorseGeneticsAccess`, and register the attachment**

- [ ] **Step 4: Run the GameTest server, verify it passes**

Run: `./gradlew runGameTestServer`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: add HorseGenetics attachment for lineage trait states"
```

---

### Task 4: Diet-guided attribute inheritance (pure logic + wiring)

**Files:**
- Create: `src/main/java/com/josebieco/strideandsire/genetics/AttributeInheritance.java`
- Create: `src/main/java/com/josebieco/strideandsire/item/genetics/BreedingDietItem.java`
- Modify: `src/main/java/com/josebieco/strideandsire/event/HorseBreedingGeneticsHandler.java`
  (new file if it doesn't exist yet — this is the first genetics breeding hook)
- Test: `src/test/java/com/josebieco/strideandsire/genetics/AttributeInheritanceTest.java`
- Test (GameTest): `src/main/java/com/josebieco/strideandsire/gametest/HorseBreedingAttributeGameTest.java`

**Interfaces:**
- Consumes: `Grade`/`AttributeGrading` (Task 1), the attribute ceiling from Task
  2.
- Produces: `enum BreedingDietItem { NONE, GOLDEN_CARROT, GOLDEN_APPLE,
  GLISTERING_MELON }` with `favoredAttribute()` returning one of
  `Attributes.MOVEMENT_SPEED/JUMP_STRENGTH/MAX_HEALTH` (`null` for `NONE`);
  `AttributeInheritance.rollChildValue(double parent1, double parent2, double
  vanillaMax, boolean favored, boolean dietGuided, RandomSource random) -> double`
  — pure function taking an injected `RandomSource` so it is deterministically
  testable; clamps to `vanillaMax` when `!dietGuided`, to `1.2 * vanillaMax`
  when `dietGuided`, and when `favored && dietGuided` has a 20% chance (rolled
  from the injected `random`) to anchor on `Math.max(parent1, parent2)` plus an
  upward bonus instead of the average.
- Consumed by: `HorseBreedingGeneticsHandler`, which calls this once per
  attribute per breeding event, passing `favored = (diet item's favoredAttribute
  == this attribute)`.

- [ ] **Step 1: Write failing unit tests for `AttributeInheritance.rollChildValue`
  using a seeded/mock `RandomSource`: (a) `dietGuided=false` never returns a value
  above `vanillaMax` across a wide spread of injected random draws; (b)
  `dietGuided=true, favored=false` can return above `vanillaMax` (variance alone)
  but never invokes the best-parent anchor path; (c) `dietGuided=true,
  favored=true` with a random draw forced into the 20% branch returns exactly
  `max(parent1, parent2)` plus the bonus, not the average-based roll**

- [ ] **Step 2: Run tests, verify they fail**

Run: `./gradlew test --tests AttributeInheritanceTest`
Expected: FAIL.

- [ ] **Step 3: Implement `BreedingDietItem` and `AttributeInheritance.rollChildValue`**

- [ ] **Step 4: Run tests, verify they pass**

Run: `./gradlew test --tests AttributeInheritanceTest`
Expected: PASS, 3/3.

- [ ] **Step 5: Write a GameTest breeding two horses at vanilla-max Speed with a
  Golden Carrot repeatedly (fixed small trial count is acceptable here since this
  is wiring, not statistics) and asserting at least one resulting foal's Speed
  grades A or S, while a parallel unguided breeding of the same two parents never
  does across the same number of trials**

- [ ] **Step 6: Implement `HorseBreedingGeneticsHandler`'s attribute half: on
  foal creation, detect which `BreedingDietItem` (if any) triggered the breeding
  and call `rollChildValue` once per attribute**

- [ ] **Step 7: Run the GameTest server, verify it passes**

Run: `./gradlew runGameTestServer`
Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "feat: add diet-guided attribute inheritance for horse breeding"
```

---

### Task 5: Lineage trait inheritance (pure logic + wiring)

**Files:**
- Create: `src/main/java/com/josebieco/strideandsire/genetics/TraitInheritance.java`
- Modify: `src/main/java/com/josebieco/strideandsire/event/HorseBreedingGeneticsHandler.java`
- Test: `src/test/java/com/josebieco/strideandsire/genetics/TraitInheritanceTest.java`
- Test (GameTest): `src/main/java/com/josebieco/strideandsire/gametest/HorseBreedingTraitGameTest.java`

**Interfaces:**
- Consumes: `TraitState`, `LineageTrait` (Task 3).
- Produces: `TraitInheritance.rollChildState(TraitState parent1, TraitState
  parent2, RandomSource random) -> TraitState` — pure function: if both parents
  are `ABSENT`, uses the 2%/1% table; if either parent is `LATENT` or
  `MANIFEST`, uses the 35%/15% table (both "one parent" and "two parents"
  carrying use the same table, per Review Focus).
- Consumed by: `HorseBreedingGeneticsHandler`, called once per trait per
  breeding.

- [ ] **Step 1: Write failing unit tests with a seeded `RandomSource`: (a) both
  parents `ABSENT` and the draw lands in the 2% band returns `LATENT`; in the 1%
  band returns `MANIFEST`; otherwise `ABSENT`; (b) one parent `LATENT` (the other
  `ABSENT`) and the draw lands in the 35% band returns `LATENT`; (c) **both**
  parents `MANIFEST` uses the same 35%/15% thresholds as one parent (not a
  higher chance)**

- [ ] **Step 2: Run tests, verify they fail**

Run: `./gradlew test --tests TraitInheritanceTest`
Expected: FAIL.

- [ ] **Step 3: Implement `TraitInheritance.rollChildState`**

- [ ] **Step 4: Run tests, verify they pass**

Run: `./gradlew test --tests TraitInheritanceTest`
Expected: PASS, 3/3.

- [ ] **Step 5: Write a GameTest breeding a MANIFEST-Fearless parent with an
  ABSENT-Fearless parent repeatedly and asserting the foal ends up LATENT or
  MANIFEST noticeably more often than a parallel ABSENT×ABSENT breeding over the
  same number of trials**

- [ ] **Step 6: Wire `TraitInheritance.rollChildState` into
  `HorseBreedingGeneticsHandler` for all three traits, writing the result via
  `HorseGeneticsAccess.set` on the foal**

- [ ] **Step 7: Run the GameTest server, verify it passes**

Run: `./gradlew runGameTestServer`
Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "feat: add lineage trait inheritance for horse breeding"
```

---

### Task 6: Enable Glistering Melon as a horse breeding item

**Files:**
- Create: `src/main/java/com/josebieco/strideandsire/event/HorseBreedingFoodHandler.java`
- Test (GameTest): `src/main/java/com/josebieco/strideandsire/gametest/HorseBreedingFoodGameTest.java`

**Interfaces:**
- Consumes: nothing new.
- Produces: a handler making `Items.GLISTERING_MELON_SLICE` a valid
  breeding-trigger item for `AbstractHorse` (vanilla today only accepts Golden
  Apple and Golden Carrot) — whichever Forge hook this version exposes for
  per-item breeding eligibility (e.g. overriding/extending the animal's `isFood`
  check via event).
- Consumed by: Task 4/5's handler, which must correctly identify Glistering
  Melon as the `GLISTERING_MELON` `BreedingDietItem` when it was the item used.

- [ ] **Step 1: Write a GameTest feeding a Glistering Melon Slice to two
  horses and asserting they enter "love mode" (the vanilla breeding-ready state),
  exactly as Golden Carrot already does**

- [ ] **Step 2: Run the GameTest server, verify it fails**

Run: `./gradlew runGameTestServer`
Expected: FAIL — Glistering Melon is not recognized as horse food.

- [ ] **Step 3: Implement `HorseBreedingFoodHandler`**

- [ ] **Step 4: Run the GameTest server, verify it passes**

Run: `./gradlew runGameTestServer`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: allow Glistering Melon Slice to trigger horse breeding"
```

---

### Task 7: Breeder's Spyglass screen

**Files:**
- Create: `src/main/java/com/josebieco/strideandsire/menu/BreederSpyglassScreen.java`
- Create: `src/main/java/com/josebieco/strideandsire/event/BreederSpyglassInteractionHandler.java`
- Test (GameTest): `src/main/java/com/josebieco/strideandsire/gametest/BreederSpyglassGameTest.java`

**Interfaces:**
- Consumes: `AttributeGrading.gradeOf` (Task 1), `HorseGeneticsAccess` (Task 3).
- Produces: a shift+right-click handler that, when the player holds a
  `Items.SPYGLASS` and targets an eligible horse, opens `BreederSpyglassScreen`
  without consuming the Spyglass or applying any durability/cooldown; the screen
  exposes a package-visible static
  `BreederSpyglassScreen.buildReport(AbstractHorse) -> SpyglassReport` (a small
  record with the 3 grades and the trait/state map) as the testable seam, since
  GameTest cannot assert rendered screen pixels.

- [ ] **Step 1: Write a GameTest asserting `BreederSpyglassScreen.buildReport`
  returns the correct `Grade` for each of the 3 attributes on a horse with known
  attribute values, and correctly reports a LATENT Sure-footed trait as distinct
  from a MANIFEST one**

- [ ] **Step 2: Run the GameTest server, verify it fails**

Run: `./gradlew runGameTestServer`
Expected: FAIL.

- [ ] **Step 3: Implement `BreederSpyglassScreen.buildReport`, the screen's
  render method, and `BreederSpyglassInteractionHandler`**

- [ ] **Step 4: Run the GameTest server, verify it passes**

Run: `./gradlew runGameTestServer`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: add Breeder's Spyglass read-only inspection screen"
```

---

### Task 8: Sure-footed auto-step behavior

**Files:**
- Create: `src/main/java/com/josebieco/strideandsire/event/SureFootedStepHandler.java`
- Test (GameTest): `src/main/java/com/josebieco/strideandsire/gametest/SureFootedStepGameTest.java`

**Interfaces:**
- Consumes: `HorseGeneticsAccess` (Task 3).
- Produces: a handler raising the ridden horse's auto-step height to 1.5–2
  blocks (goat-like) only while `LineageTrait.SURE_FOOTED` is `MANIFEST` on that
  horse; a horse with it `LATENT` or `ABSENT` keeps vanilla step height.

- [ ] **Step 1: Write GameTests: (a) a MANIFEST Sure-footed horse climbs a
  1.5-block ledge without jumping; (b) a LATENT Sure-footed horse does not**

- [ ] **Step 2: Run the GameTest server, verify (a) fails**

Run: `./gradlew runGameTestServer`
Expected: FAIL.

- [ ] **Step 3: Implement `SureFootedStepHandler`**

- [ ] **Step 4: Run the GameTest server, verify both pass**

Run: `./gradlew runGameTestServer`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: add Sure-footed auto-step behavior"
```

---

### Task 9: Fearless damage-reaction suppression

**Files:**
- Create: `src/main/java/com/josebieco/strideandsire/event/FearlessPanicSuppressionHandler.java`
- Test (GameTest): `src/main/java/com/josebieco/strideandsire/gametest/FearlessPanicSuppressionGameTest.java`

**Interfaces:**
- Consumes: `HorseGeneticsAccess` (Task 3).
- Produces: a handler that, when a mounted horse with `LineageTrait.FEARLESS`
  `MANIFEST` takes sudden damage from a zombie or a nearby creeper explosion,
  suppresses the rearing/dismount reaction — while still applying the damage
  itself (Review Focus item 4).

- [ ] **Step 1: Write GameTests: (a) a MANIFEST Fearless horse takes zombie
  damage, loses health normally, but does not dismount its rider; (b) a horse
  without the trait dismounts its rider under the same damage, confirming no
  regression to vanilla/Fase-A-era behavior**

- [ ] **Step 2: Run the GameTest server, verify (a) fails**

Run: `./gradlew runGameTestServer`
Expected: FAIL.

- [ ] **Step 3: Implement `FearlessPanicSuppressionHandler`**

- [ ] **Step 4: Run the GameTest server, verify both pass**

Run: `./gradlew runGameTestServer`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: add Fearless damage-reaction suppression"
```

---

### Task 10: Draft trait unlocks 18-slot saddlebag capacity

**Files:**
- Modify: `src/main/java/com/josebieco/strideandsire/item/saddlebag/SaddlebagItem.java`
  (container capacity becomes 18 at the data-component level for every tier,
  since the Draft trait — not the material — is what unlocks the extra 9; see
  note below)
- Modify: `src/main/java/com/josebieco/strideandsire/menu/SaddlebagSlot.java` /
  the horse menu from Fase B (expose only the first 9 container slots by
  default; expose all 18 when the mounted horse's `LineageTrait.DRAFT` is
  `MANIFEST`, recomputed every time the menu opens per Review Focus item 5)
- Test (GameTest): `src/main/java/com/josebieco/strideandsire/gametest/DraftSaddlebagCapacityGameTest.java`

**Interfaces:**
- Consumes: `HorseGeneticsAccess` (Task 3), the Fase B saddlebag menu/slot
  classes.
- Produces: no new item or recipe — every existing saddlebag (any material tier)
  already has an 18-slot container; the horse menu is what gates 9 vs. 18 based
  on the mount's trait.

**Design note (flagged, not silently assumed):** the Fase B spec reserved the
18-slot "Alforje Duplo" exclusively for the Draft trait and never defined it as a
separate craftable item. This task implements that as a capacity unlock on the
*existing* saddlebag item (its container is sized 18 from the start, with only 9
exposed by default) rather than inventing a new item/tier/recipe — this is the
simplest reading consistent with "no overlap between material and trait" from
the Fase B spec, but it was not spelled out in either spec in this much detail,
so flag it for confirmation before this task ships if anything about it looks
wrong once built.

- [ ] **Step 1: Write a GameTest: (a) a saddlebag on a horse without Draft
  exposes exactly 9 addressable container slots in the menu; (b) the same
  saddlebag, once the horse's Draft trait becomes MANIFEST (set directly via
  `HorseGeneticsAccess` for the test), exposes all 18 on the next menu open; (c)
  items already placed in slots 1–9 survive the trait being gained or lost**

- [ ] **Step 2: Run the GameTest server, verify (b) fails**

Run: `./gradlew runGameTestServer`
Expected: FAIL — menu always exposes 9.

- [ ] **Step 3: Implement the container-size and menu-slot-count changes**

- [ ] **Step 4: Run the GameTest server, verify all three pass**

Run: `./gradlew runGameTestServer`
Expected: PASS, 3/3.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: unlock 18-slot saddlebag capacity via the Draft lineage trait"
```

---

### Task 11: Cross-cutting Review Focus regression tests

**Files:**
- Test (GameTest): `src/main/java/com/josebieco/strideandsire/gametest/GeneticsReviewFocusGameTest.java`

**Interfaces:**
- Consumes: everything from Tasks 1–10. Adds no new production code — closes
  Review Focus items not already pinned by an earlier task's own tests (items 1
  and 2 are already covered by Task 4; items 3, 4, and 5 get an explicit
  end-to-end test here in case an earlier task's implementation only handled a
  narrower case).

- [ ] **Step 1: Write a GameTest that: breeds two horses that both carry
  Sure-footed MANIFEST and asserts the inheritance chance matches the "at least
  one parent" table rather than a higher one; separately, damages a MANIFEST
  Fearless horse and asserts its health dropped by the expected amount (not
  zero); separately, equips a saddlebag on a Draft horse, removes the Draft
  trait via `HorseGeneticsAccess`, and asserts the menu now exposes only 9 slots
  with the first 9 items still intact**

- [ ] **Step 2: Run the GameTest server, verify it fails if any earlier task's
  implementation only special-cased the narrower scenario its own test posed**

Run: `./gradlew runGameTestServer`
Expected: FAIL only if a regression exists; if Tasks 1–10 were implemented per
their specs this may already pass — if so, skip straight to Step 4.

- [ ] **Step 3: Fix whichever task's implementation dropped one of these cases,
  if the test failed**

- [ ] **Step 4: Run the full GameTest suite once more end-to-end**

Run: `./gradlew runGameTestServer`
Expected: PASS — every GameTest across Tasks 2–11 green, plus `./gradlew test`
for the JUnit suites from Tasks 1, 4, and 5.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "test: pin genetics edge cases (dual-carrier inheritance, fearless damage, draft capacity live-recompute)"
```
