# Fase A — Sistema de Ferraduras (Horseshoes) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a horseshoe equipment slot to all `AbstractHorse` subtypes (Horse, Donkey,
Mule, SkeletonHorse, ZombieHorse) with four material tiers, three custom
horseshoe-exclusive enchantments, passive tier effects, durability/breakage,
crafting/upgrade recipes, hoof rendering, and GameTest coverage.

**Architecture:** A data attachment on `AbstractHorse` holds the equipped horseshoe
`ItemStack`. A custom menu/screen extends the vanilla horse inventory screen with one
extra slot gated to `HorseshoeItem`. Event handlers (fall damage, fire/lava damage,
living tick, Piglin neutrality) read the attachment to apply tier/enchantment effects.
Pure eligibility/durability logic is extracted into plain Java classes so it is
JUnit-testable without a running game instance; everything that needs a live world
(attachment sync, GUI, event hooks, recipes) is covered by Forge GameTests.

**Tech Stack:** Minecraft Forge (`net.minecraftforge.gradle`), Minecraft 26.3, Java 25,
Forge GameTest framework, JUnit 5 for pure-logic unit tests.

**Spec:** `docs/superpowers/specs/2026-10-15-fase-a-ferraduras-design.md`

## Global Constraints

- Modid: `strideandsire`. Root package: `com.josebieco.strideandsire`. Display name:
  "Stride & Sire" (per CurseForge rule: no "Minecraft", no loader/version/category
  words in the project name).
- Only `AbstractHorse` subtypes get the horseshoe slot: Horse, Donkey, Mule,
  SkeletonHorse, ZombieHorse. Camel and Llama are explicitly out of scope.
- Durability (base, before Unbreaking): Copper 100, Iron 200, Gold 80, Netherite 400.
- Netherite horseshoe has **no standalone recipe** — only via Smithing Table upgrade
  (Iron Horseshoe + Netherite Ingot + Netherite Upgrade Smithing Template), matching
  vanilla tool/armor upgrade behavior.
- Enchantment eligibility matrix (exact, from spec section 4):
  - Passos de Pena (Feather Step): Copper, Iron, Netherite. NOT Gold.
  - Passos Congelantes (Frost Step): Copper, Iron. NOT Gold, NOT Netherite.
  - Caminhante de Lava (Lava Walker): Netherite only.
- Gold passive effects (always on, not enchantments): movement speed boost, Piglin
  neutrality while mounted. Netherite passive effect (always on): full fire/lava
  damage immunity.
- Depleting a horseshoe's durability to zero destroys it (no anvil repair with raw
  material); Mending/Unbreaking apply normally.
- No existing armor/saddle slot is touched. The horseshoe slot is strictly additive.

## Review Focus

- A player puts a non-horseshoe item (e.g. a diamond) into the new slot — the slot
  must reject it, not silently accept or crash the menu.
- A player tries to enchant a Gold or Netherite horseshoe with an ineligible
  enchantment (e.g. Frost Step on Netherite) via anvil + enchanted book — the
  application must be rejected the same way vanilla rejects Soul Speed on chestplates.
- The ridden horse dies or is unmounted while a timed effect (Frost Step ice,
  temporary fire immunity visuals) is active — no lingering effect or crash after
  the entity reference is gone.
- A horse already carrying a damaged Iron horseshoe gets ridden through the
  Netherite smithing upgrade — enchantments and remaining durability ratio must
  carry over, exactly like vanilla tool/armor upgrades.
