# Fase D — Genética Mendeliana de Pelagem Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add 3 exclusive coat colors to the Horse (`Obsidian`, `Albino`, `Silver Dapple`), each controlled by a single-locus Mendelian gene, inheritable through pure (item-free) breeding, rendered as a full texture override, revealed via the Breeder's Spyglass, and rewarded with a one-time advancement per pure specimen bred.

**Architecture:** A new, independent capability attachment (`HorseCoatGeneticsAttachment`) stores 3 `Zygosity` values, mirroring the Fase C `HorseGeneticsAttachment` pattern but decoupled from it. A pure-logic `HorseCoatInheritance` class implements classic Punnett-square inheritance per gene plus the manifest-gene tie-break. A spawn hook rolls rare carriers on wild Horses only; a breeding hook applies inheritance at foal birth and fixes the tie-break permanently. A client-only full-texture-override render layer and 3 new Spyglass report rows make the genetics visible; a custom advancement trigger fires exactly once per pure birth.

**Tech Stack:** Minecraft Forge "26.3", Java 25, modid `strideandsire`, package `com.josebieco.strideandsire`. JUnit 5 for pure logic. Forge GameTest (data-driven: `ModGameTests` test functions + `data/strideandsire/test_instance/*.json`) for integration behavior.

**Spec:** `docs/superpowers/specs/2026-10-17-fase-d-genetica-mendeliana-design.md`

## Global Constraints

- Scope is the Horse (`EntityTypes.HORSE`) only — never Donkey, Mule, Skeleton Horse or Zombie Horse.
- 3 genes, each independent, single-locus, classic dominant/recessive: `Obsidian`, `Albino`, `Silver Dapple`.
- Wild spawn (natural creation only): each gene independently rolls `HOMOZYGOUS_RECESSIVE` 0% of the time, `HETEROZYGOUS` 3% of the time, `HOMOZYGOUS_DOMINANT` 97% of the time. Never `aa` on spawn.
- Breeding uses pure, classic Mendelian probabilities with no item/diet influence whatsoever: `AA×AA`=100% `AA`; `AA×Aa`=50%/50% `AA`/`Aa`; `AA×aa`=100% `Aa`; `Aa×Aa`=25%/50%/25% `AA`/`Aa`/`aa`; `Aa×aa`=50%/50% `Aa`/`aa`; `aa×aa`=100% `aa`.
- When a horse manifests (`aa`) in more than one gene at once, exactly one gene is chosen uniformly at random to determine the rendered texture, fixed permanently at birth (never re-rolled on render, reload, or re-inspection).
- Pelagens exclusivas são puramente cosméticas: nenhum efeito em atributo, velocidade, ou qualquer outra mecânica.
- The special coat fully replaces the vanilla color+marking texture; it is never combined with a vanilla marking.
- One advancement per gene (`strideandsire:obsidian_coat`, `strideandsire:albino_coat`, `strideandsire:silver_dapple_coat`), granted exactly once, only at the moment a foal is born `aa` pure in that gene — never retroactively, never from Spyglass inspection alone.

## Review Focus

- A chunk unload/reload or dimension change must not re-roll a wild horse's already-assigned zygosity (the spawn hook must fire only on true creation, not on every `EntityJoinLevelEvent`) — covered in Task 5.
- A horse manifesting more than one gene must keep the same rendered coat across a save/reload cycle and after being re-inspected with the Spyglass, never re-rolling the tie-break — covered in Task 6.
- Donkeys, Mules, Skeleton Horses and Zombie Horses must never carry the new attachment or expose any non-empty genotype, even though they already carry the Fase A/B/C attachments on the same entity — covered in Task 3.
- The Spyglass must render a correct "AA / nenhum traço" row for all 3 genes on a horse with a fully empty coat-genetics attachment, without crashing on the default/unset state — covered in Task 10.
- Granting one of the 3 advancements must be idempotent: breeding a second `aa`-pure foal of the same gene must not error or re-fire side effects beyond what vanilla's own advancement system already no-ops — covered in Task 7.

---

## Task 1: `Zygosity` and `CoatGene` enums

**Files:**
- Create: `src/main/java/com/josebieco/strideandsire/genetics/Zygosity.java`
- Create: `src/main/java/com/josebieco/strideandsire/genetics/CoatGene.java`
- Test: `src/test/java/com/josebieco/strideandsire/genetics/ZygosityTest.java`

**Interfaces:**
- Produces: `Zygosity { HOMOZYGOUS_DOMINANT, HETEROZYGOUS, HOMOZYGOUS_RECESSIVE }` with `Codec<Zygosity> CODEC`, `StreamCodec<ByteBuf, Zygosity> STREAM_CODEC`, `boolean isCarrier()` (true for `HETEROZYGOUS` and `HOMOZYGOUS_RECESSIVE`), `boolean isManifest()` (true only for `HOMOZYGOUS_RECESSIVE`), mirroring `TraitState`.
- Produces: `CoatGene { OBSIDIAN, ALBINO, SILVER_DAPPLE }` with `Codec<CoatGene> CODEC`, `StreamCodec<ByteBuf, CoatGene> STREAM_CODEC`, `String translationKey()` returning `"coatgene.strideandsire.<name>"`, mirroring `LineageTrait`.

- [ ] **Step 1: Write the failing test**

