# Fase B — Alforjes Modulares (Saddlebags) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a saddle-dependent saddlebag equipment slot to every `AbstractHorse`
subtype, with four material tiers (fixed 9-slot container each), tier-specific
bonuses, a netherite smithing upgrade, saddle-removal auto-drop behavior, and
GameTest coverage — additive to, never replacing, the existing chest inventory on
Donkey/Mule.

**Architecture:** The saddlebag's 9-slot inventory lives on the `ItemStack` itself
via the vanilla "container" data component (the same mechanism shulker boxes use),
so content travels with the item automatically on drop, pickup, and smithing
upgrade. The horse's data attachment (same `AttachmentType` pattern as
`HorseshoeAttachment` from Fase A) holds only a reference to which saddlebag
`ItemStack` is equipped. The existing `HorseshoeHorseMenu` from Fase A is extended
with a saddle-gated saddlebag slot and, when filled, nine additional slots bound to
that stack's container. Bonus effects and the saddle-dependency rule hook the same
event classes Fase A established (horse death, equipment-change) rather than new
infrastructure.

**Tech Stack:** Minecraft Forge, Minecraft 26.3, Java 25, Forge GameTest framework,
JUnit 5 for pure-logic unit tests.

**Spec:** `docs/superpowers/specs/2026-10-16-fase-b-alforjes-design.md`

## Global Constraints

- Scope: Horse, Donkey, Mule, SkeletonHorse, ZombieHorse. Camel and Llama are out
  of scope (unchanged from Fase A).
- Every saddlebag has exactly 9 slots regardless of material — no tier changes
  slot count. The 18-slot "Alforje Duplo" is explicitly out of scope (Fase C).
- Donkey/Mule totals: 18 (native chest) + 9 (saddlebag) = 27 slots when both are
  equipped; the two inventories stay independent (no cross-contamination).
- No durability: a saddlebag is removable manually at any time, never breaks.
- Material bonus table (exact, from spec section 3):
  - Couro (Leather): no bonus.
  - Ferro (Iron): saddlebag contents survive the horse's death-by-explosion drop.
  - Ouro (Gold): reuses the Fase A gold-horseshoe Piglin-neutrality check, extended
    to also check "mount has a gold saddlebag equipped".
  - Netherite: saddlebag contents survive the horse's death-by-fire/lava drop.
- Netherite saddlebag has no standalone recipe — only via Smithing Table upgrade
  (Iron Saddlebag + Netherite Ingot + Netherite Upgrade Smithing Template),
  identical pattern to the Fase A horseshoe upgrade, and must preserve the
  container contents across the upgrade.
- The saddlebag slot only accepts an item when the horse already has a saddle
  equipped. Removing the saddle while a saddlebag is equipped auto-unequips the
  saddlebag (preserving its contents) and drops it as a world item at the horse's
  position — it never blocks saddle removal and never spills loose items.

## Review Focus