- Two entities (e.g. player's horse and a stray wild horse) are attachment-checked
  in the same tick (Piglin neutrality, lava-walking) — effects must be per-entity,
  never leak from one horse's attachment to another's.

---

### Task 1: Project rebrand and test infrastructure

**Files:**
- Modify: `build.gradle` (group, `minecraft.mod` id references, add JUnit 5 dependency)
- Modify: `gradle.properties` if it carries a mod-id/group property
- Modify: `settings.gradle` (root project name)
- Modify: `src/main/resources/META-INF/mods.toml` (modid `strideandsire`, displayName
  `Stride & Sire`, description)
- Move: `src/main/java/com/example/examplemod/` → `src/main/java/com/josebieco/strideandsire/`
- Modify: main mod class → `StrideAndSire.java`, `@Mod("strideandsire")`
- Create: `src/test/java/com/josebieco/strideandsire/PlaceholderTest.java`

**Interfaces:**
- Produces: modid constant `StrideAndSire.MODID = "strideandsire"`, used by every
  later registration task.

- [ ] **Step 1: Add a JUnit 5 dependency and `test` task wiring to `build.gradle`**

Use `testImplementation platform('org.junit:junit-bom:5.11.0')` and
`testImplementation 'org.junit.jupiter:junit-jupiter'`, plus
`tasks.named('test') { useJUnitPlatform() }`.

- [ ] **Step 2: Move and rename the package, update `mods.toml`, `build.gradle` group, and the main mod class's `@Mod` id to `strideandsire`**

- [ ] **Step 3: Write `PlaceholderTest` asserting `StrideAndSire.MODID.equals("strideandsire")`**

- [ ] **Step 4: Run `./gradlew test` and verify `PlaceholderTest` passes, then run `./gradlew build` and verify the mod jar still builds with the new modid**

Run: `./gradlew test build`
Expected: BUILD SUCCESSFUL, test report shows `PlaceholderTest` passed.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "chore: rebrand project to Stride & Sire (strideandsire)"
```

---

### Task 2: Horseshoe material tiers and eligibility matrix (pure logic)

**Files:**
- Create: `src/main/java/com/josebieco/strideandsire/item/horseshoe/HorseshoeMaterial.java`
- Test: `src/test/java/com/josebieco/strideandsire/item/horseshoe/HorseshoeMaterialTest.java`

**Interfaces:**
- Produces: `enum HorseshoeMaterial { COPPER, IRON, GOLD, NETHERITE }` with:
  - `int baseDurability()`
  - `boolean allowsEnchantment(HorseshoeEnchantmentKind kind)` where
    `HorseshoeEnchantmentKind` is a small enum `{ FEATHER_STEP, FROST_STEP, LAVA_WALKER }`
    defined in the same file.
  - `boolean hasGoldPassive()`, `boolean hasNetheritePassive()` (used by Task 6).
- Consumed by: Task 3 (item registration), Task 5 (attachment), Task 6 (effects),
  Task 7 (enchantment eligibility enforcement).

- [ ] **Step 1: Write failing tests for the eligibility matrix and durability values from Global Constraints**

```java
@Test void copperAllowsFeatherStepAndFrostStepNotLavaWalker() {
    assertTrue(HorseshoeMaterial.COPPER.allowsEnchantment(HorseshoeEnchantmentKind.FEATHER_STEP));
    assertTrue(HorseshoeMaterial.COPPER.allowsEnchantment(HorseshoeEnchantmentKind.FROST_STEP));
    assertFalse(HorseshoeMaterial.COPPER.allowsEnchantment(HorseshoeEnchantmentKind.LAVA_WALKER));
}

@Test void goldAllowsNoExclusiveEnchantmentAndHasGoldPassive() {
    for (HorseshoeEnchantmentKind k : HorseshoeEnchantmentKind.values()) {
        assertFalse(HorseshoeMaterial.GOLD.allowsEnchantment(k));
    }
    assertTrue(HorseshoeMaterial.GOLD.hasGoldPassive());
}

@Test void netheriteAllowsOnlyLavaWalkerAndFeatherStep() {
    assertTrue(HorseshoeMaterial.NETHERITE.allowsEnchantment(HorseshoeEnchantmentKind.LAVA_WALKER));
    assertTrue(HorseshoeMaterial.NETHERITE.allowsEnchantment(HorseshoeEnchantmentKind.FEATHER_STEP));
    assertFalse(HorseshoeMaterial.NETHERITE.allowsEnchantment(HorseshoeEnchantmentKind.FROST_STEP));
    assertTrue(HorseshoeMaterial.NETHERITE.hasNetheritePassive());
}

@Test void baseDurabilityMatchesSpec() {
    assertEquals(100, HorseshoeMaterial.COPPER.baseDurability());
    assertEquals(200, HorseshoeMaterial.IRON.baseDurability());
    assertEquals(80, HorseshoeMaterial.GOLD.baseDurability());
    assertEquals(400, HorseshoeMaterial.NETHERITE.baseDurability());
}
```

- [ ] **Step 2: Run tests, verify they fail with "class not found"**

Run: `./gradlew test --tests HorseshoeMaterialTest`
Expected: FAIL — `HorseshoeMaterial` does not exist.

- [ ] **Step 3: Implement `HorseshoeMaterial` and `HorseshoeEnchantmentKind` exactly matching the Global Constraints eligibility matrix and durability table**

- [ ] **Step 4: Run tests, verify all pass**

Run: `./gradlew test --tests HorseshoeMaterialTest`
Expected: PASS, 4/4.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/josebieco/strideandsire/item/horseshoe/HorseshoeMaterial.java \
        src/test/java/com/josebieco/strideandsire/item/horseshoe/HorseshoeMaterialTest.java
git commit -m "feat: add horseshoe material tiers and enchantment eligibility matrix"
```

---

### Task 3: HorseshoeItem and item registration

**Files:**
- Create: `src/main/java/com/josebieco/strideandsire/item/horseshoe/HorseshoeItem.java`
- Create: `src/main/java/com/josebieco/strideandsire/registry/ModItems.java`
- Modify: `StrideAndSire.java` (call `ModItems.register(modEventBus)` in constructor)
- Test (GameTest): `src/main/java/com/josebieco/strideandsire/gametest/HorseshoeItemGameTest.java`

**Interfaces:**
- Consumes: `HorseshoeMaterial` (Task 2).
- Produces:
  - `class HorseshoeItem extends Item` with `HorseshoeMaterial getMaterial()` and a
    constructor `HorseshoeItem(HorseshoeMaterial material, Item.Properties properties)`
    that sets max damage from `material.baseDurability()`.
  - `ModItems` with public `DeferredHolder<Item, HorseshoeItem>` fields:
    `COPPER_HORSESHOE`, `IRON_HORSESHOE`, `GOLD_HORSESHOE`, `NETHERITE_HORSESHOE`,
    each registered with id `strideandsire:<material>_horseshoe`.
- Consumed by: Task 4 (menu slot filter), Task 5 (attachment), Task 8 (recipes),
  Task 9 (rendering).

- [ ] **Step 1: Write a GameTest asserting each of the four registered items is
  non-null, is an instance of `HorseshoeItem`, and `getMaterial()` matches its tier**

```java
@GameTest(template = "empty")
public static void horseshoeItemsAreRegisteredWithCorrectMaterial(GameTestHelper helper) {
    helper.assertTrue(ModItems.COPPER_HORSESHOE.get() instanceof HorseshoeItem i
        && i.getMaterial() == HorseshoeMaterial.COPPER, "copper horseshoe mismatch");
    // repeat for IRON, GOLD, NETHERITE
    helper.succeed();
}
```

- [ ] **Step 2: Run the GameTest server, verify it fails (class/registry entries missing)**

Run: `./gradlew runGameTestServer`
Expected: FAIL — `ModItems` / `HorseshoeItem` not found.

- [ ] **Step 3: Implement `HorseshoeItem` and `ModItems` (DeferredRegister on the
  `Registries.ITEM` registry), register the deferred register's event in
  `StrideAndSire`'s constructor**

- [ ] **Step 4: Run the GameTest server, verify the test passes**

Run: `./gradlew runGameTestServer`
Expected: PASS — `horseshoeItemsAreRegisteredWithCorrectMaterial` succeeds.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: register four horseshoe items across material tiers"
```

---

### Task 4: Horseshoe data attachment on AbstractHorse

**Files:**
- Create: `src/main/java/com/josebieco/strideandsire/attachment/HorseshoeAttachment.java`
- Create: `src/main/java/com/josebieco/strideandsire/registry/ModAttachments.java`
- Modify: `StrideAndSire.java` (register attachment types)
- Test (GameTest): `src/main/java/com/josebieco/strideandsire/gametest/HorseshoeAttachmentGameTest.java`

**Interfaces:**
- Consumes: `HorseshoeItem` (Task 3).
- Produces:
  - `record HorseshoeAttachment(ItemStack stack)` with a static `EMPTY` constant
    (`ItemStack.EMPTY`), serializable via a `Codec<HorseshoeAttachment>`.
  - `ModAttachments.HORSESHOE` — an `AttachmentType<HorseshoeAttachment>` keyed
    `strideandsire:horseshoe`, default `HorseshoeAttachment.EMPTY`.
  - Helper statics on `HorseshoeAttachment` (or a small `HorseshoeAccess` utility):
    `static ItemStack get(AbstractHorse horse)`,
    `static void set(AbstractHorse horse, ItemStack stack)`.
- Consumed by: Task 5 (menu slot), Task 6 (effects), Task 7 (rendering).

- [ ] **Step 1: Write a GameTest that spawns a horse, calls
  `HorseshoeAccess.set(horse, new ItemStack(ModItems.IRON_HORSESHOE.get()))`, and
  asserts `HorseshoeAccess.get(horse).is(ModItems.IRON_HORSESHOE.get())`**

- [ ] **Step 2: Run the GameTest server, verify it fails**

Run: `./gradlew runGameTestServer`
Expected: FAIL — attachment classes not found.

- [ ] **Step 3: Implement `HorseshoeAttachment`, `ModAttachments`, and
  `HorseshoeAccess`, registering the attachment type on the attachment registry
  event**

- [ ] **Step 4: Run the GameTest server, verify it passes**

Run: `./gradlew runGameTestServer`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: add horseshoe data attachment for AbstractHorse"
```

---

### Task 5: Custom horse inventory menu with horseshoe slot

**Files:**
- Create: `src/main/java/com/josebieco/strideandsire/menu/HorseshoeHorseMenu.java`
- Create: `src/main/java/com/josebieco/strideandsire/menu/HorseshoeSlot.java`
- Create: `src/main/java/com/josebieco/strideandsire/registry/ModMenus.java`
- Modify: wherever the vanilla horse inventory screen is opened (mixin or event
  redirect — pick whichever this Forge version exposes for "interact with horse,
  empty hand, opens inventory") to open `HorseshoeHorseMenu` instead of vanilla's
- Test (GameTest): `src/main/java/com/josebieco/strideandsire/gametest/HorseshoeMenuGameTest.java`

**Interfaces:**
- Consumes: `HorseshoeItem` (Task 3), `HorseshoeAccess` (Task 4).
- Produces: `class HorseshoeHorseMenu extends AbstractContainerMenu` exposing the
  same saddle/armor/inventory slots as vanilla plus one `HorseshoeSlot` at a fixed
  index; `HorseshoeSlot.mayPlace(ItemStack stack)` returns
  `stack.getItem() instanceof HorseshoeItem`.
- Consumed by: nothing further in this plan (terminal UI piece), but the slot's
  `mayPlace` contract is what Review Focus item 1 tests against.

- [ ] **Step 1: Write a GameTest that opens `HorseshoeHorseMenu` for a test horse,
  attempts `horseshoeSlot.mayPlace(new ItemStack(Items.DIAMOND))` (expect false)
  and `horseshoeSlot.mayPlace(new ItemStack(ModItems.GOLD_HORSESHOE.get()))`
  (expect true), and asserts the saddle slot is unaffected**

- [ ] **Step 2: Run the GameTest server, verify it fails**

Run: `./gradlew runGameTestServer`
Expected: FAIL — `HorseshoeHorseMenu` not found.

- [ ] **Step 3: Implement `HorseshoeSlot`, `HorseshoeHorseMenu`, `ModMenus`, and the
  interaction redirect so right-clicking an eligible horse with an empty hand opens
  this menu instead of the vanilla one**

- [ ] **Step 4: Run the GameTest server, verify it passes**

Run: `./gradlew runGameTestServer`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: add horseshoe slot to horse inventory menu"
```

---

### Task 6: Passive tier effects (Gold speed/Piglin, Netherite fire/lava immunity)

**Files:**
- Create: `src/main/java/com/josebieco/strideandsire/event/HorseshoePassiveEffectHandler.java`
- Test (GameTest): `src/main/java/com/josebieco/strideandsire/gametest/HorseshoePassiveEffectGameTest.java`

**Interfaces:**
- Consumes: `HorseshoeAccess` (Task 4), `HorseshoeMaterial.hasGoldPassive()` /
  `hasNetheritePassive()` (Task 2).
- Produces: a `@Mod.EventBusSubscriber` class handling:
  - `EntityAttributeModificationEvent`-style add/remove of a movement speed
    `AttributeModifier` keyed `strideandsire:gold_horseshoe_speed` when the attachment
    becomes/stops being a Gold horseshoe (checked on a tick or equip/unequip event).
  - Fire/lava `LivingDamageEvent` (or equivalent) cancellation when the damaged
    entity is an `AbstractHorse` whose attachment `hasNetheritePassive()`.
  - A Piglin-neutrality hook alongside/extending the existing "player wearing gold"
    check, additionally neutral when the player's mount has `hasGoldPassive()`.
- Consumed by: Review Focus items 3 and 5 (per-entity isolation).

- [ ] **Step 1: Write three GameTests: (a) horse with Gold horseshoe has the speed
  modifier present, a horse with any other tier does not; (b) horse with Netherite
  horseshoe takes zero damage from a fire/lava damage source, a horse with Iron
  horseshoe takes normal damage; (c) two horses side by side, only the one with a
  Gold horseshoe carries the speed modifier (isolation)**

- [ ] **Step 2: Run the GameTest server, verify all three fail**

Run: `./gradlew runGameTestServer`
Expected: FAIL — handler class not found / no effect applied.

- [ ] **Step 3: Implement `HorseshoePassiveEffectHandler`**

One line on approach: react to attachment changes (on equip/unequip in the menu's
`setChanged`, not a per-tick scan) to add/remove the speed `AttributeModifier`, and
subscribe to the fire/lava damage event unconditionally, checking the attachment at
damage time.

- [ ] **Step 4: Run the GameTest server, verify all three pass**

Run: `./gradlew runGameTestServer`
Expected: PASS, 3/3.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: add Gold speed/Piglin and Netherite fire-immunity passive effects"
```

---

### Task 7: Custom enchantments (Feather Step, Frost Step, Lava Walker) with eligibility enforcement

**Files:**
- Create: `src/main/resources/data/strideandsire/enchantment/feather_step.json`
- Create: `src/main/resources/data/strideandsire/enchantment/frost_step.json`
- Create: `src/main/resources/data/strideandsire/enchantment/lava_walker.json`
- Create: `src/main/java/com/josebieco/strideandsire/enchantment/HorseshoeEnchantments.java`
  (resource-key constants + a static eligibility gate used by the anvil/enchanting
  logic)
- Create: `src/main/java/com/josebieco/strideandsire/event/HorseshoeEnchantmentEffectHandler.java`
  (fall damage negation, frost-walker-style block freezing, lava no-sink behavior)
- Modify: event handler registration in `StrideAndSire.java`
- Test (GameTest): `src/main/java/com/josebieco/strideandsire/gametest/HorseshoeEnchantmentGameTest.java`

**Interfaces:**
- Consumes: `HorseshoeMaterial.allowsEnchantment(HorseshoeEnchantmentKind)` (Task 2),
  `HorseshoeAccess` (Task 4).
- Produces: enchantment resource keys `strideandsire:feather_step`,
  `strideandsire:frost_step`, `strideandsire:lava_walker`; a static
  `HorseshoeEnchantments.isEligible(ItemStack horseshoeStack, ResourceKey<Enchantment> enchantment)`
  used wherever enchanting/anvil-combining is validated for horseshoes.
- Consumed by: nothing further in this plan; this is the terminal piece for the
  spec's encantamentos section.

- [ ] **Step 1: Write GameTests covering: (a) applying `frost_step` to a Netherite
  horseshoe via the eligibility gate is rejected; (b) applying `lava_walker` to an
  Iron horseshoe is rejected; (c) a horse with a Feather-Step-enchanted horseshoe
  takes zero fall damage from a fall that would otherwise deal damage; (d) a horse
  with Frost-Step freezes water blocks under it while moving over water, matching
  vanilla Frost Walker II's radius; (e) a horse with Lava-Walker on Netherite does
  not sink when walked onto a lava block; (f) a horse with active Frost-Step ice
  tiles nearby is killed (or the player dismounts and the horse is removed) mid-tick
  — assert the next server tick runs with no exception and no orphaned ice-decay
  task referencing the removed entity**

- [ ] **Step 2: Run the GameTest server, verify all six fail**

Run: `./gradlew runGameTestServer`
Expected: FAIL.

- [ ] **Step 3: Implement the three enchantment JSON definitions (category/slot
  restricted to horseshoe items), `HorseshoeEnchantments.isEligible` wired into
  anvil/enchanting-table validation, and `HorseshoeEnchantmentEffectHandler`
  reusing/adapting vanilla `FrostWalkerEnchantment`-style block freezing and
  Strider-style lava traversal logic, gated by `EnchantmentHelper` lookups on the
  equipped horseshoe stack; guard every scheduled/deferred effect (ice decay,
  lava-walk state) with an `isAlive()`/`isRemoved()` check on the horse before
  acting**

- [ ] **Step 4: Run the GameTest server, verify all six pass**

Run: `./gradlew runGameTestServer`
Expected: PASS, 6/6.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: add Feather Step, Frost Step, and Lava Walker horseshoe enchantments"
```

---

### Task 8: Durability loss and breakage

**Files:**
- Create: `src/main/java/com/josebieco/strideandsire/event/HorseshoeDurabilityHandler.java`
- Test (GameTest): `src/main/java/com/josebieco/strideandsire/gametest/HorseshoeDurabilityGameTest.java`

**Interfaces:**
- Consumes: `HorseshoeAccess` (Task 4).
- Produces: a handler that decrements the equipped horseshoe's damage value on the
  gameplay triggers chosen in Task 6/7 (fall damage event, frost-step tick, lava
  exposure tick), respecting Unbreaking (via vanilla
  `ItemStack.hurtAndBreak`-equivalent, which already rolls Unbreaking odds) and
  Mending (already handled by vanilla XP-pickup logic once the stack is a normal
  damageable item), and clears the attachment to `HorseshoeAttachment.EMPTY` when
  the stack breaks.

- [ ] **Step 1: Write a GameTest that sets a horseshoe's damage to `maxDamage - 1`,
  triggers one durability-consuming event with Unbreaking absent, and asserts the
  attachment is now `HorseshoeAttachment.EMPTY`**

- [ ] **Step 2: Run the GameTest server, verify it fails**

Run: `./gradlew runGameTestServer`
Expected: FAIL.

- [ ] **Step 3: Implement `HorseshoeDurabilityHandler`, calling the stack's standard
  damage-and-break path and writing the resulting (possibly empty) stack back via
  `HorseshoeAccess.set`**

- [ ] **Step 4: Run the GameTest server, verify it passes**

Run: `./gradlew runGameTestServer`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: wire horseshoe durability loss and breakage into attachment"
```

---

### Task 9: Crafting and Netherite smithing upgrade recipes

**Files:**
- Create: `src/main/resources/data/strideandsire/recipe/copper_horseshoe.json`
- Create: `src/main/resources/data/strideandsire/recipe/iron_horseshoe.json`
- Create: `src/main/resources/data/strideandsire/recipe/gold_horseshoe.json`
- Create: `src/main/resources/data/strideandsire/recipe/netherite_horseshoe_smithing.json`
- Test (GameTest): `src/main/java/com/josebieco/strideandsire/gametest/HorseshoeRecipeGameTest.java`

**Interfaces:**
- Consumes: `ModItems` (Task 3).
- Produces: a shaped crafting recipe per Copper/Iron/Gold (5 ingots in the "U" shape
  from spec section 5) and a `smithing_transform` recipe for Netherite (Iron
  Horseshoe + Netherite Ingot + `minecraft:netherite_upgrade_smithing_template`)
  that preserves enchantments and the input's damage ratio, matching vanilla
  netherite tool/armor upgrade semantics.

- [ ] **Step 1: Write a GameTest crafting each of the four recipes via
  `RecipeManager` lookups (three `CraftingInput` checks, one `SmithingRecipeInput`
  check including an enchanted, partially-damaged Iron horseshoe) and asserting the
  Netherite result keeps the same enchantments and the same damage *ratio* as the
  input**

- [ ] **Step 2: Run the GameTest server, verify it fails (recipes missing)**

Run: `./gradlew runGameTestServer`
Expected: FAIL.

- [ ] **Step 3: Author the four recipe JSON files**

- [ ] **Step 4: Run the GameTest server, verify it passes**

Run: `./gradlew runGameTestServer`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: add horseshoe crafting recipes and netherite smithing upgrade"
```

---

### Task 10: Hoof rendering layer

**Files:**
- Create: `src/main/java/com/josebieco/strideandsire/client/HorseshoeLayer.java`
- Create: `src/main/resources/assets/strideandsire/textures/entity/horseshoe/copper.png`
  (and `iron.png`, `gold.png`, `netherite.png`)
- Modify: client-side renderer registration (wherever `EntityRenderersEvent.AddLayers`
  is handled) to attach `HorseshoeLayer` to the Horse/Donkey/Mule/SkeletonHorse/
  ZombieHorse renderers
- Test (GameTest): `src/main/java/com/josebieco/strideandsire/gametest/HorseshoeLayerVisibilityGameTest.java`
  — note: this GameTest asserts the layer's *visibility predicate*
  (`shouldRender(AbstractHorse)` returning false when the attachment is empty, true
  and tier-correct otherwise), not pixel output, since GameTest cannot assert
  rendered frames.

**Interfaces:**
- Consumes: `HorseshoeAccess` (Task 4), `HorseshoeMaterial` (Task 2).
- Produces: `class HorseshoeLayer<T extends AbstractHorse, M extends EntityModel<T>> extends RenderLayer<T, M>`
  exposing a package-visible `static HorseshoeMaterial materialToRender(AbstractHorse horse)`
  returning `null` when the attachment is empty — this is the pure, testable seam.

- [ ] **Step 1: Write a GameTest asserting `HorseshoeLayer.materialToRender(horse)`
  is `null` for a horse with no horseshoe, and equals the correct `HorseshoeMaterial`
  for a horse with each of the four tiers equipped**

- [ ] **Step 2: Run the GameTest server, verify it fails**

Run: `./gradlew runGameTestServer`
Expected: FAIL.

- [ ] **Step 3: Implement `HorseshoeLayer.materialToRender`, the render method
  drawing the four hoof textures, and the renderer-registration wiring; placeholder
  textures are acceptable for this task (visual polish is not spec-gated)**

- [ ] **Step 4: Run the GameTest server, verify it passes**

Run: `./gradlew runGameTestServer`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: render equipped horseshoe on horse hooves"
```

---

### Task 11: Cross-cutting Review Focus regression tests

**Files:**
- Test (GameTest): `src/main/java/com/josebieco/strideandsire/gametest/HorseshoeReviewFocusGameTest.java`

**Interfaces:**
- Consumes: everything from Tasks 3–10. This task adds no new production code —
  it only closes the Review Focus items not already pinned by an earlier task's
  own tests (items 1, 2, and 3/5 are already covered by Tasks 5, 7, and 6
  respectively; this task covers item 4 explicitly end-to-end).

- [ ] **Step 1: Write a GameTest that: crafts an Iron horseshoe, enchants it with
  Feather Step and damages it to half durability, equips it on a horse, performs
  the Netherite smithing upgrade, and asserts the resulting Netherite horseshoe
  still has Feather Step and is at the same damage *ratio* (half of its new,
  larger max damage)**

- [ ] **Step 2: Run the GameTest server, verify it fails if any earlier task's
  implementation dropped enchantments or used absolute instead of ratio-based
  damage carryover**

Run: `./gradlew runGameTestServer`
Expected: FAIL only if a regression exists; if Tasks 4–9 were implemented per their
specs this may already pass — if so, skip straight to Step 4.

- [ ] **Step 3: Fix whichever task's implementation dropped the ratio/enchantment
  carryover, if the test failed**

- [ ] **Step 4: Run the full GameTest suite once more end-to-end**

Run: `./gradlew runGameTestServer`
Expected: PASS — every GameTest across Tasks 3–11 green.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "test: pin netherite upgrade enchantment/durability-ratio carryover"
```