```java
@Test
void isCarrierAndIsManifestMatchExpectedStates() {
    assertFalse(Zygosity.HOMOZYGOUS_DOMINANT.isCarrier());
    assertTrue(Zygosity.HETEROZYGOUS.isCarrier());
    assertFalse(Zygosity.HETEROZYGOUS.isManifest());
    assertTrue(Zygosity.HOMOZYGOUS_RECESSIVE.isCarrier());
    assertTrue(Zygosity.HOMOZYGOUS_RECESSIVE.isManifest());
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.josebieco.strideandsire.genetics.ZygosityTest"`
Expected: FAIL (class does not exist)

- [ ] **Step 3: Implement `Zygosity` in `genetics/Zygosity.java` and `CoatGene` in `genetics/CoatGene.java`**

Follow `TraitState`/`LineageTrait` exactly for `StringRepresentable`, `CODEC`, `STREAM_CODEC` (ordinal-based `ByteBufCodecs.idMapper`), and lowercase `getSerializedName()`.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "com.josebieco.strideandsire.genetics.ZygosityTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/josebieco/strideandsire/genetics/Zygosity.java \
        src/main/java/com/josebieco/strideandsire/genetics/CoatGene.java \
        src/test/java/com/josebieco/strideandsire/genetics/ZygosityTest.java
git commit -m "feat: add Zygosity and CoatGene enums for Fase D"
```

## Task 2: `HorseCoatInheritance` pure logic

**Files:**
- Create: `src/main/java/com/josebieco/strideandsire/genetics/HorseCoatInheritance.java`
- Test: `src/test/java/com/josebieco/strideandsire/genetics/HorseCoatInheritanceTest.java`

**Interfaces:**
- Consumes: `Zygosity` (Task 1).
- Produces: `HorseCoatInheritance.rollChildZygosity(Zygosity parent1, Zygosity parent2, RandomGenerator random): Zygosity`; `HorseCoatInheritance.pickManifestGene(Set<CoatGene> manifestGenes, RandomGenerator random): CoatGene` (requires a non-empty set; later tasks only call it when at least one gene is manifest).

- [ ] **Step 1: Write the failing tests**

```java
@Test
void aaCrossAaAlwaysProducesAa() {
    RandomGenerator random = new Random(1).nextDouble() /* any seeded source */;
    // seed the roll at exactly 0.0 and just below 1.0 and assert HOMOZYGOUS_RECESSIVE both times
}

@Test
void aaByAaSplitsAtQuarterAndThreeQuarters() {
    // stub RandomGenerator returning 0.10 -> HOMOZYGOUS_DOMINANT, 0.50 -> HETEROZYGOUS, 0.90 -> HOMOZYGOUS_RECESSIVE
}

@Test
void aaCrossBigAProducesOnlyHeterozygous() {
    // AA x aa -> always HETEROZYGOUS regardless of roll
}

@Test
void pickManifestGeneAlwaysReturnsAMemberOfTheInputSet() {
    // with a single-element set, always returns that element regardless of the random stub
}

@Test
void pickManifestGeneDistributesAcrossAllCandidatesOverManySamples() {
    // with a 2-element set and a real seeded RandomGenerator, assert both genes appear at least once over 200 draws
}
```
Write one test per row of the Global Constraints' breeding table (6 crosses total) plus the 2 tie-break tests above.

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew test --tests "com.josebieco.strideandsire.genetics.HorseCoatInheritanceTest"`
Expected: FAIL (class does not exist)

- [ ] **Step 3: Implement `HorseCoatInheritance`**

`rollChildZygosity` follows the exact same roll-and-threshold shape as `TraitInheritance.rollChildState`: compute the cumulative probability thresholds for the pairing (derivable purely from parent1/parent2 per the Global Constraints table) and compare a single `random.nextDouble()` draw against them. `pickManifestGene` draws one uniform index over the given set (e.g. collect to a list, `random.nextInt(list.size())` via an adapter, or `nextDouble() * size`) and returns that element.

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew test --tests "com.josebieco.strideandsire.genetics.HorseCoatInheritanceTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/josebieco/strideandsire/genetics/HorseCoatInheritance.java \
        src/test/java/com/josebieco/strideandsire/genetics/HorseCoatInheritanceTest.java
