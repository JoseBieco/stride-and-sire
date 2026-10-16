package com.josebieco.strideandsire.gametest;

import com.josebieco.strideandsire.StrideAndSire;
import java.util.function.Consumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.DeferredRegister;

/**
 * GameTest harness for Minecraft/Forge 26.3.
 *
 * <p>Since 1.21.5 vanilla GameTests are data-driven, and Forge 66 does not wire its
 * {@code @GameTest} annotation into registration. A test therefore has two halves:
 * <ol>
 *   <li>a <b>test function</b> ({@code Consumer<GameTestHelper>}) registered here in the
 *       static {@code minecraft:test_function} registry, and</li>
 *   <li>a <b>test instance</b> JSON at
 *       {@code data/strideandsire/test_instance/<name>.json} (datapack registry
 *       {@code minecraft:test_instance}) pointing at that function, the structure
 *       (normally {@code strideandsire:empty}) and the max tick budget.</li>
 * </ol>
 * Run all mod tests headlessly with {@code ./gradlew runGameTestServer}; the server
 * exits with the number of failed required tests, so a failure fails the Gradle task.
 */
public final class ModGameTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
        DeferredRegister.create(Registries.TEST_FUNCTION, StrideAndSire.MODID);

    static {
        TEST_FUNCTIONS.register("horseshoe_items_are_registered_with_correct_material",
            () -> HorseshoeItemGameTest::horseshoeItemsAreRegisteredWithCorrectMaterial);
        TEST_FUNCTIONS.register("horseshoe_attachment_set_then_get_on_horse",
            () -> HorseshoeAttachmentGameTest::setThenGetOnHorse);
        TEST_FUNCTIONS.register("horseshoe_attachment_all_eligible_types_store_horseshoe",
            () -> HorseshoeAttachmentGameTest::allEligibleTypesStoreHorseshoe);
        TEST_FUNCTIONS.register("horseshoe_attachment_persists_across_save_and_load",
            () -> HorseshoeAttachmentGameTest::persistsAcrossSaveAndLoad);
        TEST_FUNCTIONS.register("horseshoe_attachment_ineligible_entities_have_no_horseshoe",
            () -> HorseshoeAttachmentGameTest::ineligibleEntitiesHaveNoHorseshoe);
        TEST_FUNCTIONS.register("horseshoe_attachment_drops_horseshoe_on_death",
            () -> HorseshoeAttachmentGameTest::dropsHorseshoeOnDeath);
        TEST_FUNCTIONS.register("horseshoe_menu_slot_accepts_only_horseshoes",
            () -> HorseshoeMenuGameTest::slotAcceptsOnlyHorseshoes);
        TEST_FUNCTIONS.register("horseshoe_menu_keeps_vanilla_slot_layout",
            () -> HorseshoeMenuGameTest::keepsVanillaSlotLayout);
        TEST_FUNCTIONS.register("horseshoe_menu_quick_move_and_place_route_to_storage",
            () -> HorseshoeMenuGameTest::quickMoveAndPlaceRouteToStorage);
        TEST_FUNCTIONS.register("horseshoe_menu_sneak_interact_with_item_opens_horseshoe_menu",
            () -> HorseshoeMenuGameTest::sneakInteractWithItemOpensHorseshoeMenu);
        TEST_FUNCTIONS.register("horseshoe_menu_sneak_interact_name_tag_left_to_vanilla",
            () -> HorseshoeMenuGameTest::sneakInteractNameTagLeftToVanilla);
        TEST_FUNCTIONS.register("horseshoe_passive_gold_speed_modifier",
            () -> HorseshoePassiveEffectGameTest::goldSpeedModifier);
        TEST_FUNCTIONS.register("horseshoe_passive_netherite_fire_immunity",
            () -> HorseshoePassiveEffectGameTest::netheriteFireImmunity);
        TEST_FUNCTIONS.register("horseshoe_passive_speed_modifier_isolation",
            () -> HorseshoePassiveEffectGameTest::speedModifierIsolation);
        TEST_FUNCTIONS.register("horseshoe_passive_piglin_neutral_when_riding_gold",
            () -> HorseshoePassiveEffectGameTest::piglinNeutralWhenRidingGold);
        TEST_FUNCTIONS.register("horseshoe_passive_piglin_target_suppressed",
            () -> HorseshoePassiveEffectGameTest::piglinTargetSuppressed);
        TEST_FUNCTIONS.register("horseshoe_passive_gold_speed_survives_reload",
            () -> HorseshoePassiveEffectGameTest::goldSpeedSurvivesReload);
        TEST_FUNCTIONS.register("horseshoe_enchant_frost_step_rejected_on_netherite",
            () -> HorseshoeEnchantmentGameTest::frostStepRejectedOnNetherite);
        TEST_FUNCTIONS.register("horseshoe_enchant_lava_walker_rejected_on_iron",
            () -> HorseshoeEnchantmentGameTest::lavaWalkerRejectedOnIron);
        TEST_FUNCTIONS.register("horseshoe_enchant_tags_match_eligibility_matrix",
            () -> HorseshoeEnchantmentGameTest::tagsMatchEligibilityMatrix);
        TEST_FUNCTIONS.register("horseshoe_enchant_feather_step_negates_fall_damage",
            () -> HorseshoeEnchantmentGameTest::featherStepNegatesFallDamage);
        TEST_FUNCTIONS.register("horseshoe_enchant_frost_step_freezes_water_like_frost_walker_ii",
            () -> HorseshoeEnchantmentGameTest::frostStepFreezesWaterLikeFrostWalkerII);
        TEST_FUNCTIONS.register("horseshoe_enchant_lava_walker_stands_on_lava",
            () -> HorseshoeEnchantmentGameTest::lavaWalkerStandsOnLava);
        TEST_FUNCTIONS.register("horseshoe_enchant_frost_step_horse_removed_mid_tick",
            () -> HorseshoeEnchantmentGameTest::frostStepHorseRemovedMidTick);
        TEST_FUNCTIONS.register("horseshoe_enchant_lava_walker_ridden_no_fall_damage_on_landing",
            () -> HorseshoeEnchantmentGameTest::lavaWalkerRiddenNoFallDamageOnLanding);
        TEST_FUNCTIONS.register("horseshoe_durability_breaks_on_last_point",
            () -> HorseshoeDurabilityGameTest::breaksOnLastPoint);
        TEST_FUNCTIONS.register("horseshoe_durability_non_breaking_step_adds_one_damage",
            () -> HorseshoeDurabilityGameTest::nonBreakingStepAddsOneDamage);
        TEST_FUNCTIONS.register("horseshoe_durability_fire_negation_is_throttled",
            () -> HorseshoeDurabilityGameTest::fireNegationIsThrottled);
        TEST_FUNCTIONS.register("horseshoe_durability_gold_break_removes_speed_modifier",
            () -> HorseshoeDurabilityGameTest::goldBreakRemovesSpeedModifier);
        TEST_FUNCTIONS.register("horseshoe_durability_mending_repairs_from_rider_xp",
            () -> HorseshoeDurabilityGameTest::mendingRepairsFromRiderXp);
        TEST_FUNCTIONS.register("horseshoe_durability_mending_needs_rider",
            () -> HorseshoeDurabilityGameTest::mendingNeedsRider);
        TEST_FUNCTIONS.register("horseshoe_durability_frost_step_drains_only_over_water",
            () -> HorseshoeDurabilityGameTest::frostStepDrainsOnlyOverWater);
        TEST_FUNCTIONS.register("horseshoe_durability_lava_walker_drain_is_throttled",
            () -> HorseshoeDurabilityGameTest::lavaWalkerDrainIsThrottled);
        TEST_FUNCTIONS.register("horseshoe_durability_mending_still_breaks_on_last_point",
            () -> HorseshoeDurabilityGameTest::mendingHorseshoeStillBreaksOnLastPoint);
        TEST_FUNCTIONS.register("horseshoe_durability_riding_wears_one_point_per_hundred_blocks",
            () -> HorseshoeDurabilityGameTest::ridingWearsOnePointPerHundredBlocks);
        TEST_FUNCTIONS.register("horseshoe_durability_unridden_horse_does_not_wear_from_moving",
            () -> HorseshoeDurabilityGameTest::unriddenHorseDoesNotWearFromMoving);
        TEST_FUNCTIONS.register("horseshoe_recipe_copper_horseshoe",
            () -> HorseshoeRecipeGameTest::craftsCopperHorseshoe);
        TEST_FUNCTIONS.register("horseshoe_recipe_iron_horseshoe",
            () -> HorseshoeRecipeGameTest::craftsIronHorseshoe);
        TEST_FUNCTIONS.register("horseshoe_recipe_gold_horseshoe",
            () -> HorseshoeRecipeGameTest::craftsGoldHorseshoe);
        TEST_FUNCTIONS.register("horseshoe_recipe_no_netherite_crafting",
            () -> HorseshoeRecipeGameTest::noCraftingRecipeForNetherite);
        TEST_FUNCTIONS.register("horseshoe_recipe_netherite_smithing_keeps_enchants_and_ratio",
            () -> HorseshoeRecipeGameTest::netheriteSmithingKeepsEnchantsAndDamageRatio);
        TEST_FUNCTIONS.register("horseshoe_recipe_netherite_smithing_menu",
            () -> HorseshoeRecipeGameTest::smithingMenuProducesNetheriteHorseshoe);
        TEST_FUNCTIONS.register("horseshoe_recipe_frost_step_blocks_netherite_upgrade",
            () -> HorseshoeRecipeGameTest::frostStepBlocksNetheriteUpgrade);
        TEST_FUNCTIONS.register("horseshoe_review_focus_iron_to_netherite_flow",
            () -> HorseshoeReviewFocusGameTest::ironToNetheriteFlow);
        TEST_FUNCTIONS.register("horseshoe_layer_material_to_render_matches_equipped_tier",
            () -> HorseshoeLayerVisibilityGameTest::materialToRenderMatchesEquippedTier);
        TEST_FUNCTIONS.register("saddlebag_items_are_registered_with_container",
            () -> SaddlebagItemGameTest::saddlebagItemsAreRegisteredWithContainer);
    }

    private ModGameTests() {}

    public static void register(BusGroup modBusGroup) {
        TEST_FUNCTIONS.register(modBusGroup);
    }
}