- A player attempts to place a saddlebag into the slot on a horse with no saddle
  — the slot must reject it (return to the player's cursor/inventory), not accept
  it silently or duplicate the stack.
- On Donkey/Mule, a shift-click ("quick move") on an item in the native chest
  inventory must never land it in the saddlebag's container slots, and vice versa
  — the two inventories must stay fully isolated through every menu interaction
  path, not just direct slot clicks.
- Upgrading an Iron saddlebag that currently holds items to Netherite via the
  Smithing Table must preserve every contained item exactly (same items, same
  stack sizes, same slot order) — not silently empty the container.
- A horse wearing an empty Iron or Netherite saddlebag dies to the corresponding
  damage type (explosion / fire) — the bonus-effect handler must not throw when
  there is nothing in the container to protect.
- Two mounted horses are both killed by the same area explosion in the same tick
  (e.g., one with an Iron saddlebag, one with a Leather saddlebag) — only the Iron
  one's contents survive; the Leather one must still lose its contents exactly as
  vanilla equipment-drop behavior would, with no cross-entity leakage of the
  protection flag.

---

### Task 1: Saddlebag material tiers and bonus flags (pure logic)

**Files:**
- Create: `src/main/java/com/josebieco/strideandsire/item/saddlebag/SaddlebagMaterial.java`
- Test: `src/test/java/com/josebieco/strideandsire/item/saddlebag/SaddlebagMaterialTest.java`

**Interfaces:**
- Produces: `enum SaddlebagMaterial { LEATHER, IRON, GOLD, NETHERITE }` with:
  - `int slotCount()` — always returns `9` for every constant (explicit, not
    computed, so a future Fase C change is a different enum/flag, not a edit here).
  - `boolean protectsContentsFromExplosion()` — `true` for IRON, NETHERITE, GOLD
    (per spec: "Ferro ou superior" is the implementer's reasonable read of the
    bonus table — Iron is the first tier to gain it and nothing in the spec removes
    it at higher tiers); `false` for LEATHER.
  - `boolean protectsContentsFromFire()` — `true` only for NETHERITE.
  - `boolean hasGoldPiglinPassive()` — `true` only for GOLD.
- Consumed by: Task 2 (item registration), Task 6 (bonus-effect handler), Task 4
  (menu slot sizing).

- [ ] **Step 1: Write failing tests for the bonus matrix and fixed slot count**

```java
@Test void leatherHasNoBonuses() {
    assertFalse(SaddlebagMaterial.LEATHER.protectsContentsFromExplosion());
    assertFalse(SaddlebagMaterial.LEATHER.protectsContentsFromFire());
    assertFalse(SaddlebagMaterial.LEATHER.hasGoldPiglinPassive());
}

@Test void ironProtectsFromExplosionOnly() {
    assertTrue(SaddlebagMaterial.IRON.protectsContentsFromExplosion());
    assertFalse(SaddlebagMaterial.IRON.protectsContentsFromFire());
    assertFalse(SaddlebagMaterial.IRON.hasGoldPiglinPassive());
}

@Test void goldHasPiglinPassiveAndExplosionProtection() {
    assertTrue(SaddlebagMaterial.GOLD.protectsContentsFromExplosion());
    assertTrue(SaddlebagMaterial.GOLD.hasGoldPiglinPassive());
    assertFalse(SaddlebagMaterial.GOLD.protectsContentsFromFire());
}

@Test void netheriteProtectsFromFireAndExplosion() {
    assertTrue(SaddlebagMaterial.NETHERITE.protectsContentsFromFire());
    assertTrue(SaddlebagMaterial.NETHERITE.protectsContentsFromExplosion());
}

@Test void everyTierHasNineSlots() {
    for (SaddlebagMaterial m : SaddlebagMaterial.values()) {
        assertEquals(9, m.slotCount());
    }
}
```

- [ ] **Step 2: Run tests, verify they fail with "class not found"**

Run: `./gradlew test --tests SaddlebagMaterialTest`
Expected: FAIL.

- [ ] **Step 3: Implement `SaddlebagMaterial` matching the Global Constraints bonus table**

- [ ] **Step 4: Run tests, verify all pass**

Run: `./gradlew test --tests SaddlebagMaterialTest`
Expected: PASS, 5/5.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/josebieco/strideandsire/item/saddlebag/SaddlebagMaterial.java \
        src/test/java/com/josebieco/strideandsire/item/saddlebag/SaddlebagMaterialTest.java
git commit -m "feat: add saddlebag material tiers and bonus flags"
```

---

### Task 2: SaddlebagItem with container storage and item registration

**Files:**
- Create: `src/main/java/com/josebieco/strideandsire/item/saddlebag/SaddlebagItem.java`
- Modify: `src/main/java/com/josebieco/strideandsire/registry/ModItems.java` (add
  `LEATHER_SADDLEBAG`, `IRON_SADDLEBAG`, `GOLD_SADDLEBAG`, `NETHERITE_SADDLEBAG`,
  ids `strideandsire:<material>_saddlebag`)
- Test (GameTest): `src/main/java/com/josebieco/strideandsire/gametest/SaddlebagItemGameTest.java`

**Interfaces:**
- Consumes: `SaddlebagMaterial` (Task 1).
- Produces:
  - `class SaddlebagItem extends Item` with `SaddlebagMaterial getMaterial()`, a
    constructor `SaddlebagItem(SaddlebagMaterial material, Item.Properties properties)`,
    and the vanilla "container" data component attached at item-stack creation
    sized to `material.slotCount()` (9 for every current tier).
  - `ModItems` gains the four `DeferredHolder<Item, SaddlebagItem>` fields listed
    above.
- Consumed by: Task 3 (attachment), Task 4 (menu slot filter), Task 7 (recipes),
  Task 8 (rendering).

- [ ] **Step 1: Write a GameTest asserting each of the four registered items is a
  `SaddlebagItem` with the correct `getMaterial()`, and that a fresh
  `ItemStack` of each has a 9-slot empty container component**

```java
@GameTest(template = "empty")
public static void saddlebagItemsAreRegisteredWithContainer(GameTestHelper helper) {
    helper.assertTrue(ModItems.LEATHER_SADDLEBAG.get() instanceof SaddlebagItem i
        && i.getMaterial() == SaddlebagMaterial.LEATHER, "leather saddlebag mismatch");
    // repeat for IRON, GOLD, NETHERITE, plus container-size assertion per stack
    helper.succeed();
}
```

- [ ] **Step 2: Run the GameTest server, verify it fails**

Run: `./gradlew runGameTestServer`
Expected: FAIL — `SaddlebagItem` / `ModItems` fields not found.

- [ ] **Step 3: Implement `SaddlebagItem` and extend `ModItems`'s existing
  `DeferredRegister` registration block with the four new entries**

- [ ] **Step 4: Run the GameTest server, verify it passes**

Run: `./gradlew runGameTestServer`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: register four saddlebag items with 9-slot containers"
```

---

### Task 3: Saddlebag data attachment on AbstractHorse

**Files:**
- Create: `src/main/java/com/josebieco/strideandsire/attachment/SaddlebagAttachment.java`
- Create: `src/main/java/com/josebieco/strideandsire/attachment/SaddlebagAccess.java`
- Modify: `src/main/java/com/josebieco/strideandsire/registry/ModAttachments.java`
  (add `SADDLEBAG` attachment type, keyed `strideandsire:saddlebag`)
- Test (GameTest): `src/main/java/com/josebieco/strideandsire/gametest/SaddlebagAttachmentGameTest.java`

**Interfaces:**
- Consumes: `SaddlebagItem` (Task 2).
- Produces: `record SaddlebagAttachment(ItemStack stack)` with `EMPTY` constant,
  following the exact same shape as `HorseshoeAttachment` from Fase A; static
  `SaddlebagAccess.get(AbstractHorse)` / `SaddlebagAccess.set(AbstractHorse, ItemStack)`.
- Consumed by: Task 4 (menu), Task 5 (saddle-dependency handler), Task 6 (bonus
  effects), Task 8 (rendering).

- [ ] **Step 1: Write a GameTest that sets a Gold saddlebag (with one item placed
  in its container) on a horse via `SaddlebagAccess.set`, and asserts
  `SaddlebagAccess.get(horse)` returns a stack of the same item that still
  contains that one item in its container**

- [ ] **Step 2: Run the GameTest server, verify it fails**

Run: `./gradlew runGameTestServer`
Expected: FAIL — attachment classes not found.

- [ ] **Step 3: Implement `SaddlebagAttachment`, `SaddlebagAccess`, and register
  the attachment type in `ModAttachments`, mirroring `HorseshoeAttachment`'s
  registration exactly**

- [ ] **Step 4: Run the GameTest server, verify it passes**

Run: `./gradlew runGameTestServer`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: add saddlebag data attachment for AbstractHorse"
```

---

### Task 4: Saddlebag slot and container sub-slots in the horse menu

**Files:**
- Modify: `src/main/java/com/josebieco/strideandsire/menu/HorseshoeHorseMenu.java`
  (rename class references stay — add the saddlebag slot and the conditional
  9-slot block; consider renaming the file/class to `StrideAndSireHorseMenu` if the
  implementer judges the name now misleading, updating every reference)
- Create: `src/main/java/com/josebieco/strideandsire/menu/SaddlebagSlot.java`
- Test (GameTest): `src/main/java/com/josebieco/strideandsire/gametest/SaddlebagMenuGameTest.java`

**Interfaces:**
- Consumes: `SaddlebagItem` (Task 2), `SaddlebagAccess` (Task 3), the existing
  `HorseshoeSlot`/menu wiring from Fase A.
- Produces: `class SaddlebagSlot extends Slot` whose `mayPlace(ItemStack stack)`
  returns `stack.getItem() instanceof SaddlebagItem` AND the menu's horse currently
  has a saddle equipped (the saddle check, not the saddle-removal reaction, is this
  task's responsibility — Task 5 owns the reverse direction); the menu class
  exposes the saddlebag's 9 container slots only while a saddlebag is equipped, laid
  out below the existing slots, and leaves the native chest slots (Donkey/Mule)
  untouched and independently addressed.
- Consumed by: nothing further in this plan for the slot itself; Task 9's
  isolation test exercises it end-to-end.

- [ ] **Step 1: Write GameTests: (a) `SaddlebagSlot.mayPlace` returns `false` for a
  saddle-less horse and `true` once a saddle is equipped; (b) on a Donkey with both
  a native chest and a Gold saddlebag equipped, the menu exposes 27 total
  addressable slots, and a quick-move from a chest slot never lands an item in a
  saddlebag container slot (and vice versa)**

- [ ] **Step 2: Run the GameTest server, verify both fail**

Run: `./gradlew runGameTestServer`
Expected: FAIL.

- [ ] **Step 3: Implement `SaddlebagSlot` and extend the horse menu with the
  saddlebag slot and its conditional 9 sub-slots**

One line on approach: bind the 9 sub-slots directly to the equipped saddlebag
stack's container component (via the same container-slot helper Minecraft uses for
shulker boxes), so no separate inventory object needs to be kept in sync.

- [ ] **Step 4: Run the GameTest server, verify both pass**

Run: `./gradlew runGameTestServer`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: add saddlebag slot and container sub-slots to horse menu"
```

---

### Task 5: Saddle-dependency enforcement and auto-drop on saddle removal

**Files:**
- Create: `src/main/java/com/josebieco/strideandsire/event/SaddlebagSaddleDependencyHandler.java`
- Test (GameTest): `src/main/java/com/josebieco/strideandsire/gametest/SaddlebagSaddleDependencyGameTest.java`

**Interfaces:**
- Consumes: `SaddlebagAccess` (Task 3).
- Produces: a handler subscribed to the horse's saddle-equipment-change event
  (the same equipment-change hook Fase A's passive-effect handler uses) that, when
  the saddle is removed and `SaddlebagAccess.get(horse)` is non-empty: clears the
  attachment to `SaddlebagAttachment.EMPTY` and spawns the previously-equipped
  stack as a world item entity at the horse's position, contents intact.

- [ ] **Step 1: Write GameTests: (a) removing a saddle from a horse with no
  saddlebag equipped does nothing extra (no item spawned, saddle removal succeeds
  normally); (b) removing a saddle from a horse with a Leather saddlebag holding
  three distinct items results in the saddle being removed, the attachment becoming
  empty, and exactly one world item entity of the saddlebag spawning whose
  container still holds those same three items**

- [ ] **Step 2: Run the GameTest server, verify (b) fails**

Run: `./gradlew runGameTestServer`
Expected: FAIL — no handler exists yet.

- [ ] **Step 3: Implement `SaddlebagSaddleDependencyHandler`**

- [ ] **Step 4: Run the GameTest server, verify both pass**

Run: `./gradlew runGameTestServer`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: auto-drop equipped saddlebag when saddle is removed"
```

---

### Task 6: Tier bonus effects (explosion/fire content protection, Gold Piglin passive)

**Files:**
- Create: `src/main/java/com/josebieco/strideandsire/event/SaddlebagBonusEffectHandler.java`
- Modify: `src/main/java/com/josebieco/strideandsire/event/HorseshoePassiveEffectHandler.java`
  (extend the existing Piglin-neutrality check to also test
  `SaddlebagAccess.get(mount).getItem() instanceof SaddlebagItem s && s.getMaterial().hasGoldPiglinPassive()`)
- Modify: `src/main/resources/data/minecraft/tags/item/fire_resistant` or the
  project's equivalent fire-resistant item tag (confirm the exact vanilla tag id
  used for dropped-item fire immunity in this Forge version) to include
  `strideandsire:netherite_saddlebag`, so the Netherite saddlebag's dropped-item
  fire immunity is the general vanilla mechanism, not bespoke code
- Test (GameTest): `src/main/java/com/josebieco/strideandsire/gametest/SaddlebagBonusEffectGameTest.java`

**Interfaces:**
- Consumes: `SaddlebagMaterial` (Task 1), `SaddlebagAccess` (Task 3).
- Produces: a handler reacting to the horse's death event that, when the death's
  damage source is an explosion and `SaddlebagAccess.get(horse)`'s material
  `protectsContentsFromExplosion()`, ensures the dropped saddlebag item entity
  (and its container contents) survives the explosion's own item-destruction pass
  (not just normal equipment drop); the fire/lava case for Netherite is covered by
  the fire-resistant item tag above rather than this handler, and this handler must
  not throw when the equipped saddlebag is `EMPTY` or has an empty container.

- [ ] **Step 1: Write GameTests: (a) a horse with an Iron saddlebag holding items
  dies to an explosion — the dropped saddlebag item entity exists afterward and its
  container still holds those items; (b) the same scenario with a Leather saddlebag
  — the dropped saddlebag's contents are lost, matching unmodified vanilla
  equipment-drop-then-explosion behavior; (c) a horse with an *empty* Netherite
  saddlebag dies to fire — handler runs without throwing and the dropped item
  survives the fire by virtue of the tag, not the handler; (d) two horses (one Iron,
  one Leather) die to the same single explosion in the same tick — only the Iron
  one's contents survive, confirming no cross-entity leakage; (e) the Gold Piglin
  passive check in `HorseshoePassiveEffectHandler` returns neutral when the mount
  has a Gold saddlebag even with a non-Gold horseshoe equipped**

- [ ] **Step 2: Run the GameTest server, verify all five fail**

Run: `./gradlew runGameTestServer`
Expected: FAIL.

- [ ] **Step 3: Implement `SaddlebagBonusEffectHandler`, extend the Piglin check in
  `HorseshoePassiveEffectHandler`, and add the Netherite saddlebag to the
  fire-resistant item tag**

- [ ] **Step 4: Run the GameTest server, verify all five pass**

Run: `./gradlew runGameTestServer`
Expected: PASS, 5/5.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: add saddlebag tier bonuses (explosion/fire protection, gold piglin passive)"
```

---

### Task 7: Crafting recipes and Netherite smithing upgrade

**Files:**
- Create: `src/main/resources/data/strideandsire/recipe/leather_saddlebag.json`
- Create: `src/main/resources/data/strideandsire/recipe/iron_saddlebag.json`
- Create: `src/main/resources/data/strideandsire/recipe/gold_saddlebag.json`
- Create: `src/main/resources/data/strideandsire/recipe/netherite_saddlebag_smithing.json`
- Modify: `src/main/java/com/josebieco/strideandsire/recipe/HorseshoeSmithingUpgradeRecipe.java`
  or add a sibling `SaddlebagSmithingUpgradeRecipe.java` if the horseshoe recipe
  class is not generic enough to reuse — the implementer decides based on how
  parameterized the existing class already is
- Test (GameTest): `src/main/java/com/josebieco/strideandsire/gametest/SaddlebagRecipeGameTest.java`

**Interfaces:**
- Consumes: `ModItems` saddlebag entries (Task 2).
- Produces: a shaped crafting recipe per Leather/Iron/Gold (leather + ingots,
  shape at the implementer's discretion since the spec does not pin one — reuse
  the Fase A horseshoe "U" shape convention for visual consistency across the mod)
  and a `smithing_transform` recipe for Netherite (Iron Saddlebag + Netherite
  Ingot + `minecraft:netherite_upgrade_smithing_template`) that preserves the input
  stack's container contents exactly.

- [ ] **Step 1: Write a GameTest crafting each of the four recipes via
  `RecipeManager` lookups, with the smithing case using an Iron saddlebag whose
  container already holds two distinct items, asserting the Netherite result's
  container holds the same two items in the same slots**

- [ ] **Step 2: Run the GameTest server, verify it fails**

Run: `./gradlew runGameTestServer`
Expected: FAIL.

- [ ] **Step 3: Author the four recipe JSON files and the smithing recipe class
  handling container carryover**

- [ ] **Step 4: Run the GameTest server, verify it passes**

Run: `./gradlew runGameTestServer`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: add saddlebag crafting recipes and netherite smithing upgrade"
```

---

### Task 8: Saddlebag rendering layer

**Files:**
- Create: `src/main/java/com/josebieco/strideandsire/client/SaddlebagLayer.java`
- Create: `src/main/resources/assets/strideandsire/textures/entity/saddlebag/leather.png`
  (and `iron.png`, `gold.png`, `netherite.png`)
- Modify: `src/main/java/com/josebieco/strideandsire/render/HorseshoeRendering.java`
  (or wherever `EntityRenderersEvent.AddLayers` is handled) to also attach
  `SaddlebagLayer`
- Test (GameTest): `src/main/java/com/josebieco/strideandsire/gametest/SaddlebagLayerVisibilityGameTest.java`

**Interfaces:**
- Consumes: `SaddlebagAccess` (Task 3), `SaddlebagMaterial` (Task 1).
- Produces: `class SaddlebagLayer<T extends AbstractHorse, M extends EntityModel<T>> extends RenderLayer<T, M>`
  with a package-visible `static SaddlebagMaterial materialToRender(AbstractHorse horse)`
  returning `null` when no saddlebag is equipped, mirroring `HorseshoeLayer`'s
  testable seam from Fase A.

- [ ] **Step 1: Write a GameTest asserting `SaddlebagLayer.materialToRender(horse)`
  is `null` with no saddlebag equipped, and matches the correct tier for each of
  the four materials**

- [ ] **Step 2: Run the GameTest server, verify it fails**

Run: `./gradlew runGameTestServer`
Expected: FAIL.

- [ ] **Step 3: Implement `SaddlebagLayer.materialToRender`, the render method
  drawing the four bag textures on the saddle's sides, and the registration
  wiring; placeholder textures are acceptable**

- [ ] **Step 4: Run the GameTest server, verify it passes**

Run: `./gradlew runGameTestServer`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: render equipped saddlebag on horse saddle sides"
```

---

### Task 9: Cross-cutting Review Focus regression tests

**Files:**
- Test (GameTest): `src/main/java/com/josebieco/strideandsire/gametest/SaddlebagReviewFocusGameTest.java`

**Interfaces:**
- Consumes: everything from Tasks 2–8. Adds no new production code — closes
  Review Focus items not already pinned by an earlier task's own tests (items 1
  and 3 are already covered by Tasks 4 and 7; items 2, 4, and 5 get their own
  end-to-end test here in case any earlier task's implementation only handled the
  narrow case its own unit test posed).

- [ ] **Step 1: Write a GameTest that: equips a Gold saddlebag with items on a
  Donkey that also has a native chest with its own different items, performs a
  quick-move in both directions between chest and saddlebag slots and asserts no
  item ever crosses, then kills the Donkey with an explosion and asserts the Gold
  saddlebag's contents survive while a parallel Leather-saddlebag Donkey's do not**

- [ ] **Step 2: Run the GameTest server, verify it fails if any earlier task's
  implementation only special-cased a single-inventory scenario**

Run: `./gradlew runGameTestServer`
Expected: FAIL only if a regression exists; if Tasks 2–8 were implemented per
their specs this may already pass — if so, skip straight to Step 4.

- [ ] **Step 3: Fix whichever task's implementation let inventories cross or
  dropped the per-entity protection flag, if the test failed**

- [ ] **Step 4: Run the full GameTest suite once more end-to-end**

Run: `./gradlew runGameTestServer`
Expected: PASS — every GameTest across Tasks 2–9 green.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "test: pin saddlebag/chest isolation and per-entity explosion protection"
```