git commit -m "feat: add Mendelian coat inheritance and tie-break logic"
```

## Task 3: `HorseCoatGeneticsAttachment`, storage and access

**Files:**
- Create: `src/main/java/com/josebieco/strideandsire/attachment/HorseCoatGeneticsAttachment.java`
- Create: `src/main/java/com/josebieco/strideandsire/attachment/HorseCoatGeneticsStorage.java`
- Create: `src/main/java/com/josebieco/strideandsire/attachment/HorseCoatGeneticsAccess.java`
- Test: `src/test/java/com/josebieco/strideandsire/attachment/HorseCoatGeneticsAttachmentTest.java`

**Interfaces:**
- Consumes: `CoatGene`, `Zygosity` (Task 1); `HorseEquipmentStorage<T>` (existing base class).
- Produces: `HorseCoatGeneticsAttachment` record with fields `Map<CoatGene, Zygosity> genes` and `@Nullable CoatGene manifestGeneToRender`, constant `EMPTY`, static `Codec<HorseCoatGeneticsAttachment> CODEC`, static `StreamCodec<ByteBuf, HorseCoatGeneticsAttachment> STREAM_CODEC`, instance methods `Zygosity zygosity(CoatGene gene)`, `boolean isManifest(CoatGene gene)`, `Set<CoatGene> manifestGenes()`, `@Nullable CoatGene manifestGeneToRender()`, `HorseCoatGeneticsAttachment withGenes(Map<CoatGene, Zygosity> genes, @Nullable CoatGene manifestGeneToRender)`, `boolean isEmpty()`.
- Produces: `HorseCoatGeneticsAccess.get(AbstractHorse): HorseCoatGeneticsAttachment`; `HorseCoatGeneticsAccess.set(AbstractHorse, HorseCoatGeneticsAttachment): void`; `HorseCoatGeneticsAccess.isEligible(AbstractHorse): boolean` (true only for `EntityTypes.HORSE`, unlike `HorseshoeAccess.isEligible`).

- [ ] **Step 1: Write the failing test**

```java
@Test
void withGenesNormalizesMissingEntriesToHomozygousDominantAndIsEmptyWhenAllDominant() {
    HorseCoatGeneticsAttachment attachment = HorseCoatGeneticsAttachment.EMPTY;
    assertEquals(Zygosity.HOMOZYGOUS_DOMINANT, attachment.zygosity(CoatGene.OBSIDIAN));
    assertTrue(attachment.isEmpty());
    assertTrue(attachment.manifestGenes().isEmpty());
    assertNull(attachment.manifestGeneToRender());
}

@Test
void withGenesTracksManifestGenesAndChosenRenderGene() {
    HorseCoatGeneticsAttachment attachment = HorseCoatGeneticsAttachment.EMPTY.withGenes(
        Map.of(CoatGene.OBSIDIAN, Zygosity.HOMOZYGOUS_RECESSIVE), CoatGene.OBSIDIAN);
    assertTrue(attachment.isManifest(CoatGene.OBSIDIAN));
    assertEquals(Set.of(CoatGene.OBSIDIAN), attachment.manifestGenes());
    assertEquals(CoatGene.OBSIDIAN, attachment.manifestGeneToRender());
    assertFalse(attachment.isEmpty());
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.josebieco.strideandsire.attachment.HorseCoatGeneticsAttachmentTest"`
Expected: FAIL (class does not exist)

- [ ] **Step 3: Implement `HorseCoatGeneticsAttachment`, `HorseCoatGeneticsStorage`, `HorseCoatGeneticsAccess`**

`HorseCoatGeneticsAttachment` follows the normalization-in-constructor pattern of `HorseGeneticsAttachment` (an `EnumMap<CoatGene, Zygosity>` defaulting missing entries to `HOMOZYGOUS_DOMINANT`), plus the extra nullable `manifestGeneToRender` field serialized the same way (encode presence as a boolean + ordinal in `STREAM_CODEC`; encode as an optional field in `CODEC`, e.g. `Codec.optionalField`). `HorseCoatGeneticsStorage` extends `HorseEquipmentStorage<HorseCoatGeneticsAttachment>` exactly like `HorseGeneticsStorage` extends it for `HorseGeneticsAttachment`, pointing at a new capability (added in Task 4). `HorseCoatGeneticsAccess.isEligible` returns `horse.getType() == net.minecraft.world.entity.EntityTypes.HORSE` — this is the one access class in the mod that is *not* `HorseshoeAccess.isEligible`, per the Horse-only scope.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "com.josebieco.strideandsire.attachment.HorseCoatGeneticsAttachmentTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/josebieco/strideandsire/attachment/HorseCoatGeneticsAttachment.java \
        src/main/java/com/josebieco/strideandsire/attachment/HorseCoatGeneticsStorage.java \
        src/main/java/com/josebieco/strideandsire/attachment/HorseCoatGeneticsAccess.java \
        src/test/java/com/josebieco/strideandsire/attachment/HorseCoatGeneticsAttachmentTest.java
git commit -m "feat: add HorseCoatGeneticsAttachment with Horse-only eligibility"
```

## Task 4: Capability registration, sync packet and client sync

**Files:**
- Modify: `src/main/java/com/josebieco/strideandsire/registry/ModAttachments.java`
- Create: `src/main/java/com/josebieco/strideandsire/network/HorseCoatGeneticsSyncPacket.java`
- Create: `src/main/java/com/josebieco/strideandsire/client/HorseCoatGeneticsClientSync.java`
- Modify: `src/main/java/com/josebieco/strideandsire/network/ModNetwork.java`
- Test: `src/main/java/com/josebieco/strideandsire/gametest/HorseCoatGeneticsAttachmentGameTest.java` + `data/strideandsire/test_instance/coat_genetics_attachment_set_then_get_on_horse.json` + `data/strideandsire/test_instance/coat_genetics_attachment_ineligible_mule_has_no_coat_genetics.json`

**Interfaces:**
- Consumes: `HorseCoatGeneticsAttachment`, `HorseCoatGeneticsStorage` (Task 3).
- Produces: `ModAttachments.COAT_GENETICS_ID: Identifier`, `ModAttachments.COAT_GENETICS: Capability<HorseCoatGeneticsStorage>`; `HorseCoatGeneticsSyncPacket.sendToTracking(AbstractHorse): void` (mirrors `HorseGeneticsSyncPacket`); `HorseCoatGeneticsClientSync.apply(int entityId, HorseCoatGeneticsAttachment): void`.

- [ ] **Step 1: Write the failing GameTest JSON + function registration**

Add two `test_instance` JSON files (same shape as `genetics_attachment_set_then_get_on_horse.json`) pointing at new functions `coat_genetics_attachment_set_then_get_on_horse` and `coat_genetics_attachment_ineligible_mule_has_no_coat_genetics`, and register them in `ModGameTests.TEST_FUNCTIONS`.

```java
public static void setThenGetOnHorse(GameTestHelper helper) {
    // spawn a Horse, HorseCoatGeneticsAccess.set(horse, attachment with OBSIDIAN=HOMOZYGOUS_RECESSIVE),
    // assert HorseCoatGeneticsAccess.get(horse).isManifest(CoatGene.OBSIDIAN)
}

public static void ineligibleMuleHasNoCoatGenetics(GameTestHelper helper) {
    // spawn a Mule, assert !HorseCoatGeneticsAccess.isEligible(mule) and get(mule).isEmpty()
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew runGameTestServer` (both new tests FAIL: capability does not exist yet)

- [ ] **Step 3: Implement the capability, sync packet and client sync**

Add `COAT_GENETICS_ID`/`COAT_GENETICS` to `ModAttachments` exactly like `GENETICS_ID`/`GENETICS`, update its class javadoc. Add `HorseCoatGeneticsSyncPacket` mirroring `HorseGeneticsSyncPacket` (same `sendToTracking`/`sendTo`/`handle` shape). Add `HorseCoatGeneticsClientSync.apply` mirroring `HorseGeneticsClientSync.apply`. Register the new packet in `ModNetwork`'s clientbound builder and bump `PROTOCOL_VERSION` to 4 with an updated comment (`// 4: HorseCoatGeneticsSyncPacket`). `HorseCoatGeneticsAccess.set` must call `HorseCoatGeneticsSyncPacket.sendToTracking` on the server side, mirroring `HorseGeneticsAccess.set`.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew runGameTestServer` (both new tests PASS)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/josebieco/strideandsire/registry/ModAttachments.java \
        src/main/java/com/josebieco/strideandsire/network/HorseCoatGeneticsSyncPacket.java \
        src/main/java/com/josebieco/strideandsire/network/ModNetwork.java \
        src/main/java/com/josebieco/strideandsire/client/HorseCoatGeneticsClientSync.java \
        src/main/java/com/josebieco/strideandsire/gametest/ModGameTests.java \
        src/main/java/com/josebieco/strideandsire/gametest/HorseCoatGeneticsAttachmentGameTest.java \
        src/main/resources/data/strideandsire/test_instance/coat_genetics_attachment_set_then_get_on_horse.json \
        src/main/resources/data/strideandsire/test_instance/coat_genetics_attachment_ineligible_mule_has_no_coat_genetics.json
git commit -m "feat: register coat genetics capability, sync packet and client sync"
```

## Task 5: Wild spawn carrier roll

**Files:**
- Create: `src/main/java/com/josebieco/strideandsire/event/HorseCoatSpawnHandler.java`
- Create: `src/main/java/com/josebieco/strideandsire/gametest/HorseCoatSpawnGameTest.java`
- Create: `data/strideandsire/test_instance/coat_genetics_wild_spawn_never_manifests.json`
- Modify: `src/main/java/com/josebieco/strideandsire/StrideAndSire.java`
- Modify: `src/main/java/com/josebieco/strideandsire/gametest/ModGameTests.java`

**Interfaces:**
- Consumes: `HorseCoatGeneticsAccess` (Task 3); `CoatGene`, `Zygosity` (Task 1).
- Produces: `HorseCoatSpawnHandler.register(): void`; `HorseCoatSpawnHandler.WILD_CARRIER_CHANCE: double` (= `0.03`, the Global Constraints value).

- [ ] **Step 1: Write the failing GameTest**

```java
public static void wildSpawnNeverManifests(GameTestHelper helper) {
    // spawn 100 Horses via EntitySpawnReason.NATURAL + finalizeSpawn + addFreshEntity, same pattern as
    // AttributeRangeExpansionGameTest.wildSpawnNeverExceedsVanillaMax; for every spawned horse and every
    // CoatGene, assert HorseCoatGeneticsAccess.get(horse).zygosity(gene) != Zygosity.HOMOZYGOUS_RECESSIVE;
    // assert at least one HETEROZYGOUS roll occurred across the sample (statistical, not per-horse)
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew runGameTestServer`
Expected: FAIL (handler not registered; every horse reads `HOMOZYGOUS_DOMINANT` only — the "at least one HETEROZYGOUS across the sample" assertion fails)

- [ ] **Step 3: Implement `HorseCoatSpawnHandler`**

Register a listener on `net.minecraftforge.event.entity.living.FinalizeSpawnEvent`. Only act when `event.getEntity() instanceof AbstractHorse horse`, `HorseCoatGeneticsAccess.isEligible(horse)`, and `event.getSpawnType() == net.minecraft.world.entity.EntitySpawnReason.NATURAL` (never for breeding, spawn eggs, commands, or structures — those paths either don't reach this handler meaningfully or are explicitly excluded per the Global Constraints' "wild spawn" scope). For each `CoatGene`, roll independently: `WILD_CARRIER_CHANCE` for `HETEROZYGOUS`, otherwise `HOMOZYGOUS_DOMINANT`, never `HOMOZYGOUS_RECESSIVE`. Call `HorseCoatGeneticsAccess.set` once with the combined map. Register in `StrideAndSire`'s constructor alongside the other handlers.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew runGameTestServer`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/josebieco/strideandsire/event/HorseCoatSpawnHandler.java \
        src/main/java/com/josebieco/strideandsire/StrideAndSire.java \
        src/main/java/com/josebieco/strideandsire/gametest/HorseCoatSpawnGameTest.java \
        src/main/java/com/josebieco/strideandsire/gametest/ModGameTests.java \
        src/main/resources/data/strideandsire/test_instance/coat_genetics_wild_spawn_never_manifests.json
git commit -m "feat: roll rare wild-horse coat gene carriers on natural spawn"
```

## Task 6: Breeding inheritance and tie-break

**Files:**
- Create: `src/main/java/com/josebieco/strideandsire/event/HorseCoatBreedingHandler.java`
- Create: `src/main/java/com/josebieco/strideandsire/gametest/HorseCoatBreedingGameTest.java`
- Create: `data/strideandsire/test_instance/coat_genetics_breeding_aa_x_aa_produces_manifest_statistically.json`
- Create: `data/strideandsire/test_instance/coat_genetics_tie_break_persists_across_reload.json`
- Modify: `src/main/java/com/josebieco/strideandsire/StrideAndSire.java`
- Modify: `src/main/java/com/josebieco/strideandsire/gametest/ModGameTests.java`

**Interfaces:**
- Consumes: `HorseCoatGeneticsAccess` (Task 3), `HorseCoatInheritance` (Task 2), `HorseBreedingGeneticsHandler.adapt(RandomSource)` (existing, reused as-is).
- Produces: `HorseCoatBreedingHandler.register(): void`; `HorseCoatBreedingHandler.applyOffspringCoatGenetics(AbstractHorse parentA, AbstractHorse parentB, AbstractHorse child, RandomSource random): void`.

- [ ] **Step 1: Write the failing GameTests**

```java
public static void aaByAaProducesManifestStatistically(GameTestHelper helper) {
    // set both parents HOMOZYGOUS_RECESSIVE on OBSIDIAN, breed 1 pair via the real BabyEntitySpawnEvent path
    // (same technique as existing breeding GameTests), assert the child is HOMOZYGOUS_RECESSIVE on OBSIDIAN
    // and that HorseCoatGeneticsAccess.get(child).manifestGeneToRender() == CoatGene.OBSIDIAN
}

public static void tieBreakPersistsAcrossReload(GameTestHelper helper) {
    // construct a horse manifest on two genes at once (direct Access.set, not breeding), save/reload via
    // TagValueOutput/TagValueInput like AttributeRangeExpansionGameTest, assert manifestGeneToRender is
    // identical before and after
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew runGameTestServer`
Expected: FAIL (handler not registered; child keeps `EMPTY` coat genetics)

- [ ] **Step 3: Implement `HorseCoatBreedingHandler`**

Register a `BabyEntitySpawnEvent` listener (same event `HorseBreedingGeneticsHandler` already uses), filtered to `HorseCoatGeneticsAccess.isEligible(child)`. For each `CoatGene`, call `HorseCoatInheritance.rollChildZygosity` on the two parents' values (read via `HorseCoatGeneticsAccess.get`) using `HorseBreedingGeneticsHandler.adapt(random)`. Collect the resulting manifest genes into a set; if non-empty, call `HorseCoatInheritance.pickManifestGene` once and store the result as `manifestGeneToRender`; if empty, store `null`. Call `HorseCoatGeneticsAccess.set` once with the built attachment. Register in `StrideAndSire`'s constructor.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew runGameTestServer`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/josebieco/strideandsire/event/HorseCoatBreedingHandler.java \
        src/main/java/com/josebieco/strideandsire/StrideAndSire.java \
        src/main/java/com/josebieco/strideandsire/gametest/HorseCoatBreedingGameTest.java \
        src/main/java/com/josebieco/strideandsire/gametest/ModGameTests.java \
        src/main/resources/data/strideandsire/test_instance/coat_genetics_breeding_aa_x_aa_produces_manifest_statistically.json \
        src/main/resources/data/strideandsire/test_instance/coat_genetics_tie_break_persists_across_reload.json
git commit -m "feat: apply Mendelian coat inheritance and fixed tie-break at foal birth"
```

## Task 7: Advancements for pure specimens

**Files:**
- Create: `src/main/java/com/josebieco/strideandsire/advancement/BredPureCoatTrigger.java`
- Create: `src/main/resources/data/strideandsire/advancement/obsidian_coat.json`
- Create: `src/main/resources/data/strideandsire/advancement/albino_coat.json`
- Create: `src/main/resources/data/strideandsire/advancement/silver_dapple_coat.json`
- Create: `src/main/resources/data/strideandsire/advancement/root.json` (parent advancement; a single "Stride & Sire" root with a vanilla icon, so the 3 coat advancements have a tab to attach to — a minimal placeholder, since no root advancement exists yet in this mod)
- Modify: `src/main/java/com/josebieco/strideandsire/event/HorseCoatBreedingHandler.java`
- Modify: `src/main/java/com/josebieco/strideandsire/registry/ModCriteriaTriggers.java` (create if it does not yet exist, following the `DeferredRegister` pattern used by `ModGameTests.TEST_FUNCTIONS`)
- Create: `src/main/java/com/josebieco/strideandsire/gametest/BredPureCoatAdvancementGameTest.java`
- Create: `data/strideandsire/test_instance/coat_genetics_advancement_granted_on_pure_birth.json`
- Modify: `src/main/java/com/josebieco/strideandsire/gametest/ModGameTests.java`

**Interfaces:**
- Consumes: `HorseCoatBreedingHandler.applyOffspringCoatGenetics` (Task 6, as the call site); `BabyEntitySpawnEvent`'s player-bred context (the breeding parent's owner, where available, is the player to grant the advancement to — vanilla horses bred by a player are always tamed by that player, so `parentA`'s/`parentB`'s owner via `AbstractHorse.getOwner()` is the recipient; if neither parent has an owner, no advancement is granted, since there is no player to grant it to).
- Produces: `BredPureCoatTrigger.trigger(ServerPlayer player, CoatGene gene): void`; registered under id `strideandsire:bred_pure_coat`.

- [ ] **Step 1: Write the failing GameTest**

```java
public static void advancementGrantedOnPureBirth(GameTestHelper helper) {
    // tame and own a Horse pair as a fake/mock ServerPlayer (same helper pattern used elsewhere in the
    // breeding GameTests for an owned horse), set both parents HOMOZYGOUS_RECESSIVE on ALBINO, breed,
    // assert player.getAdvancements().getOrStartProgress(<albino_coat Advancement holder>).isDone()
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew runGameTestServer`
Expected: FAIL (advancement/trigger do not exist)

- [ ] **Step 3: Implement the trigger, advancement JSON, and the call site**

`BredPureCoatTrigger` extends vanilla's simple criterion trigger base class (follow whatever base class Minecraft 26.3 actually exposes for a zero-condition custom trigger; inspect a vanilla trigger such as the bred-animals trigger for the exact signature) and exposes a static `trigger(ServerPlayer, CoatGene)` that looks up the registered instance and calls its own trigger method filtered by gene. Register it via `ModCriteriaTriggers` (a `DeferredRegister<CriterionTrigger<?>>`, same registration style as `ModGameTests.TEST_FUNCTIONS`'s `DeferredRegister`), called from `StrideAndSire`'s constructor. Each advancement JSON uses `"criteria": {"bred_<gene>": {"trigger": "strideandsire:bred_pure_coat", "conditions": {"gene": "<gene>"}}}`, parent `strideandsire:root`, and a vanilla item icon per gene (`minecraft:black_dye` for Obsidian, `minecraft:white_dye` for Albino, `minecraft:light_gray_dye` for Silver Dapple — placeholder icons reusing existing vanilla items, since no new item exists for this feature). In `HorseCoatBreedingHandler.applyOffspringCoatGenetics`, after setting the attachment, for each gene the child manifests, resolve an owning `ServerPlayer` from `parentA.getOwner()` or `parentB.getOwner()` and call `BredPureCoatTrigger.trigger`.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew runGameTestServer`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/josebieco/strideandsire/advancement/BredPureCoatTrigger.java \
        src/main/java/com/josebieco/strideandsire/registry/ModCriteriaTriggers.java \
        src/main/java/com/josebieco/strideandsire/event/HorseCoatBreedingHandler.java \
        src/main/java/com/josebieco/strideandsire/StrideAndSire.java \
        src/main/java/com/josebieco/strideandsire/gametest/BredPureCoatAdvancementGameTest.java \
        src/main/java/com/josebieco/strideandsire/gametest/ModGameTests.java \
        src/main/resources/data/strideandsire/advancement/root.json \
        src/main/resources/data/strideandsire/advancement/obsidian_coat.json \
        src/main/resources/data/strideandsire/advancement/albino_coat.json \
        src/main/resources/data/strideandsire/advancement/silver_dapple_coat.json \
        src/main/resources/data/strideandsire/test_instance/coat_genetics_advancement_granted_on_pure_birth.json
git commit -m "feat: grant a one-time advancement for each pure coat gene bred"
```

## Task 8: Placeholder coat textures

**Files:**
- Modify: `tools/pixelart/generate.py`
- Create (generated, not hand-edited): `src/main/resources/assets/strideandsire/textures/entity/horse/obsidian.png`
- Create (generated): `src/main/resources/assets/strideandsire/textures/entity/horse/albino.png`
- Create (generated): `src/main/resources/assets/strideandsire/textures/entity/horse/silver_dapple.png`

**Interfaces:**
- Produces: 3 PNGs at 64x64 (vanilla adult-horse UV layout), listed in the generator's `MANIFEST`.

- [ ] **Step 1: Add the 3 outputs to `tools/pixelart/generate.py`'s `MANIFEST` and write their deterministic pixel maps**

Draw each as a flat base-coat fill across every body/leg/head UV region vanilla's `horse.png` layout defines (no shading variation needed — these are placeholders, matching the project's existing documented policy of real screenshots/art replacing placeholders later), using these fixed palettes: Obsidian — base `#15141a`, mane/tail `#0c0b10` (slightly darker, not contrasting); Albino — base `#f5f2ea`, mane/tail `#f5f2ea` (same as base, no contrast, per the spec), eye region recolored to `#d98a93`; Silver Dapple — base `#a7a7b0`, with a fixed, deterministic scatter of `#6f6f7a` dapple dots (reuse the same dot-placement technique already used for an existing spotted texture in this generator, if any, otherwise a fixed modulo-based grid offset so the output is reproducible).

- [ ] **Step 2: Run the generator**

Run: `python tools/pixelart/generate.py`
Expected: creates/overwrites the 3 PNGs with no errors

- [ ] **Step 3: Run the generator's own validation**

Run: `python tools/pixelart/generate.py --check`
Expected: exits 0

- [ ] **Step 4: Commit**

```bash
git add tools/pixelart/generate.py \
        src/main/resources/assets/strideandsire/textures/entity/horse/obsidian.png \
        src/main/resources/assets/strideandsire/textures/entity/horse/albino.png \
        src/main/resources/assets/strideandsire/textures/entity/horse/silver_dapple.png
git commit -m "feat: add deterministic placeholder textures for the 3 exclusive coats"
```

## Task 9: Coat override render layer

**Files:**
- Create: `src/main/java/com/josebieco/strideandsire/render/HorseCoatRendering.java`
- Create: `src/main/java/com/josebieco/strideandsire/client/HorseCoatOverrideLayer.java`
- Modify: `src/main/java/com/josebieco/strideandsire/client/StrideAndSireClient.java`
- Test: `src/test/java/com/josebieco/strideandsire/render/HorseCoatRenderingTest.java`

**Interfaces:**
- Consumes: `HorseCoatGeneticsAccess` (Task 3); the 3 textures (Task 8).
- Produces: `HorseCoatRendering.textureToRender(AbstractHorse horse): @Nullable Identifier` (returns `null` when `manifestGeneToRender()` is `null`, otherwise `strideandsire:textures/entity/horse/<gene>.png`, mirroring `HorseshoeRendering.hoofTexture`).

- [ ] **Step 1: Write the failing test**

```java
@Test
void textureToRenderIsNullWhenNoGeneManifest() {
    AbstractHorse horse = mockOrFakeHorseWithEmptyCoatGenetics();
    assertNull(HorseCoatRendering.textureToRender(horse));
}

@Test
void textureToRenderMatchesTheChosenManifestGene() {
    AbstractHorse horse = horseWithManifestGeneToRender(CoatGene.SILVER_DAPPLE);
    assertEquals(Identifier.fromNamespaceAndPath("strideandsire", "textures/entity/horse/silver_dapple.png"),
        HorseCoatRendering.textureToRender(horse));
}
```

Use the same lightweight fake-entity technique the existing `render` package tests (e.g. `SaddlebagPlacementTest`) already use to avoid booting a full Minecraft server in a JUnit test.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.josebieco.strideandsire.render.HorseCoatRenderingTest"`
Expected: FAIL (class does not exist)

- [ ] **Step 3: Implement `HorseCoatRendering` and `HorseCoatOverrideLayer`**

`HorseCoatRendering.textureToRender` delegates to `HorseCoatGeneticsAccess.get(horse).manifestGeneToRender()` and maps it to the texture path, exactly like `HorseshoeRendering.materialToRender`/`hoofTexture`. `HorseCoatOverrideLayer` extends `RenderLayer<S, M>` like `HorseshoeLayer`, but submits a fully opaque full-body texture (not a cutout) so it visually replaces the vanilla coat drawn by the base model, at a submission order placed immediately after the base body and before the markings/saddle layers (inspect `AbstractHorseRenderer`'s existing layer order, same way `HorseshoeLayer`'s own `ORDER = 1` comment documents vanilla's order). Skip rendering (return early) when `state.isBaby` or `state.isInvisible`, same guard as `HorseshoeLayer`. In `StrideAndSireClient`, add a new `COAT_LAYER_TYPES = List.of(EntityTypes.HORSE)` list (distinct from `EQUIPMENT_LAYER_TYPES`, which also covers Donkey/Mule/Skeleton/Zombie) and attach `HorseCoatOverrideLayer` only to renderers in that list, inside `onAddLayers`.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "com.josebieco.strideandsire.render.HorseCoatRenderingTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/josebieco/strideandsire/render/HorseCoatRendering.java \
        src/main/java/com/josebieco/strideandsire/client/HorseCoatOverrideLayer.java \
        src/main/java/com/josebieco/strideandsire/client/StrideAndSireClient.java \
        src/test/java/com/josebieco/strideandsire/render/HorseCoatRenderingTest.java
git commit -m "feat: render the chosen exclusive coat as a full texture override"
```

## Task 10: Breeder's Spyglass extension

**Files:**
- Modify: `src/main/java/com/josebieco/strideandsire/menu/BreederSpyglassScreen.java`
- Modify: `src/main/java/com/josebieco/strideandsire/client/BreederSpyglassClientScreen.java`
- Modify: `src/main/resources/assets/strideandsire/lang/en_us.json`
- Modify: `src/main/resources/assets/strideandsire/lang/pt_br.json`
- Modify: `src/main/java/com/josebieco/strideandsire/gametest/BreederSpyglassGameTest.java`
- Modify: `data/strideandsire/test_instance/genetics_spyglass_report_and_interaction.json` (or add a sibling test instance if the existing one should stay focused on Fase C)

**Interfaces:**
- Consumes: `HorseCoatGeneticsAccess` (Task 3); `CoatGene`, `Zygosity` (Task 1).
- Produces: `BreederSpyglassScreen.SpyglassReport` gains `Map<CoatGene, Zygosity> coatGenes()` and `Zygosity coatZygosity(CoatGene gene)`.

- [ ] **Step 1: Write the failing GameTest assertion**

Extend the existing Spyglass GameTest (or add a new test function `coat_genetics_spyglass_reports_all_genes`) asserting that `BreederSpyglassScreen.buildReport(horse).coatZygosity(CoatGene.OBSIDIAN)` reflects a zygosity set directly via `HorseCoatGeneticsAccess.set` before the report is built, for all 3 genes.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew runGameTestServer`
Expected: FAIL (`coatZygosity` does not exist)

- [ ] **Step 3: Implement the report extension and the client rows**

Add `coatGenes` to the `SpyglassReport` record (same `EnumMap`-normalizing constructor pattern as `traits`) and populate it in `buildReport` from `HorseCoatGeneticsAccess.get(horse)`. In `BreederSpyglassClientScreen`, after the existing traits section, add a third section `"gui.strideandsire.spyglass.section.coat_genetics"` with one row per `CoatGene` showing its translated name and a zygosity label (`"AA (nenhum traço)"`/`"Aa (portador)"`/`"aa (puro)"`, i.e. 3 new lang keys `gui.strideandsire.spyglass.state.homozygous_dominant`/`heterozygous`/`homozygous_recessive`, styled dark-gray/yellow/green respectively, mirroring `styleForState`). Increase `PANEL_HEIGHT` by `8 + 3 * 12` to fit the new section without overlapping the panel border. Add the 3 `coatgene.strideandsire.<name>` lang keys and the section/state lang keys to both `en_us.json` and `pt_br.json`.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew runGameTestServer`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/josebieco/strideandsire/menu/BreederSpyglassScreen.java \
        src/main/java/com/josebieco/strideandsire/client/BreederSpyglassClientScreen.java \
        src/main/resources/assets/strideandsire/lang/en_us.json \
        src/main/resources/assets/strideandsire/lang/pt_br.json \
        src/main/java/com/josebieco/strideandsire/gametest/BreederSpyglassGameTest.java \
        src/main/resources/data/strideandsire/test_instance/genetics_spyglass_report_and_interaction.json
git commit -m "feat: show coat gene zygosity in the Breeder's Spyglass"
```

## Task 11: Review Focus regression

**Files:**
- Create: `src/main/java/com/josebieco/strideandsire/gametest/CoatGeneticsReviewFocusGameTest.java`
- Create: `data/strideandsire/test_instance/coat_genetics_review_focus_cross_phase_and_edge_cases.json`
- Modify: `src/main/java/com/josebieco/strideandsire/gametest/ModGameTests.java`

**Interfaces:**
- Consumes: everything from Tasks 1–10, plus the existing `HorseshoeAccess`/`SaddlebagAccess`/`HorseGeneticsAccess` from Fases A–C.

- [ ] **Step 1: Write the failing GameTest covering every Review Focus line**

```java
public static void crossPhaseAndEdgeCases(GameTestHelper helper) {
    // 1. Spawn a wild Horse via NATURAL + finalizeSpawn + addFreshEntity, assert no gene is manifest;
    //    call the client-join path (addFreshEntity again / simulate EntityJoinLevelEvent) and assert the
    //    zygosity map is unchanged (not re-rolled).
    // 2. Equip a horseshoe and a saddlebag (existing Fase A/B items) on a Horse that also manifests a
    //    coat gene; assert HorseshoeAccess.get/SaddlebagAccess.get/HorseCoatGeneticsAccess.get all read back
    //    correctly and independently (no field collision).
    // 3. Build an attachment manifest on two genes, save/reload it (TagValueOutput/TagValueInput), and after
    //    reload call BreederSpyglassScreen.buildReport twice; assert manifestGeneToRender is identical both
    //    times and matches the pre-reload value.
    // 4. Spawn a Donkey and a Mule; assert HorseCoatGeneticsAccess.isEligible returns false for both and
    //    get(...) returns HorseCoatGeneticsAttachment.EMPTY for both, even though HorseshoeAccess.isEligible
    //    returns true for both.
    // 5. Breed two aa x aa Horses for the same gene twice (two separate owned pairs); assert the owning
    //    player's advancement is done after the first birth and that triggering again after the second birth
    //    does not throw.
    helper.succeed();
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew runGameTestServer`
Expected: FAIL (some sub-assertions fail until prior tasks' code exists; by this point in the plan all of Tasks 1–10 are already implemented, so this step should mostly double-check wiring — any genuine failure here is a regression to fix before continuing)

- [ ] **Step 3: Fix any regression surfaced by the test**

If every sub-assertion already passes because Tasks 1–10 implemented them correctly, this step is a no-op confirmation. Otherwise fix the specific task's code.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew runGameTestServer`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/josebieco/strideandsire/gametest/CoatGeneticsReviewFocusGameTest.java \
        src/main/java/com/josebieco/strideandsire/gametest/ModGameTests.java \
        src/main/resources/data/strideandsire/test_instance/coat_genetics_review_focus_cross_phase_and_edge_cases.json
git commit -m "test: cross-phase and edge-case regression coverage for Fase D"
```
